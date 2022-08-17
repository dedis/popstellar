package com.github.dedis.popstellar.ui.settings;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager;
import com.github.dedis.popstellar.ui.home.HomeActivity;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SettingsActivity extends AppCompatActivity {

  public final String TAG = SettingsActivity.class.getSimpleName();

  @Inject GlobalNetworkManager networkManager;

  private Button applyButton;
  private String initialUrl;
  private EditText entryBoxServerUrl;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.settings_activity);

    initialUrl = networkManager.getCurrentUrl();

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
}
