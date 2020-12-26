package com.github.dedis.student20_pop;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.github.dedis.student20_pop.model.Event;
import com.github.dedis.student20_pop.model.Keys;
import com.github.dedis.student20_pop.model.Lao;
import com.github.dedis.student20_pop.model.Person;
import com.github.dedis.student20_pop.utility.network.HighLevelClientProxy;
import com.github.dedis.student20_pop.utility.network.PoPClientEndpoint;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.github.dedis.student20_pop.PoPApplication.AddWitnessResult.ADD_WITNESS_ALREADY_EXISTS;
import static com.github.dedis.student20_pop.PoPApplication.AddWitnessResult.ADD_WITNESS_SUCCESSFUL;
import static com.github.dedis.student20_pop.model.Event.EventType.DISCUSSION;
import static com.github.dedis.student20_pop.model.Event.EventType.MEETING;
import static com.github.dedis.student20_pop.model.Event.EventType.POLL;

/**
 * Class modelling the application : a unique person associated with LAOs
 */
public class PoPApplication extends Application {
    public static final String TAG = PoPApplication.class.getSimpleName();
    public static final String USERNAME = "USERNAME"; //TODO: let user choose/change its name
    private static final String LOCAL_BACKEND_URI = "ws://10.0.2.2:2000";

    private static Context appContext;
    private Person person;
    private Map<Lao, List<Event>> laoEventsMap;
    private Map<Lao, List<String>> laoWitnessMap;
    private Map<URI, HighLevelClientProxy> openSessions;

    //represents the Lao which we are connected to, can be null
    private Lao currentLao;
    private HighLevelClientProxy localProxy;

    //TODO: person/laos used for testing when we don't have a backend connected
    private Person dummyPerson;
    private Lao dummyLao;
    private Map<Lao, List<Event>> dummyLaoEventsMap;

    /**
     * @return PoP Application Context
     */
    public static Context getAppContext() {
        return appContext;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        PoPClientEndpoint.startPurgeRoutine(new Handler(Looper.getMainLooper()));

        appContext = getApplicationContext();

        if (person == null) {
            // TODO: when can the user change/choose its name
            setPerson(new Person(USERNAME));
        }

        if (laoEventsMap == null) {
            laoEventsMap = new HashMap<>();
        }

        if (laoWitnessMap == null) {
            this.laoWitnessMap = new HashMap<>();
        }

        dummyPerson = new Person("name");
        dummyLao = new Lao("LAO I just joined", new Date(), dummyPerson.getId());
        dummyLaoEventsMap = dummyLaoEventMap();
        laoWitnessMap.put(dummyLao, new ArrayList<>());

        localProxy = PoPClientEndpoint.connect(URI.create(LOCAL_BACKEND_URI), person);
    }

    /**
     * @return Person corresponding to the user
     */
    public Person getPerson() {
        return dummyPerson;
        //TODO when connected to backend
        //return person;
    }

    /**
     * sets the person for this Application, can only be done once
     *
     * @param person
     */
    public void setPerson(Person person) {
        if (person != null) {
            this.person = person;
        }
    }

    /**
     * @return list of LAOs corresponding to the user
     */
    public List<Lao> getLaos() {
        return new ArrayList<>(dummyLaoEventsMap.keySet());
        //TODO when connected to backend
        //return new ArrayList<>(laoEventsMap.keySet());
    }

    /**
     * adds a Lao to the app
     *
     * @param lao
     */
    public void addLao(Lao lao) {
        if (!laoEventsMap.containsKey(lao)) {
            this.laoEventsMap.put(lao, new ArrayList<>());
        }
    }

    /**
     * @return the list of Events associated with the given LAO, null if lao is not in the map
     */
    public List<Event> getEvents(Lao lao) {
        return dummyLaoEventsMap.get(lao);
        //TODO when connected to backend
        //return laoEventsMap.get(lao);
    }

    /**
     * adds an event e to the list of events of the LAO lao
     *
     * @param lao
     * @param e
     */
    public void addEvent(Lao lao, Event e) {
        getEvents(lao).add(e);
    }

    /**
     * adds an event e to the list of events of current lao
     *
     * @param event
     */
    public void addEvent(Event event) {
        addEvent(getCurrentLao(), event);
    }

    /**
     * @return the current lao
     */
    public Lao getCurrentLao() {
        return dummyLao;
        //TODO when connected to backend
        //return currentLao;
    }

    /**
     * sets the current lao
     *
     * @param lao
     */
    public void setCurrentLao(Lao lao) {
        this.currentLao = lao;
    }

    /**
     * Get the proxy of the local device's backend
     *
     * @return the proxy
     */
    public HighLevelClientProxy getLocalProxy() {
        return localProxy;
    }

    /**
     * @return map of LAOs as keys and lists of events corresponding to the lao as values
     */
    public Map<Lao, List<Event>> getLaoEventsMap() {
        return dummyLaoEventsMap;
        //TODO when connected to backend
        //return laoEventsMap;
    }

    /**
     * This method creates a map for testing, when no backend is connected
     *
     * @return the dummy map
     */
    private Map<Lao, List<Event>> dummyLaoEventMap() {
        Map<Lao, List<Event>> map = new HashMap<>();
        List<Event> events = new ArrayList<>();
        Event event1 = new Event("Future Event 1", new Date(2617547969000L), new Keys().getPublicKey(), "EPFL", POLL);
        Event event2 = new Event("Present Event 1", new Date(), new Keys().getPublicKey(), "Somewhere", DISCUSSION);
        Event event3 = new Event("Past Event 1", new Date(1481643086000L), new Keys().getPublicKey(), "Here", MEETING);
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

    /**
     * Add witness' id to lao
     *
     * @param lao
     * @param witness
     * @return ADD_WITNESS_SUCCESSFUL if witness has been added
     * ADD_WITNESS_ALREADY_EXISTS if witness already exists
     */
    public AddWitnessResult addWitness(Lao lao, String witness) {
        //TODO when connected to backend
        // send info to backend
        // If witness has been added return true, otherwise false

        // List<String> laoWitnesses = laoWitnessMap.get(laoId);
        List<String> laoWitnesses = laoWitnessMap.get(lao);
        if (laoWitnesses == null) {
            laoWitnesses = new ArrayList<>();
            laoWitnessMap.put(lao, laoWitnesses);
            //laoWitnessMap.put(laoId, laoWitnesses);
        }

        if (laoWitnesses.contains(witness))
            return ADD_WITNESS_ALREADY_EXISTS;


        laoWitnesses.add(witness);

        return ADD_WITNESS_SUCCESSFUL;
    }

    /**
     * @param witness add witness to current lao
     * @return ADD_WITNESS_SUCCESSFUL if witness has been added
     * ADD_WITNESS_ALREADY_EXISTS if witness already exists
     */
    public AddWitnessResult addWitness(String witness) {
        return addWitness(dummyLao, witness);
        //TODO when connected to backend
        //addWitness(currentLao, witness);
    }

    /**
     * @param witnesses add witness to current lao
     * @return corresponding result for each witness in the list
     */
    public List<AddWitnessResult> addWitnesses(List<String> witnesses) {
        return addWitnesses(dummyLao, witnesses);
        //TODO when connected to backend
        //addWitnesses(currentLao, witness);
    }

    /**
     * @param witnesses add witness to current lao
     * @return corresponding result for each witness in the list
     */
    public List<AddWitnessResult> addWitnesses(Lao lao, List<String> witnesses) {
        List<AddWitnessResult> results = new ArrayList<>();
        for (String witness : witnesses) {
            results.add(addWitness(lao, witness));
        }
        return results;
    }

    /**
     * Get witnesses of a LAO
     *
     * @param lao
     * @return lao's corresponding list of witnesses
     */
    public List<String> getWitnesses(Lao lao) {
        return laoWitnessMap.get(lao);
    }

    /**
     * Get witnesses of current LAO
     *
     * @return lao's corresponding list of witnesses
     */
    public List<String> getWitnesses() {
        return laoWitnessMap.get(dummyLao);
        //TODO when connected to backend
        //return laoWitnessMap.get(currentLao);
    }

    /**
     * Get Lao -> Witnesses HashMap
     */
    public Map<Lao, List<String>> getLaoWitnessMap() {
        return laoWitnessMap;
    }

    public enum AddWitnessResult {
        ADD_WITNESS_SUCCESSFUL,
        ADD_WITNESS_ALREADY_EXISTS
    }
}
