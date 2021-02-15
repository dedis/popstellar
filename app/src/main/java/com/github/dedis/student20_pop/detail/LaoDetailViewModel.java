package com.github.dedis.student20_pop.detail;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.github.dedis.student20_pop.model.Lao;

class LaoDetailViewModel extends AndroidViewModel {

    public static final String TAG = LaoDetailViewModel.class.getSimpleName();

    private Lao lao;

    public LaoDetailViewModel(@NonNull Application application, Lao lao) {
        super(application);

        this.lao = lao;
    }
}
