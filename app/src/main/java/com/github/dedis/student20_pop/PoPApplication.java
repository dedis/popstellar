package com.github.dedis.student20_pop;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.github.dedis.student20_pop.model.Lao;
import com.github.dedis.student20_pop.model.Person;
import com.github.dedis.student20_pop.utility.security.PrivateInfoStorage;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

public class PoPApplication extends Application {

    public static final String TAG = PoPApplication.class.getSimpleName();

    public static final String USERNAME = "USERNAME"; //TODO: let user choose/change its name
    public static final String SP_PERSON_ID_KEY = "SHARED_PREFERENCES_PERSON_ID";
    public static final String SP_LAOS_KEY = "SHARED_PREFERENCES_LAOS";

    private Person person;
    private List<Lao> laos;

    @Override
    public void onCreate() {
        super.onCreate();

        Gson gson = new Gson();
        SharedPreferences sp = this.getSharedPreferences(TAG, Context.MODE_PRIVATE);

        // Verify if the information is not present
        if(person == null && laos == null) {
            // Verify if the user already exists
            if(sp.contains(SP_PERSON_ID_KEY) && sp.contains(SP_LAOS_KEY)) {
                // Recover user's information
                String id = sp.getString(SP_PERSON_ID_KEY, "");
                String authentication = PrivateInfoStorage.readData(this, id);
                if (authentication == null) {
                    person = new Person(USERNAME);
                    Log.d(TAG, "Private key of user cannot be accessed, new key pair is created");
                }
                else {
                    person = new Person(USERNAME, id, authentication, Lao.getIds(laos));
                }
                // Recover LAO's information
                laos = gson.fromJson(sp.getString(SP_LAOS_KEY, ""), List.class);
            }
            else {
                // Create new user and list of LAOs
                person = new Person(USERNAME);
                laos = new ArrayList<>();
                // Store private key of user
                if (PrivateInfoStorage.storeData(this, person.getId(), person.getAuthentication()))
                    Log.d(TAG, "Stored private key of organizer");
            }
        }
    }

    @Override
    public void onTerminate() {
        super.onTerminate();

        SharedPreferences sp = this.getSharedPreferences(TAG, Context.MODE_PRIVATE);

        // Use commit for information to be stored immediately
        sp.edit().putString(SP_PERSON_ID_KEY, person.getId()).commit();
        sp.edit().putString(SP_LAOS_KEY, (new Gson()).toJson(laos)).commit();
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
        return laos;
    }

    /**
     *
     * @param person Person model, can't be null
     */
    public void setPerson(Person person) {
        if(person != null) {
            this.person = person;
        }
    }

    /**
     *
     * @param laos list of LAOs, can't be null
     */
    public void setLaos(List<Lao> laos) {
        if(laos != null) {
            this.laos = laos;
        }
    }
}
