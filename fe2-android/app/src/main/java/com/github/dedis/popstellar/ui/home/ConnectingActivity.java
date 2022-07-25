package com.github.dedis.popstellar.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager;
import com.github.dedis.popstellar.ui.detail.LaoDetailActivity;
import com.github.dedis.popstellar.utility.Constants;
import com.github.dedis.popstellar.utility.error.ErrorUtils;
import com.tinder.scarlet.WebSocket;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observables.ConnectableObservable;

@AndroidEntryPoint
public class ConnectingActivity extends AppCompatActivity {
  public static final String TAG = ConnectingActivity.class.getSimpleName();

  private final CompositeDisposable disposables = new CompositeDisposable();

  @Inject GlobalNetworkManager networkManager;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.connecting_activity);
    String channelId = (String) getIntent().getExtras().get(Constants.LAO_ID_EXTRA);
    setupCancelButton();
    handleOpenConnection(channelId);
  }

  private void openLaoDetailActivity(String laoId) {
    Log.d(TAG, "Trying to open lao detail for lao with id " + laoId);

    Intent intent = new Intent(this, LaoDetailActivity.class);
    intent.putExtra(Constants.LAO_ID_EXTRA, laoId);
    intent.putExtra(Constants.FRAGMENT_TO_OPEN_EXTRA, Constants.LAO_DETAIL_EXTRA);

    startActivity(intent);
  }

  private void openHomeActivity() {
    Log.d(TAG, "Opening Home activity");

    Intent intent = new Intent(this, HomeActivity.class);
    startActivity(intent);
    finish();
  }

  /**
   * Handles the subscription and catchup process to an LAO
   *
   * @param channelId the id of the lao
   */
  private void handleConnecting(String channelId) {
    Lao lao = new Lao(channelId);

    Log.d(TAG, "connecting to lao " + channelId);
    disposables.add(
        networkManager
            .getMessageSender()
            .subscribe(lao.getChannel())
            .subscribe(
                () -> {
                  Log.d(TAG, "subscribing to LAO with id " + lao.getId());
                  openLaoDetailActivity(lao.getId());
                },
                error -> {
                  // In case of error, log it and go to home activity
                  ErrorUtils.logAndShow(getApplication(), TAG, error, R.string.error_subscribe_lao);
                  openHomeActivity();
                }));
  }

  private void setupCancelButton() {
    Button cancelButton = findViewById(R.id.button_cancel_connecting);
    cancelButton.setOnClickListener(v -> openHomeActivity());
  }

  /**
   * Waits in background for the connection to the new server address to be opened And then starts
   * call for the subscribe and catchup process
   *
   * @param channelId the id of the lao
   */
  private void handleOpenConnection(String channelId) {
    // Sets the lao id displayed to users
    TextView connectingText = findViewById(R.id.connecting_lao);
    connectingText.setText(channelId);

    // This object will allow us to get all the event of the underlying observable, even if they
    // were emitted prior to the subscribe
    ConnectableObservable<WebSocket.Event> replay =
        networkManager.getMessageSender().getConnectEvents().replay();
    disposables.add(
        replay.subscribe(
            v -> {
              if (v.toString().contains("OnConnectionOpened")) {
                Log.d(TAG, "connection opened with new server address");
                handleConnecting(channelId);
              }
            }));

    // Now that we observe the events we let the observable know it can begin to emit events
    replay.connect();
  }
}
