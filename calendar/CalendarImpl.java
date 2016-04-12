package com.ps.google.calendar;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;
import com.google.api.services.calendar.model.EventDateTime;
import com.ps.google.GoogleOauth2Impl;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import java.util.Iterator;

import org.json.JSONObject;

/**
 * Class which contains helper methods used to communicate with the Google Calendar API.
 */
public class CalendarImpl extends GoogleOauth2Impl {

    /** The main class used to make API calls to Calendar */
    protected Calendar service;

    /**
     * Default constructor.
     *
     * @param token the user's access token used for authentication.
     */
    public CalendarImpl(String token) {
        super(token);
        service = new Calendar.Builder(httpTransport, jsonFactory, credential).setApplicationName(APP_NAME).build();
    }

    /**
     * Used to retrieve a user's list of calendars.
     *
     * @return a {@link com.google.api.services.calendar.model.CalendarList} of the user's calendars.
     * @throws IOException
     */
    public CalendarList getCalendarList() throws IOException {
        return service.calendarList().list().execute();
    }

    public Event updateEvent(String calendarId,
                             String event) throws IOException {

        Logger.getLogger(CalendarImpl.class.getName()).log(Level.WARNING, event);
        Event content = new Event();
        JSONObject json = new JSONObject(event);
        String eventId = json.getString("id");
        content.setSummary(json.getString("title"));

        DateTime startDateTime = new DateTime(json.getString("start"));
        EventDateTime start = new EventDateTime().setDateTime(startDateTime);
        content.setStart(start);

        DateTime endDateTime = new DateTime(json.getString("end"));
        EventDateTime endTime = new EventDateTime().setDateTime(endDateTime);
        content.setEnd(endTime);
        //throw new IOException();
        return service.events().update(calendarId, eventId, content).execute();
    }
    public void deleteEvent(String calendarId,
                            String event) throws IOException {
        Logger.getLogger(CalendarImpl.class.getName()).log(Level.WARNING, event);
        JSONObject json = new JSONObject(event);
        String eventId = json.getString("id");
        Logger.getLogger(CalendarImpl.class.getName()).log(Level.WARNING, eventId);
        Logger.getLogger(CalendarImpl.class.getName()).log(Level.WARNING, calendarId);

        //throw new IOException();
        try {
        service.events().delete(calendarId, eventId).execute();
        } catch (IOException e) {
          Logger.getLogger(CalendarImpl.class.getName()).log(Level.WARNING, e.getMessage());

        }

    }
    public Event createEvent(String calendarId,
                             String event) throws IOException {
   Logger.getLogger(CalendarImpl.class.getName()).log(Level.WARNING, event);
        JSONObject json = new JSONObject(event);
        Event content = new Event().setSummary(json.getString("title"));

        DateTime startDateTime = new DateTime(json.getString("start"));
        EventDateTime start = new EventDateTime().setDateTime(startDateTime);
        content.setStart(start);

        DateTime endDateTime = new DateTime(json.getString("end"));
        EventDateTime end = new EventDateTime().setDateTime(endDateTime);
        content.setEnd(end);

        return service.events().insert(calendarId, content).execute();
    }

    /**
     * Used to retrieve a user's events in a specified calendar.
     *
     * @param calendarID the ID of the calendar from which to retrieve events.
     * @param max        the max time (exclusive) of events to search for.
     * @param min        the min time (exclusive) of events to search for.
     * @return an {@link com.google.api.services.calendar.model.Events} list of events.
     * @throws IOException
     */
    public Events getEvents(String calendarID, DateTime max, DateTime min) throws IOException {
        return getEvents(calendarID, max, min, true);
    }

    /**
     * Used to retrieve a user's events in a specified calendar.
     *
     * @param calendarID   the ID of the calendar from which to retrieve events.
     * @param max          the max time (exclusive) of events to search for.
     * @param min          the min time (exclusive) of events to search for.
     * @param singleEvents true to expand recurring events into instances and only return single one-off events and
     *                     instances of recurring events, but not the underlying recurring events themselves, false otherwise.
     * @return an {@link com.google.api.services.calendar.model.Events} list of events.
     * @throws IOException
     */
    public Events getEvents(String calendarID, DateTime max, DateTime min, boolean singleEvents) throws IOException {
        return service.events().list(calendarID).setTimeMax(max).setTimeMin(min).setSingleEvents(singleEvents).execute();
    }

    /**
     * Helper method used to get an initialized calender instance.
     *
     * @return an initialized calendar instance.
     */
    public static java.util.Calendar getInitializedCalendar() {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0);
        calendar.set(java.util.Calendar.MINUTE, 0);
        calendar.set(java.util.Calendar.SECOND, 0);
        calendar.set(java.util.Calendar.MILLISECOND, 0);
        return calendar;
    }
}
