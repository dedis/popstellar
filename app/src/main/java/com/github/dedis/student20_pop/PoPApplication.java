package com.github.dedis.student20_pop;

import android.app.Application;
import android.content.Context;

import androidx.annotation.VisibleForTesting;

import com.github.dedis.student20_pop.model.Event;
import com.github.dedis.student20_pop.model.Lao;
import com.github.dedis.student20_pop.model.Person;

import java.util.ArrayList;
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
        return person;
    }

    /**
     *
     * @return list of LAOs corresponding to the user
     */
    public List<Lao> getLaos() {
        return new ArrayList<>(laoEventsMap.keySet());
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
        return laoEventsMap.get(lao);
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
        return currentLao;
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
    public Map<Lao, List<Event>> getLaoEventsMap(){ return laoEventsMap; }
}
