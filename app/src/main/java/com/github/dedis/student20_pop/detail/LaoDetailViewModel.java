package com.github.dedis.student20_pop.detail;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.github.dedis.student20_pop.Event;
import com.github.dedis.student20_pop.model.data.LAORepository;
import com.github.dedis.student20_pop.model.entities.LAO;
import com.github.dedis.student20_pop.model.entities.LAOEntity;
import com.github.dedis.student20_pop.model.entities.Person;
import com.github.dedis.student20_pop.utility.ui.listener.OnAddWitnessListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LaoDetailViewModel extends AndroidViewModel {

    public static final String TAG = LaoDetailViewModel.class.getSimpleName();

    private final MutableLiveData<Event<Boolean>> mOpenHomeEvent = new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> mOpenIdentityEvent = new MutableLiveData<>();
    private final MutableLiveData<LAOEntity> mCurrentLao = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isOrganizer = new MutableLiveData<>();
    private final MutableLiveData<Boolean> showProperties = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> editProperties = new MutableLiveData<>(false);
    private final MutableLiveData<String> mLaoName = new MutableLiveData<>("");
    private final LAORepository mLAORepository;
    private final MutableLiveData<Map<String, Person>> mWitnessesById = new MutableLiveData<>();
    private final LiveData<List<Person>> mWitnesses = Transformations.map(mWitnessesById, witnesses ->
            new ArrayList<>(witnesses.values()));

    public LaoDetailViewModel(@NonNull Application application, LAORepository laoRepository) {
        super(application);

        mLAORepository = laoRepository;
    }

    public LiveData<Event<Boolean>> getOpenHomeEvent() {
        return mOpenHomeEvent;
    }

    public LiveData<Event<Boolean>> getOpenIdentityEvent() {
        return mOpenIdentityEvent;
    }

    public LAOEntity getCurrentLao() {
        return mCurrentLao.getValue();
    }

    public String getCurrentLaoName() {
        return getCurrentLao().lao.name;
    }

    public Boolean isOrganizer() {
        return isOrganizer.getValue();
    }

    public Boolean showProperties() {
        return showProperties.getValue();
    }

    public Boolean editProperties() {
        return editProperties.getValue();
    }

    public LiveData<List<Person>> getWitnesses() {
        return mWitnesses;
    }

    public void openHome() {
        mOpenHomeEvent.setValue(new Event<>(true));
    }

    public void openIdentity() {
        mOpenIdentityEvent.setValue(new Event<>(true));
    }

    public void openProperties() {
        showProperties.setValue(!showProperties());
    }

    public void openEditProperties() {
        editProperties.setValue(true);
    }

    public void closeEditProperties() {
        editProperties.setValue(false);
    }

    public void confirmEdit() {
        closeEditProperties();

        if(mLaoName.getValue().isEmpty()) {
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

    public void setCurrentLao(String laoId) {
        if(laoId == null) {
            throw new IllegalArgumentException("Can't access details from a null LAO");
        }
        //TODO: get user id
        isOrganizer.setValue(laoId.equals("user id"));
        mCurrentLao.setValue(mLAORepository.getLAO(laoId));

        Log.d(TAG, "The user is organizer: " + isOrganizer());

        //TODO: create insert for LAOs into db
        mCurrentLao.setValue(new LAOEntity());
    }

    public void setCurrentLaoName(String laoName) {
        if(laoName != null && !laoName.isEmpty() && !laoName.equals(getCurrentLaoName())) {
            Log.d(TAG, "New name for current LAO: " + laoName);
            mLaoName.setValue(laoName);
        }
    }
}
