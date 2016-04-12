package com.ps.google;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;

import com.google.api.client.auth.oauth2.Credential;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class which contains common methods and variables used by all Google APIs.
 */
public abstract class GoogleOauth2Impl {

    /** The application name */
    protected static final String APP_NAME = "Focus Feed";

    /** Credential used to authenticate oauth requests */
    protected final GoogleCredential credential;

    /** Objects used to create the credential and service */
    protected final HttpTransport httpTransport;
    protected final JsonFactory jsonFactory;

    /**
     * Default constructor.
     *
     * @param token the user's access token used for authentication.
     */
    public GoogleOauth2Impl(String token) {
        httpTransport = new NetHttpTransport();
        jsonFactory = new JacksonFactory();
        credential = new GoogleCredential.Builder()
            .setTransport(httpTransport)
            .setJsonFactory(jsonFactory)
            .build();
        credential.setAccessToken(token);
    }

    /**
     * Parses and returns a String from the {@code HttpResponse} object passed in.
     *
     *  @param httpResponse the response from the http request
     *  @return a String from the (@code HttpResponse}  object passed in.
     */

    public String parseContent(HttpResponse httpResponse) throws IOException {
        BufferedReader reader = null;
        StringBuilder builder;
        String content = "";
        try {
            reader = new BufferedReader(new InputStreamReader(httpResponse.getContent()));
            builder = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            content = builder.toString();
        } catch (IOException e) {
            Logger.getLogger(GoogleOauth2Impl.class.getName()).log(Level.WARNING, "Error reading response from request.", e);
        } finally {
            if (reader != null) {
                reader.close();
            }
        }

        return content;
    }


    /**
     * Calls parse content to parse the HttpResponse and returns a {@code JSONObject} built from the string returned from parseContent
     *
     * @param httpResponse the response from the http request.
     * @return a {@code JSONObject} from the {@code HttpResponse} object passed in.
     */
    public JSONObject parseJson(HttpResponse httpResponse) throws IOException {
        String content = parseContent(httpResponse);
        return new JSONObject(content);
    }
    /**
     * Calls parse content to parse the HttpResponse and returns a {@code JSONArray} built from the string returned from parseContent
     *
     * @param httpResponse the response from the http request.
     * @return a {@code JSONArray} from the {@code HttpResponse} object passed in.
     */
    public JSONArray parseJsonArray(HttpResponse httpResponse) throws IOException {
        String content = parseContent(httpResponse);
        return new JSONArray(content);
    }
}
