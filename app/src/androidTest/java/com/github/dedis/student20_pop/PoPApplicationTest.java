package com.github.dedis.student20_pop;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.core.app.ActivityScenario;

import com.github.dedis.student20_pop.model.Lao;
import com.github.dedis.student20_pop.model.Person;
import com.google.gson.Gson;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import static androidx.test.espresso.matcher.ViewMatchers.assertThat;
import static com.github.dedis.student20_pop.PoPApplication.SP_LAOS_KEY;
import static com.github.dedis.student20_pop.PoPApplication.SP_PERSON_ID_KEY;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PoPApplicationTest {

    private final Person person = new Person(PoPApplication.USERNAME);
    private final Lao lao1 = new Lao("LAO1", new Date(), person.getId());
    private final Lao lao2 = new Lao("LAO2", new Date(), person.getId());
    private final ArrayList<Lao> laos = new ArrayList<>(Arrays.asList(lao1, lao2));

    @Test
    public void terminateAppSavesInfoTest() {
        ActivityScenario.launch(MainActivity .class).onActivity(a -> {
            PoPApplication app = (PoPApplication) a.getApplication();
            SharedPreferences sp = app.getSharedPreferences(PoPApplication.TAG, Context.MODE_PRIVATE);

            app.setPerson(person);
            app.setLaos(laos);
            app.onTerminate();

            for(int i = 0; i < laos.size(); i++) {
                assertTrue(sp.contains(SP_LAOS_KEY + i));
            }
            assertTrue(sp.contains(SP_PERSON_ID_KEY));
        });
    }

    @Test
    public void restartAppRestoresInfoTest() {
        ActivityScenario.launch(MainActivity .class).onActivity(a -> {
            PoPApplication app = (PoPApplication) a.getApplication();
            app.setPerson(person);
            app.setLaos(laos);
            app.onTerminate();
            app.onCreate();

            assertThat(app.getLaos(), is(laos));
            assertThat(app.getPerson(), is(person));
        });
    }

    @Test
    public void startAppCreatesInfoTest() {
        ActivityScenario.launch(MainActivity .class).onActivity(a -> {
            PoPApplication app = (PoPApplication) a.getApplication();
            assertThat(app.getLaos(), is(new ArrayList<>()));
            assertThat(app.getPerson().getName(), is(PoPApplication.USERNAME));
            assertNotNull(app.getPerson());
        });
    }
}
