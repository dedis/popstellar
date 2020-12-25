package com.github.dedis.student20_pop;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.test.core.app.ActivityScenario;

import com.github.dedis.student20_pop.model.Lao;
import com.github.dedis.student20_pop.model.Person;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import static androidx.test.espresso.matcher.ViewMatchers.assertThat;
import static com.github.dedis.student20_pop.PoPApplication.SP_PERSON_ID_KEY;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.util.Collections;
import java.util.List;

import static com.github.dedis.student20_pop.PoPApplication.AddWitnessResult;
import static com.github.dedis.student20_pop.PoPApplication.AddWitnessResult.*;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertNull;

public class PoPApplicationTest {

    private final Person person = new Person(PoPApplication.USERNAME);
    private final Lao lao1 = new Lao("LAO1", new Date(), person.getId());
    private final String witness1 = "Alphonse";
    private final String witness2 = "Bertrand";
    private final ArrayList<String> witnesses = new ArrayList<>(Arrays.asList(witness1, witness2));

    @Test
    public void terminateAppSavesInfoTest() {
        ActivityScenario.launch(MainActivity .class).onActivity(a -> {
            PoPApplication app = (PoPApplication) a.getApplication();
            SharedPreferences sp = app.getSharedPreferences(PoPApplication.TAG, Context.MODE_PRIVATE);

            app.setPerson(person);
            app.onTerminate();

            assertTrue(sp.contains(SP_PERSON_ID_KEY));
            });
    }

    @Test
    public void canAddOneWitnessToLAO() {
        ActivityScenario.launch(MainActivity.class).onActivity(a -> {
            PoPApplication app = (PoPApplication) a.getApplication();
            assertNull(app.getWitnesses(lao1));
            AddWitnessResult result = app.addWitness(lao1, witness1);
            assertThat(app.getWitnesses(lao1), is(Collections.singletonList(witness1)));
            assertThat(result, is(ADD_WITNESS_SUCCESSFUL));
        });
    }

    @Test
    public void canAddOneWitnessToCurrentLAO() {
        ActivityScenario.launch(MainActivity.class).onActivity(a -> {
            PoPApplication app = (PoPApplication) a.getApplication();
            assertThat(app.getWitnesses(), is(empty()));
            AddWitnessResult result = app.addWitness(witness1);
            assertThat(app.getWitnesses(), is(Collections.singletonList(witness1)));
            assertThat(result, is(ADD_WITNESS_SUCCESSFUL));
        });
    }

    @Test
    public void canAddWitnessesToLAO() {
        ActivityScenario.launch(MainActivity.class).onActivity(a -> {
            PoPApplication app = (PoPApplication) a.getApplication();
            assertNull(app.getWitnesses(lao1));
            List<AddWitnessResult> results = app.addWitnesses(lao1, witnesses);
            assertThat(app.getWitnesses(lao1), is(witnesses));
            for (AddWitnessResult result : results) {
                assertThat(result, is(ADD_WITNESS_SUCCESSFUL));
            }
        });
    }

    @Test
    public void canAddWitnessesToCurrentLAO() {
        ActivityScenario.launch(MainActivity.class).onActivity(a -> {
            PoPApplication app = (PoPApplication) a.getApplication();
            assertThat(app.getWitnesses(), is(empty()));
            List<AddWitnessResult> results = app.addWitnesses(witnesses);
            assertThat(app.getWitnesses(), is(witnesses));
            for (AddWitnessResult result : results) {
                assertThat(result, is(ADD_WITNESS_SUCCESSFUL));
            }
        });
    }

    @Test
    public void restartAppRestoresInfoTest() {
        ActivityScenario.launch(MainActivity.class).onActivity(a -> {
            PoPApplication app = (PoPApplication) a.getApplication();
            app.setPerson(person);
            app.onTerminate();
            app.onCreate();

            assertThat(app.getPerson(), is(person));
        });
    }

    @Test
    public void cannotAddTwiceSameWitnessToLAO() {
        ActivityScenario.launch(MainActivity.class).onActivity(a -> {
            PoPApplication app = (PoPApplication) a.getApplication();
            assertNull(app.getWitnesses(lao1));
            AddWitnessResult result1 = app.addWitness(lao1, witness1);
            AddWitnessResult result2 = app.addWitness(lao1, witness1);

            assertThat(app.getWitnesses(lao1), is(Collections.singletonList(witness1)));
            assertThat(result1, is(ADD_WITNESS_SUCCESSFUL));
            assertThat(result2, is(ADD_WITNESS_ALREADY_EXISTS));
        });
    }

    @Test
    public void startAppCreatesInfoTest() {
        ActivityScenario.launch(MainActivity .class).onActivity(a -> {
            PoPApplication app = (PoPApplication) a.getApplication();
            assertThat(app.getLaos(), is(new ArrayList<>(app.getLaoEventsMap().keySet())));
            assertThat(app.getPerson().getName(), is(PoPApplication.USERNAME));
            assertNotNull(app.getPerson());
        });
    }

    @Test
    public void cannotAddTwiceSameWitnessToCurrentLAO() {
        ActivityScenario.launch(MainActivity.class).onActivity(a -> {
            PoPApplication app = (PoPApplication) a.getApplication();
            assertThat(app.getWitnesses(), is(empty()));
            AddWitnessResult result1 = app.addWitness(witness1);
            AddWitnessResult result2 = app.addWitness(witness1);

            assertThat(app.getWitnesses(), is(Collections.singletonList(witness1)));
            assertThat(result1, is(ADD_WITNESS_SUCCESSFUL));
            assertThat(result2, is(ADD_WITNESS_ALREADY_EXISTS));
        });
    }

    @Test
    public void cannotAddTwiceSameWitnessesToLAO() {
        ActivityScenario.launch(MainActivity.class).onActivity(a -> {
            PoPApplication app = (PoPApplication) a.getApplication();
            assertNull(app.getWitnesses(lao1));
            List<AddWitnessResult> results1 = app.addWitnesses(lao1, witnesses);
            List<AddWitnessResult> results2 = app.addWitnesses(lao1, witnesses);

            assertThat(app.getWitnesses(lao1), is(witnesses));
            for (AddWitnessResult result : results1) {
                assertThat(result, is(ADD_WITNESS_SUCCESSFUL));
            }
            for (AddWitnessResult result : results2) {
                assertThat(result, is(ADD_WITNESS_ALREADY_EXISTS));
            }
        });
    }

    @Test
    public void cannotAddTwiceSameWitnessesToCurrentLAO(){
        ActivityScenario.launch(MainActivity.class).onActivity(a -> {
            PoPApplication app = (PoPApplication) a.getApplication();
            assertThat(app.getWitnesses(), is(empty()));
            List<AddWitnessResult> results1 = app.addWitnesses(witnesses);
            List<AddWitnessResult> results2 = app.addWitnesses(witnesses);

            assertThat(app.getWitnesses(), is(witnesses));
            for (AddWitnessResult result : results1) {
                assertThat(result, is(ADD_WITNESS_SUCCESSFUL));
            }
            for (AddWitnessResult result : results2) {
                assertThat(result, is(ADD_WITNESS_ALREADY_EXISTS));
            }
        });
    }
}