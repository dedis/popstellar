package com.github.dedis.student20_pop.model;

import java.util.Date;
import java.util.List;
import java.util.Objects;


/**
 * Class modeling an Event
 */
public class Event {

    private long uid;
    private java.util.Date creationTime;
    private String name;
    private String ownerLaoUid; //SHA1(Name + Creation Date/Time Unix Timestamp -> what's the best type?
    private List<String> attendees;
    private java.util.Date startTime;
    private java.util.Date endTime;
    private String location;
    private EventType type;
    private EventCategory category;


    /**
     * Whether the event is in the past, present or future
     */
    public enum EventCategory {
        PAST, PRESENT, FUTURE
    }

    //Later, create one subclass for each type if necessary
    public enum EventType{
        MEETING, ROLL_CALL, POLL, DISCUSSION
    }


    /**
     * Constructor for an Event
     *
     * @param uid
     * @param creationTime
     * @param name
     * @param ownerLaoUid
     * @param attendees
     * @param startTime
     * @param endTime
     * @param location
     * @param type
     * @param category
     */
    public Event(long uid, Date creationTime, String name, String ownerLaoUid, List<String> attendees, Date startTime, Date endTime,
                 String location, EventType type, EventCategory category) {
        this.creationTime = creationTime;
        this.uid = uid;
        this.name = name;
        this.ownerLaoUid = ownerLaoUid;
        this.attendees = attendees;
        this.startTime = startTime;
        this.endTime = endTime;
        this.location = location;
        this.type = type;
        this.category = category;
    }

    public Event(String name, String location, EventType type, EventCategory category){
        this.startTime = new Date();
        this.name = name;
        this.location = location;
        this.type = type;
        this.category = category;
    }

    public EventCategory getCategory() {
        return category;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Event event = (Event) o;
        return uid == event.uid;
    }

    @Override
    public int hashCode() {
        return Objects.hash(uid);
    }

    public String getTitleString(){
        StringBuilder sb = new StringBuilder();
        sb.append(name);
        sb.append(" : ");
        /*sb.append(type.toString());*/
        return sb.toString();
    }

    public String getDescriptionString(){
        StringBuilder sb = new StringBuilder();
        sb.append("Where: ");
        sb.append(location);
        /*sb.append(" - When: ");
        sb.append(time.toString());*/
        return sb.toString();
    }


}
