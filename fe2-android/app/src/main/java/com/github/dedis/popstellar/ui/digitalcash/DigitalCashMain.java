package com.github.dedis.popstellar.ui.digitalcash;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.github.dedis.popstellar.R;

import dagger.hilt.android.AndroidEntryPoint;

/** Activity for the digital cash */
@AndroidEntryPoint
public class DigitalCashMain extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.digital_cash_main_activity);
  }
}
