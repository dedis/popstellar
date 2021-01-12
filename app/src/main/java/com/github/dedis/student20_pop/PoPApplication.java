package com.github.dedis.student20_pop;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.github.dedis.student20_pop.model.Keys;
import com.github.dedis.student20_pop.model.Lao;
import com.github.dedis.student20_pop.model.Person;
import com.github.dedis.student20_pop.model.event.Event;
import com.github.dedis.student20_pop.utility.protocol.HighLevelProxy;
import com.github.dedis.student20_pop.utility.protocol.LowLevelProxy;
import com.github.dedis.student20_pop.utility.protocol.ProtocolProxyFactory;
import com.github.dedis.student20_pop.utility.security.PrivateInfoStorage;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.dedis.student20_pop.PoPApplication.AddWitnessResult.ADD_WITNESS_ALREADY_EXISTS;
import static com.github.dedis.student20_pop.PoPApplication.AddWitnessResult.ADD_WITNESS_SUCCESSFUL;
import static com.github.dedis.student20_pop.model.event.EventType.DISCUSSION;
import static com.github.dedis.student20_pop.model.event.EventType.MEETING;
import static com.github.dedis.student20_pop.model.event.EventType.POLL;

/**
 * Class modelling the application : a unique person associated with LAOs
 */
public class PoPApplication extends Application {
    public static final String TAG = PoPApplication.class.getSimpleName();
    public static final String SP_PERSON_ID_KEY = "SHARED_PREFERENCES_PERSON_ID";
    public static final String USERNAME = "USERNAME";

    private static final URI LOCAL_BACKEND_URI = URI.create("ws://10.0.2.2:2000");

    private final Map<Lao, List<Event>> laoEventsMap = new HashMap<>();
    private final Map<Lao, List<String>> laoWitnessMap = new HashMap<>();
    private final Map<URI, HighLevelProxy> openSessions = new HashMap<>();

    private static Context appContext;

    private Person person;

    //represents the Lao which we are connected to, can be null
    private Lao currentLao;
    private HighLevelProxy localProxy;

    //TODO: person/laos used for testing when we don't have a backend connected
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

        startPurgeRoutine(new Handler(Looper.getMainLooper()));

        appContext = getApplicationContext();

        SharedPreferences sp = this.getSharedPreferences(TAG, Context.MODE_PRIVATE);

        // Verify if the information is not present
        if (person == null) {
            // Verify if the user already exists
            if (sp.contains(SP_PERSON_ID_KEY)) {
                // Recover user's information
                String id = sp.getString(SP_PERSON_ID_KEY, "");
                String authentication = PrivateInfoStorage.readData(this, id);
                if (authentication == null) {
                    person = new Person(USERNAME);
                    Log.d(TAG, "Private key of user cannot be accessed, new key pair is created");
                    if (PrivateInfoStorage.storeData(this, person.getId(), person.getAuthentication()))
                        Log.d(TAG, "Stored private key of organizer");
                } else {
                    person = new Person(USERNAME, id, authentication, new ArrayList<>());
                }
            } else {
                // Create new user
                person = new Person(USERNAME);
                // Store private key of user
                if (PrivateInfoStorage.storeData(this, person.getId(), person.getAuthentication()))
                    Log.d(TAG, "Stored private key of organizer");
            }
        }

        currentLao = new Lao("LAO I just joined", person.getId());
        dummyLaoEventsMap = dummyLaoEventMap();
        laoWitnessMap.put(currentLao, new ArrayList<>());

        localProxy = getProxy(LOCAL_BACKEND_URI);
    }

    /**
     * Start the routine the will purge periodically every open session to close timeout requests
     *
     * @param handler to run the routine on
     */
    private void startPurgeRoutine(Handler handler) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (openSessions) {
                    openSessions.values().forEach(hlp -> hlp.lowLevel().purgeTimeoutRequests());
                    handler.postDelayed(this, LowLevelProxy.REQUEST_TIMEOUT);
                }
            }
        });
    }

    @Override
    public void onTerminate() {
        super.onTerminate();

        SharedPreferences sp = this.getSharedPreferences(TAG, Context.MODE_PRIVATE);

        // Use commit for information to be stored immediately
        sp.edit().putString(SP_PERSON_ID_KEY, person.getId()).commit();
    }

    /**
     * @return Person corresponding to the user
     */
    public Person getPerson() {
        return person;
    }

    /**
     * @param person to be set for this Application, can only be done once
     */
    public void setPerson(Person person) {
        if (person != null) {
            this.person = person;
        }
    }

    /**
     * @return the current lao
     */
    public Lao getCurrentLao() {
        return  currentLao;
    }

    /**
     * Sets the current lao
     *
     * @param lao
     */
    public void setCurrentLao(Lao lao) {
        this.currentLao = lao;
    }

    /**
     * @return list of LAOs corresponding to the user
     */
    public List<Lao> getLaos() {
        return new ArrayList<>(laoEventsMap.keySet());
    }

    /**
     * @return map of LAOs as keys and lists of events corresponding to the lao as values
     */
    public Map<Lao, List<Event>> getLaoEventsMap() {
        return laoEventsMap;
    }

    /**
     * @return the list of Events associated with the given LAO, null if lao is not in the map
     */
    public List<Event> getEvents(Lao lao) {
        return laoEventsMap.get(lao);
    }

    /**
     * Get witnesses of current LAO
     *
     * @return lao's corresponding list of witnesses
     */
    public List<String> getWitnesses() {
        return laoWitnessMap.get(currentLao);
    }

    /**
     * @param lao from where we want to get the witnesses
     * @return lao's corresponding list of witnesses
     */
    public List<String> getWitnesses(Lao lao) {
        List<String> laoWitnesses = laoWitnessMap.get(lao);
        if (laoWitnesses == null) {
            laoWitnesses = new ArrayList<>();
            laoWitnessMap.put(lao, laoWitnesses);
        }

        return laoWitnessMap.get(lao);
    }

    /**
     * Get the map of LAOs and its witnesses
     */
    public Map<Lao, List<String>> getLaoWitnessMap() {
        return laoWitnessMap;
    }

    /**
     * Get the proxy of the local device's backend
     *
     * @return the proxy
     */
    public HighLevelProxy getLocalProxy() {
        return localProxy;
    }

    /**
     * Get the proxy for the given host. If the connection was not established yet, creates it.
     *
     * @param host of the backend
     * @return the proxy
     */
    public HighLevelProxy getProxy(URI host) {
        synchronized (openSessions) {
            if(openSessions.containsKey(host)) {
                return openSessions.get(host);
            } else {
                HighLevelProxy proxy = ProtocolProxyFactory.getInstance().createHighLevelProxy(host, person);
                openSessions.put(host, proxy);
                return proxy;
            }
        }
    }

    /**
     * @param lao to add to the app
     */
    public void addLao(Lao lao) {
        if (!laoEventsMap.containsKey(lao)) {
            this.laoEventsMap.put(lao, new ArrayList<>());
        }
    }

    /**
     * @param event to be added to the current lao
     */
    public void addEvent(Event event) {
        addEvent(getCurrentLao(), event);
    }

    /**
     * @param lao   of the new event
     * @param event to be added
     */
    public void addEvent(Lao lao, Event event) {
        getEvents(lao).add(event);
    }

    /**
     * @param witness add witness to current lao
     * @return ADD_WITNESS_SUCCESSFUL if witness has been added
     * ADD_WITNESS_ALREADY_EXISTS if witness already exists
     */
    public AddWitnessResult addWitness(String witness) {
        return addWitness(currentLao, witness);
    }

    /**
     * @param lao     of the new witness
     * @param witness id to add on the list of witnesses for the LAO
     * @return ADD_WITNESS_SUCCESSFUL if witness has been added
     * ADD_WITNESS_ALREADY_EXISTS if witness already exists
     */
    public AddWitnessResult addWitness(Lao lao, String witness) {
        //TODO when connected to backend
        // send info to backend
        // If witness has been added return true, otherwise false

        List<String> laoWitnesses = laoWitnessMap.get(lao);
        if (laoWitnesses == null) {
            laoWitnesses = new ArrayList<>();
            laoWitnessMap.put(lao, laoWitnesses);
        }

        if (laoWitnesses.contains(witness)) {
            return ADD_WITNESS_ALREADY_EXISTS;
        }

        laoWitnesses.add(witness);

        return ADD_WITNESS_SUCCESSFUL;
    }

    /**
     * @param witnesses add witness to current lao
     * @return corresponding result for each witness in the list
     */
    public List<AddWitnessResult> addWitnesses(List<String> witnesses) {
        return addWitnesses(currentLao, witnesses);
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

        map.put(currentLao, events);
        map.put(new Lao("LAO 1", notMyPublicKey), events);
        map.put(new Lao("LAO 2", notMyPublicKey), events);
        map.put(new Lao("My LAO 3", person.getId()), events);
        map.put(new Lao("LAO 4", notMyPublicKey), events);
        return map;
    }

    /**
     * Type of results when adding a witness
     */
    public enum AddWitnessResult {
        ADD_WITNESS_SUCCESSFUL,
        ADD_WITNESS_ALREADY_EXISTS
    }
}
