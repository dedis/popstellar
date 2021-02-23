package com.github.dedis.student20_pop;

import static com.github.dedis.student20_pop.PoPApplication.AddWitnessResult.ADD_WITNESS_ALREADY_EXISTS;
import static com.github.dedis.student20_pop.PoPApplication.AddWitnessResult.ADD_WITNESS_SUCCESSFUL;
import static com.github.dedis.student20_pop.model.event.EventType.DISCUSSION;
import static com.github.dedis.student20_pop.model.event.EventType.MEETING;
import static com.github.dedis.student20_pop.model.event.EventType.POLL;

import android.app.Application;
import android.content.Context;
import com.github.dedis.student20_pop.model.Keys;
import com.github.dedis.student20_pop.model.Lao;
import com.github.dedis.student20_pop.model.Person;
import com.github.dedis.student20_pop.model.event.Event;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Class modelling the application : a unique person associated with LAOs */
@Deprecated
public class PoPApplication extends Application {

  public static final String TAG = PoPApplication.class.getSimpleName();
  public static final String SP_PERSON_ID_KEY = "SHARED_PREFERENCES_PERSON_ID";
  public static final String USERNAME = "USERNAME";
  public static final URI LOCAL_BACKEND_URI = URI.create("ws://10.0.2.2:8000");

  private final Map<String, Lao> laos = new HashMap<>();

  private static Context appContext;

  private Person person;

  // represents the Lao which we are connected to, can be null
  private Lao currentLao;

  @Override
  public void onCreate() {
    super.onCreate();

    person = new Person("test");

    dummyLaos();
  }

  /** Returns Person corresponding to the user. */
  public Person getPerson() {
    return person;
  }

  /** Returns the current LAO as an Optional */
  public Optional<Lao> getCurrentLao() {
    return Optional.ofNullable(currentLao);
  }

  /**
   * Returns the current LAO. Must only be called if it would not make sense that there is no
   * current Lao.
   *
   * @throws IllegalStateException if there is no current LAO
   */
  public Lao getCurrentLaoUnsafe() {
    if (currentLao == null) throw new IllegalStateException();
    return currentLao;
  }

  /** @return list of LAOs corresponding to the user */
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

  /** @param event to be added to the current lao */
  public void addEvent(Event event) {
    getCurrentLao()
        .ifPresent(
            lao -> {
              lao.addEvent(event);
              // TODO Call backend
            });
  }

  /**
   * Add a witness to the current LAO
   *
   * @param witness add witness to current lao
   * @return ADD_WITNESS_SUCCESSFUL if witness has been added ADD_WITNESS_ALREADY_EXISTS if witness
   *     already exists
   */
  public AddWitnessResult addWitness(String witness) {
    return addWitness(currentLao, witness);
  }

  /**
   * Add a witness to a specified LAO
   *
   * @param lao of the new witness
   * @param witness id to add on the list of witnesses for the LAO
   * @return ADD_WITNESS_SUCCESSFUL if witness has been added ADD_WITNESS_ALREADY_EXISTS if witness
   *     already exists
   */
  public AddWitnessResult addWitness(Lao lao, String witness) {
    // TODO when connected to backend
    // send info to backend
    // If witness has been added return true, otherwise false

    if (lao.addWitness(witness)) {
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

  /** Only useful when testing without a back-end. */
  public void activateTestingValues() {
    currentLao = new Lao("LAO I just joined", person.getId(), LOCAL_BACKEND_URI);
    dummyLaos();
  }

  /** This method creates a map for testing, when no backend is connected. */
  private void dummyLaos() {
    String notMyPublicKey = new Keys().getPublicKey();
    Lao lao0 = new Lao("LAO I just joined", getPerson().getId(), LOCAL_BACKEND_URI);
    Lao lao1 = new Lao("LAO 1", notMyPublicKey, LOCAL_BACKEND_URI);
    Lao lao2 = new Lao("LAO 2", notMyPublicKey, LOCAL_BACKEND_URI);
    Lao lao3 = new Lao("My LAO 3", getPerson().getId(), LOCAL_BACKEND_URI);
    Lao lao4 = new Lao("LAO 4", notMyPublicKey, LOCAL_BACKEND_URI);

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
        new Event(
            "Present Event 1", laoId, Instant.now().getEpochSecond(), "Somewhere", DISCUSSION),
        new Event("Past Event 1", laoId, 1481643086L, "Here", MEETING));
  }

  /** Type of results when adding a witness */
  public enum AddWitnessResult {
    ADD_WITNESS_SUCCESSFUL,
    ADD_WITNESS_ALREADY_EXISTS
  }
}
