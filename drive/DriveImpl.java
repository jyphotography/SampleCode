package com.ps.google.drive;

import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileWriter;
import java.io.BufferedWriter;
import org.apache.commons.io.IOUtils;

import com.google.api.client.http.FileContent;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.ParentReference;
import com.google.api.services.drive.model.Permission;
import com.ps.ddintegration.DownloadFile;
import com.ps.google.GoogleOauth2Impl;
import com.google.common.io.Files;
import com.google.common.base.Charsets;

import javax.activation.MimetypesFileTypeMap;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.NoSuchElementException;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.List;
import java.util.Iterator;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Class which contains helper methods used to communicate with the Google Drive API.
 */
public class DriveImpl extends GoogleOauth2Impl {

    /** Root folder ID */
    public static final String ROOT_ID = "root";

    /** The default result size */
    protected static final int DEFAULT_RESULT_SIZE = 100;

    /** The main class used to make API calls to Drive */
    protected Drive service;

    /**
     * Default constructor.
     *
     * @param token the user's access token used for authentication.
     */
    public DriveImpl(String token) {
        super(token);
        service = new Drive.Builder(httpTransport, jsonFactory, credential).setApplicationName(GoogleOauth2Impl.APP_NAME).build();
    }

    /**
     * Used to retrieve a list containing all files in a specified folder.
     *
     * @param folderID the folder ID in which to search for files.
     * @param numFiles the number of files to retrieve.
     * @return a <code>FileList</code> containing all files in a specified folder.
     * @throws IOException
     */
    public FileList getFilesInFolder(String folderID, int numFiles) throws IOException {
        FileList fileList = service.files().list()
            .setQ(String.format("'%s' in parents", folderID))
            .setMaxResults(numFiles)
            .execute();
        List<File> list = fileList.getItems();
        Iterator<File> i = list.iterator();
        while (i.hasNext()) {
            File file = i.next();
            //            log(file);

            File.Labels labels = file.getLabels();
            if (labels.getTrashed())
                i.remove();
        }

        return fileList;
    }

    /**
     * Used to retrieve a list containing all files in a specified folder.
     *
     * @param folderID the folder ID in which to search for files.
     * @return a <code>FileList</code> containing all files in a specified folder.
     * @throws IOException
     */
    public FileList getFilesInFolder(String folderID) throws IOException {
        return getFilesInFolder(folderID, DEFAULT_RESULT_SIZE);
    }

    /**
     * Shares a specified file by inserting a new permission for the specified file.
     *
     * @param fileID ID of the file to insert permission for.
     * @param value  User or group e-mail address, domain name or {@code null}
     *               "default" type.
     * @param type   The value "user", "group", "domain" or "default".
     * @param role   The value "owner", "writer" or "reader".
     * @return The inserted permission if successful
     */
    public Permission shareDoc(String fileID, String value, String type, String role) throws IOException {
        Permission permission = new Permission();
        permission.setValue(value);
        permission.setType(type);
        permission.setRole(role);
        return service.permissions().insert(fileID, permission).execute();
    }

    /**
     * Removes the user's permissions from the specified file.
     *
     * @param fileID       ID of the file to remove permission for.
     * @param permissionID ID of the permission to remove.
     */
    public void unShareDoc(String fileID, String permissionID) throws IOException {
        service.permissions().delete(fileID, permissionID).execute();
    }

    /**
     * Looks for an existing permission relating to the specified file ID and email address passed in.
     *
     * @param fileID ID of the file to look for existing permissions for.
     * @param email  user's email address to look for existing permissions for.
     * @return the {@link Permission} object if one could be found or {@code null} if no existing permission exists.
     * @throws java.io.IOException
     */
    public Permission getExistingPermission(String fileID, String email) throws java.io.IOException {
        try {
            return service.permissions().list(fileID).execute().getItems().stream()
                .filter(p -> p.getEmailAddress().equals(email)).findFirst().get();
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    /**
     * Used to update the role of an existing permission.
     *
     * @param fileID     ID of the file to update existing permissions for.
     * @param permission the object to be updated.
     * @param role       the new role of the existing permission.
     * @return the updated {@link Permission} object.
     * @throws IOException
     */
    public Permission updatePermission(String fileID, Permission permission, String role) throws IOException {
        permission.setRole(role);
        return service.permissions().update(fileID, permission.getId(), permission).execute();
    }

    /**
     * Moves a file to a different folder.
     *
     * @param fileID      ID of the file to be moved.
     * @param newParentID ID of the folder in which the file is to be moved.
     * @return the updated file.
     * @throws IOException
     */
    public File moveFile(String fileID, String newParentID) throws IOException {
        File file = service.files().get(fileID).execute();

        // make sure the parent id is valid
        if (newParentID != null && !newParentID.isEmpty()) {
            file.setParents(Collections.singletonList(new ParentReference().setId(newParentID)));
            return service.files().update(fileID, file).execute();
        } else {
            return null;
        }
    }

    /**
     * Uploads a file to Drive.
     *
     * @param upFile   File to be uploaded to drive.
     * @param parentId String id referring who the parent of the file should be.
     * @throws IOException
     */
    public void uploadFile(java.io.File upFile, String parentId) throws IOException {

        File body = new File();
        body.setTitle(upFile.getName());
        String mimeType = new MimetypesFileTypeMap().getContentType(upFile);
        body.setMimeType(mimeType);
        // Set the parent folder.
        if (parentId != null && parentId.length() > 0) {
            body.setParents(Collections.singletonList(new ParentReference().setId(parentId)));

        }
        FileContent mediaContent = new FileContent(mimeType, upFile);

        service.files().insert(body, mediaContent).execute();

    }

    /**
     * Uploads a file to Drive.
     *
     * @param upFile   File to be uploaded to drive.
     * @param parentId String id referring who the parent of the file should be.
     * @throws IOException
     */
    public void putFile(String upFile, String parentId, String name) throws IOException {

        java.io.File temp = java.io.File.createTempFile("tempfile", ".tmp");

        BufferedWriter bw = new BufferedWriter(new FileWriter(temp));
        bw.write(upFile);
        bw.close();

        File body = new File();
        body.setTitle(name);
        String mimeType = "";
        body.setMimeType(mimeType);
        // Set the parent folder.
        if (parentId != null && parentId.length() > 0) {
            body.setParents(Collections.singletonList(new ParentReference().setId(parentId)));

        }
        FileContent mediaContent = new FileContent(mimeType, temp);

        try {
            File resp = service.files().insert(body, mediaContent).execute();
        } catch (IOException e) {
            Logger.getLogger(DriveImpl.class.getName()).log(Level.WARNING, e.toString());
        } 
    }

    /**
     * Downloads a file
     *
     * @param fileID The ID of the file to be downloaded.
     * @return InputStream of the file downloaded
     * @throws IOException
     */
    public DownloadFile downloadFile(String fileID) throws IOException {
        File file = service.files().get(fileID).execute();
        if (file == null) {
            throw new FileNotFoundException(String.format("File with ID: %s could not be found.", fileID));
        }
        long fileSize = file.getFileSize();
        String fileName = file.getTitle();
        Logger.getLogger(DriveImpl.class.getName()).log(Level.WARNING, fileName);
        InputStream inputStream = service.files().get(file.getId()).executeMediaAsInputStream();
        return new DownloadFile(fileName, fileSize, inputStream);
    }
   
    /**
     * Return a file
     *
     * @param fileID The ID of the file to be downloaded.
     * @return File downloaded
     * @throws IOException
     */
    public String returnFile(String fileID) throws IOException {
        File file = service.files().get(fileID).execute();
        if (file == null) {
            throw new FileNotFoundException(String.format("File with ID: %s could not be found.", fileID));
        }
        long fileSize = file.getFileSize();
        String fileSizeStr = " " + fileSize;
        String fileName = file.getTitle();
        InputStream inputStream = service.files().get(file.getId()).executeMediaAsInputStream();
        BufferedReader streamReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
        StringBuilder responseStrBuilder = new StringBuilder();

        String inputStr;
        while ((inputStr = streamReader.readLine()) != null)
          responseStrBuilder.append(inputStr);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("file", responseStrBuilder.toString());
        jsonObject.put("name", fileName);
        jsonObject.put("file_size", fileSizeStr);
        return jsonObject.toString();
    }

    /**
     * Search for files and folders.
     *
     * @param query The words contained in the title.
     * @return a <code>FileList</code> containing all files that match the query.
     * @throws IOException
     */
    public FileList search(String query) throws IOException {
        return service.files().list().setQ(String.format("title contains '%s'", query)).execute();
    }

    //helper for debugging
    private static void log(Object s) {
        Logger
            .getLogger(DriveImpl.class.getName())
            .log(Level.WARNING, s.toString());
    }
}
