package com.github.dedis.popstellar.repository;

import android.app.Activity;
import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;

import com.github.dedis.popstellar.model.objects.WitnessMessage;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.repository.database.AppDatabase;
import com.github.dedis.popstellar.repository.database.witnessing.*;
import com.github.dedis.popstellar.utility.ActivityUtils;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
import timber.log.Timber;

import static java.util.Collections.*;

/** This class is the repository for witness messages */
@Singleton
public class WitnessingRepository {

  private static final String TAG = WitnessingRepository.class.getSimpleName();

  private final Map<String, LaoWitness> witnessByLao = new HashMap<>();

  private final WitnessingDao witnessingDao;
  private final WitnessDao witnessDao;

  private final CompositeDisposable disposables = new CompositeDisposable();

  @Inject
  public WitnessingRepository(AppDatabase appDatabase, Application application) {
    witnessingDao = appDatabase.witnessingDao();
    witnessDao = appDatabase.witnessDao();
    Map<Lifecycle.Event, Consumer<Activity>> consumerMap = new EnumMap<>(Lifecycle.Event.class);
    consumerMap.put(Lifecycle.Event.ON_STOP, activity -> disposables.clear());
    application.registerActivityLifecycleCallbacks(
        ActivityUtils.buildLifecycleCallback(consumerMap));
  }

  /**
   * This adds to the repository a witness message. This is done at creation of a particular
   * message, such that the witnesses are empty.
   *
   * @param laoId identifier of the lao where the message belongs
   * @param witnessMessage witness message containing the message id to sign
   */
  public void addWitnessMessage(String laoId, WitnessMessage witnessMessage) {
    Timber.tag(TAG).d("Adding a witness message on lao %s : %s", laoId, witnessMessage);
    // Persist the message
    WitnessingEntity witnessingEntity = new WitnessingEntity(laoId, witnessMessage);
    disposables.add(
        witnessingDao
            .insert(witnessingEntity)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                () ->
                    Timber.tag(TAG).d("Successfully persisted witness message %s", witnessMessage),
                err -> Timber.tag(TAG).e(err, "Error in persisting witness message")));
    // Retrieve Lao data and add the witness message to it
    getLaoWitness(laoId).add(witnessMessage);
  }

  /**
   * This adds a set of witnesses to a lao.
   *
   * @param laoId identifier of the lao
   * @param witnesses set of public keys of the witnesses to add
   */
  public void addWitnesses(String laoId, Set<PublicKey> witnesses) {
    Timber.tag(TAG).d("Adding witnesses to lao %s : %s", laoId, witnesses);
    // Persist the witnesses
    List<WitnessEntity> witnessEntities =
        witnesses.stream().map(pk -> new WitnessEntity(laoId, pk)).collect(Collectors.toList());
    disposables.add(
        witnessDao
            .insert(witnessEntities)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                () -> Timber.tag(TAG).d("Successfully persisted witnesses %s", witnesses),
                err -> Timber.tag(TAG).e(err, "Error in persisting witnesses")));
    // Retrieve Lao data and add the witnesses to it
    getLaoWitness(laoId).addWitnesses(witnesses);
  }

  /**
   * This function adds a witness public key to a witness message contained in the repository.
   *
   * @param laoId identifier of the lao where the message belongs
   * @param messageID id of the message signed
   * @param witness public key to add
   * @return true if it exists a witness message with that messageId, false otherwise
   */
  public boolean addWitnessToMessage(String laoId, MessageID messageID, PublicKey witness) {
    Timber.tag(TAG).d("Adding a witness to a witness message on lao %s : %s", laoId, messageID);
    // Retrieve Lao data and add the witness message to it
    return getLaoWitness(laoId).addWitnessToMessage(messageID, witness);
  }

  /**
   * It checks if a public keys belongs to the set of witnesses for a given lao.
   *
   * @param laoId lao identifier
   * @param witness public key to check
   * @return true if present, false otherwise
   */
  public boolean isWitness(String laoId, PublicKey witness) {
    return getLaoWitness(laoId).isWitness(witness);
  }

  /** Check whether the witness set is empty for a given lao. */
  public boolean areWitnessesEmpty(String laoId) {
    return getLaoWitness(laoId).isWitnessEmpty();
  }

  /**
   * Check whether the passed witnesses are matching with the lao's witnesses.
   *
   * @param laoId identifier of the lao
   * @param witnesses set of public keys representing to witnesses to check
   * @return true if equals, false otherwise
   */
  public boolean areWitnessesEquals(String laoId, Set<PublicKey> witnesses) {
    return getLaoWitness(laoId).areWitnessesEquals(witnesses);
  }

  /**
   * This returns the set of public keys of the witnesses of a lao.
   *
   * @param laoId lao identifier whose witnesses are being retrieved
   * @return set of public keys of the witnesses
   */
  public Set<PublicKey> getWitnesses(String laoId) {
    return getLaoWitness(laoId).getWitnesses();
  }

  /**
   * Returns an observable over the set of witnesses in a lao.
   *
   * @param laoId the id of the Lao whose witnesses we want to observe
   * @return an observable set of public keys which corresponds to the set of witnesses on the given
   *     lao
   */
  public Observable<Set<PublicKey>> getWitnessesObservableInLao(String laoId) {
    return getLaoWitness(laoId).getWitnessesSubject();
  }

  /**
   * Returns an observable of the list of witness messages. Each time a witness is added to a
   * witness message the observable is notified with the updated list.
   *
   * @param laoId identifier of the lao whose witness messages are observed
   * @return an observable list of witness messages (message id and witnesses having signed it)
   */
  public Observable<List<WitnessMessage>> getWitnessMessagesObservableInLao(String laoId) {
    return getLaoWitness(laoId).getWitnessMessagesSubject();
  }

  /** Get in a thread-safe fashion the witness object for the lao, computes it if absent. */
  @NonNull
  private synchronized LaoWitness getLaoWitness(String laoId) {
    // Create the lao witness object if it is not present yet
    return witnessByLao.computeIfAbsent(laoId, lao -> new LaoWitness(laoId, this));
  }

  private static class LaoWitness {

    private final String laoId;
    private final WitnessingRepository repo;
    private boolean alreadyRetrieved = false;

    private final Set<PublicKey> witnesses = new HashSet<>();
    private final Subject<Set<PublicKey>> witnessesSubject =
        BehaviorSubject.createDefault(unmodifiableSet(emptySet()));
    private final Map<MessageID, WitnessMessage> witnessMessages = new LinkedHashMap<>();
    private final Subject<List<WitnessMessage>> witnessMessagesSubject =
        BehaviorSubject.createDefault(unmodifiableList(emptyList()));

    public LaoWitness(String laoId, WitnessingRepository repo) {
      this.laoId = laoId;
      this.repo = repo;
      loadFromDisk();
    }

    public synchronized void add(WitnessMessage witnessMessage) {
      witnessMessages.put(witnessMessage.getMessageId(), witnessMessage);
      witnessMessagesSubject.onNext(unmodifiableList(new ArrayList<>(witnessMessages.values())));
    }

    public synchronized void addWitnesses(Set<PublicKey> witnesses) {
      this.witnesses.addAll(witnesses);
      witnessesSubject.onNext(unmodifiableSet(new HashSet<>(this.witnesses)));
    }

    public synchronized boolean addWitnessToMessage(MessageID messageID, PublicKey witness) {
      WitnessMessage witnessMessage = witnessMessages.get(messageID);
      if (witnessMessage == null) {
        return false;
      }
      witnessMessage.addWitness(witness);
      // Persist the new message
      repo.disposables.add(
          repo.witnessingDao
              .insert(new WitnessingEntity(laoId, witnessMessage))
              .subscribeOn(Schedulers.io())
              .observeOn(AndroidSchedulers.mainThread())
              .subscribe(
                  () ->
                      Timber.tag(TAG)
                          .d("Successfully persisted witness message %s", witnessMessage),
                  err -> Timber.tag(TAG).e(err, "Error in persisting witness message")));
      // Update the subject
      witnessMessagesSubject.onNext(unmodifiableList(new ArrayList<>(witnessMessages.values())));
      return true;
    }

    public synchronized boolean isWitness(PublicKey witness) {
      return witnesses.contains(witness);
    }

    public synchronized boolean isWitnessEmpty() {
      return witnesses.isEmpty();
    }

    public synchronized boolean areWitnessesEquals(Set<PublicKey> witnesses) {
      return this.witnesses.equals(witnesses);
    }

    public synchronized Set<PublicKey> getWitnesses() {
      return new HashSet<>(witnesses);
    }

    public Observable<Set<PublicKey>> getWitnessesSubject() {
      return witnessesSubject;
    }

    public Observable<List<WitnessMessage>> getWitnessMessagesSubject() {
      return witnessMessagesSubject;
    }

    /** This function loads the lao witnessing state from the disk at creation of the lao */
    private void loadFromDisk() {
      if (alreadyRetrieved) {
        return;
      }
      repo.disposables.addAll(
          repo.witnessDao
              .getWitnessesByLao(laoId)
              .subscribeOn(Schedulers.io())
              .observeOn(AndroidSchedulers.mainThread())
              .subscribe(
                  witnessList -> addWitnesses(new HashSet<>(witnessList)),
                  err -> Timber.tag(TAG).e(err, "No witnesses found on the disk")),
          repo.witnessingDao
              .getWitnessMessagesByLao(laoId)
              .subscribeOn(Schedulers.io())
              .observeOn(AndroidSchedulers.mainThread())
              .subscribe(
                  witnessMessageList -> witnessMessageList.forEach(this::add),
                  err -> Timber.tag(TAG).e(err, "No witness messages found on the disk")));
      alreadyRetrieved = true;
    }
  }
}
