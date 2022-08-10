package com.github.dedis.popstellar.ui.settings;

import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager;
import com.github.dedis.popstellar.ui.home.HomeActivity;
import com.github.dedis.popstellar.utility.ActivityUtils;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SettingsActivity extends AppCompatActivity {

  public final String TAG = SettingsActivity.class.getSimpleName();

  @Inject GlobalNetworkManager networkManager;

  private Button applyButton;
  private String initialUrl;
  private EditText entryBoxServerUrl;

  private Button clearButton;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.settings_activity);

    initialUrl = networkManager.getCurrentUrl();

    setupClearButton();
    setupApplyButton();
    setupEntryBox();
  }

  private void setupEntryBox() {
    entryBoxServerUrl = findViewById(R.id.entry_box_server_url);
    entryBoxServerUrl.setText(initialUrl);
    entryBoxServerUrl.addTextChangedListener(
        new TextWatcher() {
          @Override
          public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            // Do nothing
          }

          @Override
          public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            // Do nothing
          }

          @Override
          public void afterTextChanged(Editable editable) {
            String newAddress = editable.toString();
            applyButton.setEnabled(newAddress.length() > 0 && !newAddress.equals(initialUrl));
          }
        });
  }

  private void setupApplyButton() {
    applyButton = findViewById(R.id.button_apply);
    applyButton.setOnClickListener(v -> applyChanges());
  }

  private void applyChanges() {
    networkManager.connect(entryBoxServerUrl.getText().toString());
    Log.d(TAG, "Trying to open home");
    startActivity(HomeActivity.newIntent(this));
  }

  private void setupClearButton() {
    clearButton = findViewById(R.id.settings_clear_button);
    clearButton.setOnClickListener(clearListener);
  }

  private final View.OnClickListener clearListener =
      v ->
          new AlertDialog.Builder(this)
              .setTitle(R.string.confirm_title)
              .setMessage(R.string.clear_confirmation_text)
              .setPositiveButton(
                  R.string.yes,
                  (dialogInterface, i) -> {
                    boolean success = ActivityUtils.clearStorage(this);
                    Toast.makeText(
                            this,
                            success ? R.string.clear_success : R.string.clear_failure,
                            Toast.LENGTH_LONG)
                        .show();

                    restartAndroidApp();
                  })
              .setNegativeButton(R.string.no, null)
              .show();

  private void restartAndroidApp() {
    Intent homeIntent = HomeActivity.newIntent(this);
    int mPendingIntentId = 123456;
    PendingIntent mPendingIntent =
        PendingIntent.getActivity(this, mPendingIntentId, homeIntent, PendingIntent.FLAG_IMMUTABLE);
    AlarmManager mgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
    mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
    System.exit(0);
  }
}
