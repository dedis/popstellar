package com.github.dedis.student20_pop;

import android.annotation.SuppressLint;
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
import com.github.dedis.student20_pop.model.network.method.message.data.Data;
import com.github.dedis.student20_pop.model.network.method.message.data.lao.StateLao;
import com.github.dedis.student20_pop.model.network.method.message.data.meeting.StateMeeting;
import com.github.dedis.student20_pop.utility.protocol.DataHandler;
import com.github.dedis.student20_pop.utility.protocol.HighLevelProxy;
import com.github.dedis.student20_pop.utility.protocol.LowLevelProxy;
import com.github.dedis.student20_pop.utility.protocol.ProtocolProxyFactory;
import com.github.dedis.student20_pop.utility.security.PrivateInfoStorage;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    private final Map<URI, HighLevelProxy> openSessions = new HashMap<>();
    private final Map<String, Lao> laos = new HashMap<>();

    private final DataHandler dataHandler = new PoPDataHandler();

    private static Context appContext;

    private Person person;

    //represents the Lao which we are connected to, can be null
    private Lao currentLao;
    private HighLevelProxy localProxy;

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

        activateTestingValues(); //comment this line when testing with a back-end
        localProxy = getProxy(LOCAL_BACKEND_URI);
    }

    @SuppressLint("ApplySharedPref")
    @Override
    public void onTerminate() {
        super.onTerminate();
        SharedPreferences sp = this.getSharedPreferences(TAG, Context.MODE_PRIVATE);
        // Use commit instead of apply for information to be stored immediately
        sp.edit().putString(SP_PERSON_ID_KEY, person.getId()).commit();
    }

    /**
     * Returns PoP Application Context.
     */
    public static Context getAppContext() {
        return appContext;
    }

    /**
     * Returns Person corresponding to the user.
     */
    public Person getPerson() {
        return person;
    }

    /**
     * Returns the current LAO.
     */
    public Optional<Lao> getCurrentLao() {
        return Optional.ofNullable(currentLao);
    }

    /**
     * @return the current LAO, unsafe
     */
    public Lao getCurrentLaoUnsafe() {
        return currentLao;
    }

    /**
     * @return list of LAOs corresponding to the user
     */
    public List<Lao> getLaos() {
        return new ArrayList<>(laos.values());
    }

    /**
     * Get witnesses of the current LAO
     *
     * @return lao's corresponding list of witnesses
     */
    public List<String> getWitnesses() {
        return getCurrentLao().map(Lao::getWitnesses).orElseGet(ArrayList::new);
    }

    /**
     * Returns the proxy of the local device's backend.
     */
    public HighLevelProxy getLocalProxy() {
        return localProxy;
    }

    /**
     * Get the proxy for the given host
     * If the connection was not established yet, creates it.
     *
     * @param host of the backend
     * @return the proxy
     */
    public HighLevelProxy getProxy(URI host) {
        synchronized (openSessions) {
            if(openSessions.containsKey(host)) {
                return openSessions.get(host);
            } else {
                HighLevelProxy proxy = ProtocolProxyFactory.getInstance().createHighLevelProxy(host, person, dataHandler);
                openSessions.put(host, proxy);
                return proxy;
            }
        }
    }

    /**
     * Add a new LAO to the app
     *
     * @param lao to add
     */
    public void addLao(Lao lao) {
        laos.put(lao.getId(), lao);
    }

    /**
     * Set a Person for this Application, can only be done once
     *
     * @param person to be set for this Application
     */
    public void setPerson(Person person) {
        if (person != null) {
            this.person = person;
        }
    }

    /**
     * Sets the current LAO of this Application
     *
     * @param lao current LAO to be set
     */
    public void setCurrentLao(Lao lao) {
        this.currentLao = lao;
    }

    /**
     * @param event to be added to the current lao
     */
    public void addEvent(Event event) {
        getCurrentLao().ifPresent(lao -> {
            lao.addEvent(event);
            // TODO Call backend
        });
    }

    /**
     * Add a witness to the current LAO
     *
     * @param witness add witness to current lao
     * @return ADD_WITNESS_SUCCESSFUL if witness has been added
     * ADD_WITNESS_ALREADY_EXISTS if witness already exists
     */
    public AddWitnessResult addWitness(String witness) {
        return addWitness(currentLao, witness);
    }

    /**
     * Add a witness to a specified LAO
     *
     * @param lao     of the new witness
     * @param witness id to add on the list of witnesses for the LAO
     * @return ADD_WITNESS_SUCCESSFUL if witness has been added
     * ADD_WITNESS_ALREADY_EXISTS if witness already exists
     */
    public AddWitnessResult addWitness(Lao lao, String witness) {
        //TODO when connected to backend
        // send info to backend
        // If witness has been added return true, otherwise false

        if(lao.addWitness(witness)) {
            return ADD_WITNESS_SUCCESSFUL;
        } else {
            return ADD_WITNESS_ALREADY_EXISTS;
        }
    }

    /**
     * Add witnesses to the current LAO
     *
     * @param witnesses add witness to current lao
     * @return corresponding result for each witness in the list
     */
    public List<AddWitnessResult> addWitnesses(List<String> witnesses) {
        return addWitnesses(currentLao, witnesses);
    }

    /**
     * Add witnesses to a specified LAO
     *
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
     * Only useful when testing without a back-end.
     */
    public void activateTestingValues() {
        currentLao = new Lao("LAO I just joined", person.getId());
        dummyLaos();
    }

    /**
     * Handle received data messages inorder
     *
     * @param dataMessages List of received messages
     */
    public void handleDataMessages(List<Data> dataMessages) {
        for(Data data : dataMessages)
            data.accept(dataHandler);
    }

    /**
     * This method creates a map for testing, when no backend is connected.
     */
    private void dummyLaos() {
        String notMyPublicKey = new Keys().getPublicKey();
        Lao lao0 = new Lao("LAO I just joined", getPerson().getId());
        Lao lao1 = new Lao("LAO 1", notMyPublicKey);
        Lao lao2 = new Lao("LAO 2", notMyPublicKey);
        Lao lao3 = new Lao("My LAO 3", getPerson().getId());
        Lao lao4 = new Lao("LAO 4", notMyPublicKey);

        lao0.setEvents(dummyEvents(lao0.getId()));
        lao1.setEvents(dummyEvents(lao0.getId()));
        lao2.setEvents(dummyEvents(lao0.getId()));
        lao2.setEvents(dummyEvents(lao0.getId()));
        lao3.setEvents(dummyEvents(lao0.getId()));

        addLao(lao0);
        addLao(lao1);
        addLao(lao2);
        addLao(lao3);
        addLao(lao4);

        setCurrentLao(lao0);
    }

    private List<Event> dummyEvents(String laoId) {
        return Arrays.asList(
                new Event("Future Event 1", laoId, 2617547969L, "EPFL", POLL),
                new Event("Present Event 1", laoId, Instant.now().getEpochSecond(), "Somewhere", DISCUSSION),
                new Event("Past Event 1", laoId, 1481643086L, "Here", MEETING));
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

    /**
     * Type of results when adding a witness
     */
    public enum AddWitnessResult {
        ADD_WITNESS_SUCCESSFUL,
        ADD_WITNESS_ALREADY_EXISTS
    }

    /**
     * Data handler of the PoP application
     */
    private class PoPDataHandler implements DataHandler {

        @Override
        public void handle(StateLao stateLao) {
            Lao lao = laos.get(stateLao.getId());
            if(lao == null)
                lao = new Lao(stateLao.getName(),
                        stateLao.getCreation(),
                        stateLao.getId(),
                        stateLao.getOrganizer(),
                        stateLao.getWitnesses(),
                        new ArrayList<>(),
                        new ArrayList<>());
            else {
                lao.setName(stateLao.getName());
                lao.setWitnesses(stateLao.getWitnesses());
            }

            laos.put(lao.getId(), lao);
        }

        @Override
        public void handle(StateMeeting stateMeeting) {
            //TODO later in the project
        }
    }
}
