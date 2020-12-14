package com.github.dedis.student20_pop;

import android.app.Application;
import android.content.Context;

import androidx.annotation.VisibleForTesting;

import com.github.dedis.student20_pop.model.Event;
import com.github.dedis.student20_pop.model.Keys;
import com.github.dedis.student20_pop.model.Lao;
import com.github.dedis.student20_pop.model.Person;
import com.github.dedis.student20_pop.utility.security.Hash;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class modelling the application : a unique person associated with LAOs
 */
public class PoPApplication extends Application {

    private static Context appContext;
    private Person person;
    private Map<Lao, List<Event>> laoEventsMap;

    //represents the Lao which we are connected to, can be null
    private Lao currentLao;

    //TODO: person/laos used for testing when we don't have a backend connected
    private Person dummyPerson;
    private Lao dummyLao;
    private Map<Lao, List<Event>> dummyLaoEventsMap;

    @Override
    public void onCreate() {
        super.onCreate();

        appContext = getApplicationContext();

        if(person == null) {
            // TODO: when can the user change/choose its name
            setPerson(new Person("USER"));
        }
        if(laoEventsMap == null){
            laoEventsMap = new HashMap<>();
        }
        dummyPerson =  new Person("name");
        dummyLao = new Lao("LAO I just joined", new Date(), dummyPerson.getId());
        dummyLaoEventsMap = dummyMap();
    }

    /**
     *
     * @return PoP Application Context
     */
    public static Context getAppContext() {
        return appContext;
    }

    /**
     *
     * @return Person corresponding to the user
     */
    public Person getPerson() {
        return dummyPerson;
        //TODO when connected to backend
        //return person;
    }

    /**
     *
     * @return list of LAOs corresponding to the user
     */
    public List<Lao> getLaos() {
        return new ArrayList<>(dummyLaoEventsMap.keySet());
        //TODO when connected to backend
        //return new ArrayList<>(laoEventsMap.keySet());
    }

    /**
     *
     * sets the person for this Application, can only be done once
     * @param person
     */
    public void setPerson(Person person) {
        if(person != null) {
            this.person = person;
        }
    }

    /**
     *
     * adds a Lao to the app
     * @param lao
     */
    public void addLao(Lao lao){
        if (!laoEventsMap.containsKey(lao)) {
            this.laoEventsMap.put(lao, new ArrayList<>());
        }
    }

    /**
     *
     * @return the list of Events associated with the given LAO, null if lao is not in the map
     */
    public List<Event> getEvents(Lao lao){
        return dummyLaoEventsMap.get(lao);
        //TODO when connected to backend
        //return laoEventsMap.get(lao);
    }

    /**
     *
     * adds an event e to the list of events of the LAO lao
     * @param lao
     * @param e
     */
    public void addEvent(Lao lao, Event e){
        getEvents(lao).add(e);
    }

    /**
     *
     * @return the current lao
     */
    public Lao getCurrentLao(){
        return dummyLao;
        //TODO when connected to backend
        //return currentLao;
    }

    /**
     *
     * sets the current lao
     * @param lao
     */
    public void setCurrentLao(Lao lao){
        this.currentLao = lao;
    }

    /**
     *
     * @return map of LAOs as keys and lists of events corresponding to the lao as values
     */
    public Map<Lao, List<Event>> getLaoEventsMap(){
        return dummyLaoEventsMap;
        //TODO when connected to backend
        //return laoEventsMap;
    }


    /**
     *
     * This method creates a map for testing, when no backend is connected
     * @return the dummy map
     */
    private Map<Lao, List<Event>> dummyMap(){
        Map<Lao, List<Event>> map = new HashMap<>();
        List<Event> events = new ArrayList<>();
        Event event1 = new Event("Future Event 1", new Date(2617547969000L), new Keys().getPublicKey(), "EPFL", "Poll");
        Event event2 = new Event("Present Event 1", new Date(), new Keys().getPublicKey(), "Somewhere", "Discussion");
        Event event3 = new Event("Past Event 1", new Date(1481643086000L), new Keys().getPublicKey(), "Here", "Meeting");
        events.add(event1);
        events.add(event2);
        events.add(event3);

        String notMyPublicKey = new Keys().getPublicKey();

        map.put(dummyLao, events);
        map.put(new Lao("LAO 1", new Date(), notMyPublicKey), events);
        map.put(new Lao("LAO 2", new Date(), notMyPublicKey), events);
        map.put(new Lao("My LAO 3", new Date(), dummyPerson.getId()), events);
        map.put(new Lao("LAO 4", new Date(), notMyPublicKey), events);
        return map;
    }

}
