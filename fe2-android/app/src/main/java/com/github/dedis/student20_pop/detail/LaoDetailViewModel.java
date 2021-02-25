package com.github.dedis.student20_pop.detail;

import android.app.Application;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import com.github.dedis.student20_pop.Event;
import com.github.dedis.student20_pop.model.Lao;
import com.github.dedis.student20_pop.model.data.LAORepository;
import com.github.dedis.student20_pop.model.event.EventType;
import com.github.dedis.student20_pop.utility.security.Keys;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.integration.android.AndroidKeysetManager;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

public class LaoDetailViewModel extends AndroidViewModel {

  public static final String TAG = LaoDetailViewModel.class.getSimpleName();

  private final MutableLiveData<Event<Boolean>> mOpenHomeEvent = new MutableLiveData<>();
  private final MutableLiveData<Event<Boolean>> mOpenIdentityEvent = new MutableLiveData<>();
  private final MutableLiveData<Event<Boolean>> mShowPropertiesEvent = new MutableLiveData<>();
  private final MutableLiveData<Event<Boolean>> mEditPropertiesEvent = new MutableLiveData<>();
  private final MutableLiveData<Lao> mCurrentLao = new MutableLiveData<>();
  private final MutableLiveData<Boolean> mIsOrganizer = new MutableLiveData<>();
  private final MutableLiveData<Boolean> showProperties = new MutableLiveData<>(false);
  private final MutableLiveData<String> mLaoName = new MutableLiveData<>("");
  private final LAORepository mLAORepository;
  private final AndroidKeysetManager mKeysetManager;
  private final LiveData<List<String>> mWitnesses =
      Transformations.map(
          mCurrentLao,
          lao -> lao == null ? new ArrayList<>() : new ArrayList<>(lao.getWitnesses()));

  private final LiveData<String> mCurrentLaoName =
      Transformations.map(mCurrentLao, lao -> lao == null ? "" : lao.getName());

  private final MutableLiveData<Event<EventType>> mNewLaoEventEvent = new MutableLiveData<>();

  private Disposable disposable;

  public LaoDetailViewModel(
      @NonNull Application application,
      LAORepository laoRepository,
      AndroidKeysetManager keysetManager) {
    super(application);

    mLAORepository = laoRepository;
    mKeysetManager = keysetManager;
  }

  public LiveData<Event<Boolean>> getOpenHomeEvent() {
    return mOpenHomeEvent;
  }

  public LiveData<Event<Boolean>> getOpenIdentityEvent() {
    return mOpenIdentityEvent;
  }

  public LiveData<Event<Boolean>> getShowPropertiesEvent() {
    return mShowPropertiesEvent;
  }

  public LiveData<Event<Boolean>> getEditPropertiesEvent() {
    return mEditPropertiesEvent;
  }

  public Lao getCurrentLao() {
    return mCurrentLao.getValue();
  }

  public LiveData<String> getCurrentLaoName() {
    return mCurrentLaoName;
  }

  public LiveData<Boolean> isOrganizer() {
    return mIsOrganizer;
  }

  public Boolean getShowProperties() {
    return showProperties.getValue();
  }

  public LiveData<List<String>> getWitnesses() {
    return mWitnesses;
  }

  public void openHome() {
    mOpenHomeEvent.setValue(new Event<>(true));
  }

  public void openIdentity() {
    mOpenIdentityEvent.setValue(new Event<>(true));
  }

  public void showHideProperties() {
    mShowPropertiesEvent.setValue(new Event<>(!getShowProperties()));
    showProperties.postValue(!getShowProperties());
  }

  public void openEditProperties() {
    mEditPropertiesEvent.setValue(new Event<>(true));
  }

  public void closeEditProperties() {
    mEditPropertiesEvent.setValue(new Event<>(false));
  }

  public void confirmEdit() {
    closeEditProperties();

    if (!mLaoName.getValue().isEmpty()) {
      updateLaoName();
    }
  }

  public void updateLaoName() {
    // TODO: send update Lao message

    // TODO: modify LAO in LAORepository

    // TODO: set mCurrentLao with modified LAO
  }

  public void cancelEdit() {
    mLaoName.setValue("");
    closeEditProperties();
  }

  @Override
  protected void onCleared() {
    if (disposable != null) {
      disposable.dispose();
    }
    super.onCleared();
  }

  public void subscribeToLao(String laoId) {
    disposable =
        mLAORepository
            .getLaoObservable(laoId)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                lao -> {
                  Log.d(TAG, "got an update for lao: " + lao.getName());
                  mCurrentLao.postValue(lao);
                  try {
                    KeysetHandle publicKeysetHandle =
                        mKeysetManager.getKeysetHandle().getPublicKeysetHandle();
                    boolean isOrganizer =
                        lao.getOrganizer().equals(Keys.getEncodedKey(publicKeysetHandle));
                    Log.d(TAG, "isOrganizer: " + isOrganizer);
                    mIsOrganizer.postValue(isOrganizer);
                  } catch (GeneralSecurityException e) {
                    Log.d(TAG, "failed to get public keyset handle", e);
                  } catch (IOException e) {
                    Log.d(TAG, "failed to get public key", e);
                  }
                  mIsOrganizer.postValue(false);
                });
  }

  public void removeWitness(String witness) {
    // TODO: implement this by sending an UpdateLao
    Log.d(TAG, "trying to remove witness: " + witness);
  }

  public void addEvent(EventType eventType) {
    mNewLaoEventEvent.postValue(new Event<>(eventType));
  }

  public void setCurrentLaoName(String laoName) {
    if (laoName != null && !laoName.isEmpty() && !laoName.equals(getCurrentLaoName())) {
      Log.d(TAG, "New name for current LAO: " + laoName);
      mLaoName.setValue(laoName);
    }
  }

  public LiveData<Event<EventType>> getNewLaoEventEvent() {
    return mNewLaoEventEvent;
  }
}
