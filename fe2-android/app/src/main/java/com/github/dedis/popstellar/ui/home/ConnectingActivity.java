package com.github.dedis.popstellar.ui.home;

import android.content.Context;
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

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

@AndroidEntryPoint
public class ConnectingActivity extends AppCompatActivity {
  public static final String TAG = ConnectingActivity.class.getSimpleName();

  private final CompositeDisposable disposables = new CompositeDisposable();

  @Inject GlobalNetworkManager networkManager;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.connecting_activity);
    setupCancelButton();
    handleOpenConnection();
  }

  /**
   * Waits in background for the connection to the new server address to be opened And then starts
   * call for the subscribe and catchup process
   */
  private void handleOpenConnection() {
    String toOpen = (String) getIntent().getExtras().get(Constants.ACTIVITY_TO_OPEN_EXTRA);
    String laoId = (String) getIntent().getExtras().get(Constants.LAO_ID_EXTRA);
    boolean isDestinationHome = toOpen.equals(Constants.HOME_EXTRA);

    // Sets the lao id displayed to users
    TextView connectingText = findViewById(R.id.connecting_lao);
    connectingText.setText(isDestinationHome ? getString(R.string.stored_channels) : laoId);

    // This object will allow us to get all the event of the underlying observable, even if they
    // were emitted prior to the subscribe
    ConnectableObservable<WebSocket.Event> replay =
        networkManager.getMessageSender().getConnectEvents().replay();
    disposables.add(
        replay.subscribe(
            v -> {
              Log.d(TAG, "connect message is " + v);
              if (v instanceof WebSocket.Event.OnConnectionOpened) {
                Log.d(TAG, "connection opened with new server address");
                handleConnecting(laoId, isDestinationHome);
              } else if (v instanceof WebSocket.Event.OnConnectionFailed) {
                startActivity(HomeActivity.newIntent(this));
                disposables.dispose();
                finish();
              }
            },
            e -> Log.e(TAG, "error on subscription to connection events")));

    // Now that we observe the events we let the observable know it can begin to emit events
    replay.connect();
  }

  /**
   * Handles the subscription and catchup process to an LAO
   *
   * @param channelId the id of the lao
   */
  private void handleConnecting(String channelId, boolean isDestinationHome) {
    if (isDestinationHome) {
      Log.d(TAG, "Opening Home activity");
      startActivity(HomeActivity.newIntent(this));
      return;
    }
    Lao lao = new Lao(channelId);

    Log.d(TAG, "connecting to lao " + channelId);
    disposables.add(
        networkManager
            .getMessageSender()
            .subscribe(lao.getChannel())
            .subscribe(
                () -> {
                  Log.d(TAG, "subscribing to LAO with id " + lao.getId());
                  startActivity(LaoDetailActivity.newIntentForLao(this, lao.getId()));
                },
                error -> {
                  // In case of error, log it and go to home activity
                  ErrorUtils.logAndShow(getApplication(), TAG, error, R.string.error_subscribe_lao);
                  startActivity(HomeActivity.newIntent(this));
                }));
  }

  private void setupCancelButton() {
    Button cancelButton = findViewById(R.id.button_cancel_connecting);
    cancelButton.setOnClickListener(v -> startActivity(HomeActivity.newIntent(this)));
  }

  public static Intent newIntentForDetail(Context ctx, String laoId) {
    Intent intent = new Intent(ctx, ConnectingActivity.class);
    intent.putExtra(Constants.LAO_ID_EXTRA, laoId);
    intent.putExtra(Constants.ACTIVITY_TO_OPEN_EXTRA, Constants.LAO_DETAIL_EXTRA);
    intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
    return intent;
  }

  public static Intent newIntentForHome(Context ctx) {
    Intent intent = new Intent(ctx, ConnectingActivity.class);
    intent.putExtra(Constants.LAO_ID_EXTRA, ""); // not needed but allows more elegant handling
    intent.putExtra(Constants.ACTIVITY_TO_OPEN_EXTRA, Constants.HOME_EXTRA);
    intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
    return intent;
  }
}
