package com.ps.google.contacts;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.ps.google.GoogleOauth2Impl;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.gdata.client.Query;
import com.google.gdata.client.Service;
import com.google.gdata.client.contacts.ContactsService;
import com.google.gdata.client.http.HttpGDataRequest;
import com.google.gdata.data.DateTime;
import com.google.gdata.data.Link;
import com.google.gdata.data.contacts.ContactEntry;
import com.google.gdata.data.contacts.ContactFeed;
import com.google.gdata.data.contacts.ContactGroupEntry;
import com.google.gdata.data.contacts.ContactGroupFeed;
import com.google.gdata.data.extensions.PhoneNumber;
import com.google.gdata.data.extensions.FullName;
import com.google.gdata.data.extensions.Name;
import com.google.gdata.data.extensions.Email;
import com.google.gdata.data.extensions.StructuredPostalAddress;
import com.google.gdata.data.extensions.ExtendedProperty;
import com.google.gdata.util.AuthenticationException;
import com.google.gdata.util.NoLongerAvailableException;
import com.google.gdata.util.ServiceException;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.logging.ConsoleHandler;

import com.google.api.client.http.HttpContent;
import java.util.Map;
import java.lang.IllegalArgumentException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.http.json.JsonHttpContent;

/**
 * Class which contains helper methods used to communicate with the
 * Google Contacts API.
 */
public class ContactsImpl extends GoogleOauth2Impl {

    /** Feed URLs */
    protected static final String CONTACTS_FEED =
        "https://www.google.com/m8/feeds/contacts";
    protected static final String GROUPS_FEED =
        "https://www.google.com/m8/feeds/groups";

    private enum SystemGroup {
        MY_CONTACTS("Contacts", "My Contacts"),
        FRIENDS("Friends", "Friends"),
        FAMILY("Family", "Family"),
        COWORKERS("Coworkers", "Coworkers");

        private final String systemGroupId;
        private final String prettyName;

        SystemGroup(String systemGroupId, String prettyName) {
            this.systemGroupId = systemGroupId;
            this.prettyName = prettyName;
        }

        static SystemGroup fromSystemGroupId(String id) {
            for(SystemGroup group : SystemGroup.values()) {
                if (id.equals(group.systemGroupId)) {
                    return group;
                }
            }
            throw new IllegalArgumentException("Unrecognized system group id: " + id);
        }

        @Override
        public String toString() {
            return prettyName;
        }
    }

    private final String projection;
    protected ContactsService service;

    /**
     * Default constructor.
     *
     * @param token the user's access token used for authentication.
     */
    public ContactsImpl(String token) {
        super(token);

        service = new ContactsService("Focus Feed");

        //refresh the token just in case
        try {
            credential.refreshToken();
        } catch (IOException e) {
            Logger
                .getLogger(ContactsImpl.class.getName())
                .log(Level.WARNING, "refreshtoken ioerror ", e);
        }
        service.setOAuth2Credentials(credential);

        //stuff that may not be needed
        service.getRequestFactory().setHeader("User-Agent", "Focus Feed");//npe
        service.setHeader("Authorization", "Bearer " + token); //prevent npe
        service.setHeader("WWW-Authenticate", "junk"); //prevent npe [maybe]

        projection = "full"; //TODO make this a parameter
    }

    public void deleteContact(String id)
        throws IOException {
        log("DELETE param: " + id);
        try {
            getContactInternal(id).delete();
        } catch (ServiceException e) {
            log(e);
        } catch (java.lang.UnsupportedOperationException e) {
            log("If deletion is not supported for the target entry.");
        } catch (java.io.IOException e) {
            log("If there is an error communicating with the GData service.");
        }
    }

    private ContactEntry getContactInternal(String id)
        throws IOException, ServiceException {
        ContactEntry contact = null;
        id = id.replace("http://", "https://");
        //very important after 2015-10-7

        id = id.replace("/base/", "/" + projection + "/");
        contact = service.getEntry(new URL(id), ContactEntry.class);
        return contact;
    }

    private static URL buildContactsFeed(String userId, String projection)
        throws java.net.MalformedURLException {
        projection = projectionHelper(projection);
        String url = CONTACTS_FEED + "/" + userId + "/" + projection;
        return (new URL(url));
    }

    private static URL buildGroupsFeed(String userEmail, String projection)
        throws java.net.MalformedURLException {
        projection = projectionHelper(projection);
        String url = GROUPS_FEED + "/" + userEmail + "/" + projection;
        return (new URL(url));
    }

    private static URL buildGroupsFeed(String userEmail)
        throws java.net.MalformedURLException {
        return buildGroupsFeed(userEmail, null);
    }

    private static URL buildContactsFeed(String userId)
        throws java.net.MalformedURLException {
        return buildContactsFeed(userId, null);
    }

    private static URL buildGroupsFeed()
        throws java.net.MalformedURLException {
        return buildGroupsFeed("default");
    }

    private static URL buildContactsFeed()
        throws java.net.MalformedURLException {
        return buildContactsFeed("default");
    }

    private static String projectionHelper(String projection) {
        if (projection == null) {
            projection = "thin";
        } else if (!projection.equals("full")) {
            projection = "property-" + projection;
        }
        return projection;
    }

    /**
     * Used to retrieve a user's contacts.
     *
     * @return a user's contacts in a JSON formatted string.
     * @throws IOException
     */
    public JSONObject getContacts() throws IOException, ServiceException {
        URL feedUrl = buildContactsFeed();
        ContactFeed resultFeed;
        Link link;
        JSONObject feed = new JSONObject();
        JSONObject entryObject = new JSONObject();
        feed.put("feed", entryObject);
        JSONArray entryArray = new JSONArray();
        entryObject.put("entry", entryArray);

        do {
            //objectify entries
            resultFeed = service.getFeed(feedUrl, ContactFeed.class);
            List<ContactEntry> entries = resultFeed.getEntries();
            //log(entries.size());

            //loop each 25 entries
            for (ContactEntry entry : entries) {
                JSONObject json = entryToJSON(entry);
                entryArray.put(json);
            }

            //get next 25 entries
            //link = resultFeed.getNextLink();
            link = null; //stop breaking google
            if (link == null) {
                feedUrl = null;
            } else {
                feedUrl = new URL(link.getHref());
            }
        } while (feedUrl != null);

        return feed;
    }

    //convert entry object to json object
    private static JSONObject entryToJSON(ContactEntry contact) {
        JSONObject response = new JSONObject();

        JSONArray a = null;

        a = new JSONArray();
        for (Email email : contact.getEmailAddresses()) {
            a.put(email.getAddress());
        }
        response.put("emailAddresses", a);

        a = new JSONArray();
        for (PhoneNumber email : contact.getPhoneNumbers()) {
            a.put(email.getPhoneNumber());
        }
        response.put("phoneNumbers", a);

        a = new JSONArray();
        for (StructuredPostalAddress email :
                 contact.getStructuredPostalAddresses()) {
            JSONObject obj = new JSONObject();
            if (email.hasStreet())
                obj.put("street", email.getStreet().getValue());
            if (email.hasPobox())
                obj.put("poBox", email.getPobox().getValue());
            if (email.hasNeighborhood())
                obj.put("neighborhood", email.getNeighborhood().getValue());
            if (email.hasCity())
                obj.put("city", email.getCity().getValue());
            if (email.hasRegion())
                obj.put("region", email.getRegion().getValue());
            if (email.hasPostcode())
                obj.put("postCode", email.getPostcode().getValue());
            if (email.hasCountry())
                obj.put("country", email.getCountry().getValue());
            a.put(obj);
        }
        response.put("postalAddresses", a);

        //add name
        if (contact.hasName()) {
            Name name = contact.getName();

            if (name.hasFullName()) {
                FullName fullName = name.getFullName();
                String value = fullName.getValue();
                response.put("name", value);
            }
        }

        //add id
        response.put("id", contact.getId());

        return response;
    }

    //GET contact
    public JSONObject getContact(String contactId) throws IOException,
    com.google.gdata.util.ServiceException {
        ContactEntry contact = getContactInternal(contactId);
        JSONObject response = entryToJSON(contact);
        return response;
    }

    //PUT contact
    public JSONObject putContact(
                                 String contactId,
                                 JSONObject contactP)
        throws IOException, com.google.gdata.util.ServiceException {
        ContactEntry contact = getContactInternal(contactId);
        JSONObject response;
        JSONArray a = null;
        log("PUT param: " + contactP);

        a = contactP.getJSONArray("emailAddresses");
        int i = 0;
        for (Email email : contact.getEmailAddresses()) {
            email.setAddress(a.getString(i++));
        }

        a = contactP.getJSONArray("phoneNumbers");
        i = 0;
        for (PhoneNumber email : contact.getPhoneNumbers()) {
            email.setPhoneNumber(a.getString(i++));
        }

        a = contactP.getJSONArray("postalAddresses");
        i = 0;
        for (StructuredPostalAddress email :
                 contact.getStructuredPostalAddresses()) {
            JSONObject obj = a.getJSONObject(i++);
            if (obj.has("street"))
                email.getStreet().setValue(obj.getString("street"));
            if (obj.has("poBox"))
                email.getPobox().setValue(obj.getString("poBox"));
            if (obj.has("neighborhood"))
                email.getNeighborhood()
                    .setValue(obj.getString("neighborhood"));
            if (obj.has("city"))
                email.getCity().setValue(obj.getString("city"));
            if (obj.has("region"))
                email.getRegion().setValue(obj.getString("region"));
            if (obj.has("postCode"))
                email.getPostcode().setValue(obj.getString("postCode"));
            if (obj.has("country"))
                email.getCountry().setValue(obj.getString("country"));
        }

        response = entryToJSON(contact.update());
        log(response);

        return response;
    }

    //POST contact
    public JSONObject postContact(String contactP) throws IOException,
    com.google.gdata.util.ServiceException {
        JSONObject response = new JSONObject();
        //TODO: not implemented yet
        return response;
    }

    //helper for debugging
    private static void log(Object s) {
        Logger
            .getLogger(ContactsImpl.class.getName())
            .log(Level.WARNING, s.toString());
    }
}
