package com.github.dedis.student20_pop;

import androidx.test.core.app.ActivityScenario;

import com.github.dedis.student20_pop.model.Lao;
import com.github.dedis.student20_pop.model.Person;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static androidx.test.espresso.matcher.ViewMatchers.assertThat;
import static com.github.dedis.student20_pop.PoPApplication.ADD_WITNESS_ALREADY_EXISTS;
import static com.github.dedis.student20_pop.PoPApplication.ADD_WITNESS_SUCCESSFUL;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;

public class PoPApplicationTest {

    private final Person person = new Person(PoPApplication.USERNAME);

    private final Lao lao1 = new Lao("LAO1", new Date(), person.getId());
    private final Lao lao2 = new Lao("LAO2", new Date(), person.getId());
    private final ArrayList<Lao> laos = new ArrayList<>(Arrays.asList(lao1, lao2));

    private final String witness1 = "Alphonse";
    private final String witness2 = "Bertrand";
    private final ArrayList<String> witnesses = new ArrayList<>(Arrays.asList(witness1, witness2));


    @Test
    public void canAddOneWitnessToLAO() {
        ActivityScenario.launch(MainActivity.class).onActivity(a -> {
            PoPApplication app = (PoPApplication) a.getApplication();
            assertNull(app.getWitnesses(lao1));
            int result = app.addWitness(lao1, witness1);
            assertThat(app.getWitnesses(lao1), is(Collections.singletonList(witness1)));
            assertThat(result, is(ADD_WITNESS_SUCCESSFUL));
        });
    }

    @Test
    public void canAddOneWitnessToCurrentLAO() {
        ActivityScenario.launch(MainActivity.class).onActivity(a -> {
            PoPApplication app = (PoPApplication) a.getApplication();
            assertThat(app.getWitnesses(), is(empty()));
            int result = app.addWitness(witness1);
            assertThat(app.getWitnesses(), is(Collections.singletonList(witness1)));
            assertThat(result, is(ADD_WITNESS_SUCCESSFUL));
        });
    }

    @Test
    public void canAddWitnessesToLAO() {
        ActivityScenario.launch(MainActivity.class).onActivity(a -> {
            PoPApplication app = (PoPApplication) a.getApplication();
            assertNull(app.getWitnesses(lao1));
            List<Integer> results = app.addWitnesses(lao1, witnesses);
            assertThat(app.getWitnesses(lao1), is(witnesses));
            for (int result : results) {
                assertThat(result, is(ADD_WITNESS_SUCCESSFUL));
            }
        });
    }

    @Test
    public void canAddWitnessesToCurrentLAO() {
        ActivityScenario.launch(MainActivity.class).onActivity(a -> {
            PoPApplication app = (PoPApplication) a.getApplication();
            assertThat(app.getWitnesses(), is(empty()));
            List<Integer> results = app.addWitnesses(witnesses);
            assertThat(app.getWitnesses(), is(witnesses));
            for (int result : results) {
                assertThat(result, is(ADD_WITNESS_SUCCESSFUL));
            }
        });
    }

    @Test
    public void cannotAddTwiceSameWitnessToLAO() {
        ActivityScenario.launch(MainActivity.class).onActivity(a -> {
            PoPApplication app = (PoPApplication) a.getApplication();
            assertNull(app.getWitnesses(lao1));
            int result1 = app.addWitness(lao1, witness1);
            int result2 = app.addWitness(lao1, witness1);

            assertThat(app.getWitnesses(lao1), is(Collections.singletonList(witness1)));
            assertThat(result1, is(ADD_WITNESS_SUCCESSFUL));
            assertThat(result2, is(ADD_WITNESS_ALREADY_EXISTS));
        });
    }

    @Test
    public void cannotAddTwiceSameWitnessToCurrentLAO() {
        ActivityScenario.launch(MainActivity.class).onActivity(a -> {
            PoPApplication app = (PoPApplication) a.getApplication();
            assertThat(app.getWitnesses(), is(empty()));
            int result1 = app.addWitness(witness1);
            int result2 = app.addWitness(witness1);

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
            List<Integer> results1 = app.addWitnesses(lao1, witnesses);
            List<Integer> results2 = app.addWitnesses(lao1, witnesses);

            assertThat(app.getWitnesses(lao1), is(witnesses));
            for (int result : results1) {
                assertThat(result, is(ADD_WITNESS_SUCCESSFUL));
            }
            for (int result : results2) {
                assertThat(result, is(ADD_WITNESS_ALREADY_EXISTS));
            }
        });
    }

    @Test
    public void cannotAddTwiceSameWitnessesToCurrentLAO(){
        ActivityScenario.launch(MainActivity.class).onActivity(a -> {
            PoPApplication app = (PoPApplication) a.getApplication();
            assertThat(app.getWitnesses(), is(empty()));
            List<Integer> results1 = app.addWitnesses(witnesses);
            List<Integer> results2 = app.addWitnesses(witnesses);

            assertThat(app.getWitnesses(), is(witnesses));
            for (int result : results1) {
                assertThat(result, is(ADD_WITNESS_SUCCESSFUL));
            }
            for (int result : results2) {
                assertThat(result, is(ADD_WITNESS_ALREADY_EXISTS));
            }
        });
    }
}