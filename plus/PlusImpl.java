package com.ps.google.plus;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequestFactory;
import com.ps.google.GoogleOauth2Impl;
import org.json.JSONObject;

import java.io.IOException;

/**
 * Class which contains helper methods used to communicate with the Google Plus API.
 */
public class PlusImpl extends GoogleOauth2Impl {

    /** URLs */
    protected static final String ACTIVITIES_LIST_URL = "https://www.googleapis.com/plus/v1/people/me/activities/";

    /** The main class used to make HTTP requests */
    protected HttpRequestFactory requestFactory;

    /**
     * Default constructor.
     *
     * @param token the user's access token used for authentication.
     */
    public PlusImpl(String token) {
        super(token);
        requestFactory = httpTransport.createRequestFactory(credential);
    }

    /**
     * Retrieves the authorized user's activities.
     *
     * @param collection the collection to retrieve the activities from.
     * @return the authorized user's activities.
     * @throws IOException
     */
    public JSONObject getActivities(String collection) throws IOException {
        return parseJson(requestFactory.buildGetRequest(new GenericUrl(ACTIVITIES_LIST_URL + collection)).execute());
    }
}
