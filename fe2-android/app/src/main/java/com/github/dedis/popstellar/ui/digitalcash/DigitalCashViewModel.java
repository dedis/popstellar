package com.github.dedis.popstellar.ui.digitalcash;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class DigitalCashViewModel extends AndroidViewModel {
  public static final String TAG = DigitalCashViewModel.class.getSimpleName();

  @Inject
  public DigitalCashViewModel(@NonNull Application application) {
    super(application);
  }
}
