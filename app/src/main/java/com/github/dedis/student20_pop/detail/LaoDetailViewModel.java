package com.github.dedis.student20_pop.detail;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.github.dedis.student20_pop.Event;
import com.github.dedis.student20_pop.model.Lao;
import com.github.dedis.student20_pop.model.data.LAORepository;
import com.github.dedis.student20_pop.model.entities.LAO;
import com.github.dedis.student20_pop.model.entities.LAOEntity;

public class LaoDetailViewModel extends AndroidViewModel {

    public static final String TAG = LaoDetailViewModel.class.getSimpleName();

    private final MutableLiveData<Event<Boolean>> mOpenHomeEvent = new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> mOpenIdentityEvent = new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> mOpenPropertiesEvent = new MutableLiveData<>();
    private final MutableLiveData<LAOEntity> mCurrentLao = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isOrganizer = new MutableLiveData<>();
    private final LAORepository mLAORepository;

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

    public LiveData<Event<Boolean>> getOpenPropertiesEvent() {
        return mOpenPropertiesEvent;
    }

    public LAOEntity getCurrentLao() {
        return mCurrentLao.getValue();
    }

    public Boolean isOrganizer() {
        return isOrganizer.getValue();
    }

    public void openHome() {
        mOpenHomeEvent.setValue(new Event<>(true));
    }

    public void openIdentity() {
        mOpenIdentityEvent.setValue(new Event<>(true));
    }

    public void openProperties() {
        mOpenPropertiesEvent.setValue(new Event<>(true));
    }

    public void setCurrentLao(String laoId) {
        if(laoId == null) {
            throw new IllegalArgumentException("Can't access details from a null LAO");
        }
        //TODO: get user id
        isOrganizer.setValue(laoId.equals("user id"));
        mCurrentLao.setValue(mLAORepository.getLAO(laoId));
    }
}
