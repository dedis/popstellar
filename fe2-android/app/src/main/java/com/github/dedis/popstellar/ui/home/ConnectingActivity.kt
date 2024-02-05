package com.github.dedis.popstellar.ui.home

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.databinding.ConnectingActivityBinding
import com.github.dedis.popstellar.model.network.method.message.data.lao.CreateLao
import com.github.dedis.popstellar.model.objects.Channel
import com.github.dedis.popstellar.model.objects.Lao
import com.github.dedis.popstellar.model.objects.security.PublicKey
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager
import com.github.dedis.popstellar.ui.lao.LaoActivity
import com.github.dedis.popstellar.utility.Constants
import com.github.dedis.popstellar.utility.error.ErrorUtils.logAndShow
import com.github.dedis.popstellar.utility.security.KeyManager
import com.tinder.scarlet.WebSocket
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observables.ConnectableObservable
import java.util.stream.Collectors
import javax.inject.Inject
import timber.log.Timber

@AndroidEntryPoint
class ConnectingActivity : AppCompatActivity() {
  private val disposables = CompositeDisposable()

  @Inject lateinit var networkManager: GlobalNetworkManager
  @Inject lateinit var keyManager: KeyManager

  private lateinit var binding: ConnectingActivityBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    binding = ConnectingActivityBinding.inflate(layoutInflater)

    setContentView(binding.root)
    setupCancelButton()
    setupConnectingText()

    handleOpenConnection()
  }

  public override fun onPause() {
    super.onPause()
    disposables.dispose()
  }

  private fun setupConnectingText() {
    when {
      intent.getStringExtra(Constants.ACTIVITY_TO_OPEN_EXTRA) == Constants.HOME_EXTRA -> {
        binding.connectingLao.setText(R.string.stored_channels)
      }
      intent.getStringExtra(Constants.CONNECTION_PURPOSE_EXTRA) == Constants.JOINING_EXTRA -> {
        binding.connectingLao.text = intent.getStringExtra(Constants.LAO_ID_EXTRA)
      }
      intent.getStringExtra(Constants.CONNECTION_PURPOSE_EXTRA) == Constants.CREATING_EXTRA -> {
        binding.connectingText.setText(R.string.creating_new_lao)
        binding.connectingLao.text = intent.getStringExtra(Constants.LAO_NAME)
      }
    }
  }

  /**
   * Waits in background for the connection to the new server address to be opened And then starts
   * call for the subscribe and catchup process
   */
  private fun handleOpenConnection() {
    val toOpen = intent.getStringExtra(Constants.ACTIVITY_TO_OPEN_EXTRA)
    val isDestinationHome = toOpen == Constants.HOME_EXTRA

    // This object will allow us to get all the event of the underlying observable, even if they
    // were emitted prior to the subscribe
    val replay: ConnectableObservable<WebSocket.Event> =
        networkManager.messageSender.connectEvents.replay()
    disposables.add(
        replay.subscribe(
            { v: WebSocket.Event ->
              Timber.tag(TAG).d("connect message is %s", v)

              if (v is WebSocket.Event.OnConnectionOpened<*>) {
                Timber.tag(TAG).d("connection opened with new server address")
                handleConnecting(isDestinationHome)
              } else if (v is WebSocket.Event.OnConnectionFailed) {
                startActivity(HomeActivity.newIntent(this))
                disposables.dispose()
                finish()
              }
            },
            { e: Throwable -> Timber.tag(TAG).e(e, "error on subscription to connection events") }))

    // Now that we observe the events we let the observable know it can begin to emit events
    replay.connect()
  }

  /** Handles the subscription and catchup process to an LAO */
  private fun handleConnecting(isDestinationHome: Boolean) {
    if (isDestinationHome) {
      // This is when we reconnect to laos, therefore returning home
      Timber.tag(TAG).d("Opening Home activity")
      startActivity(HomeActivity.newIntent(this))
      finish()
      return
    }

    // We are either joining an LAO or creating a new one
    val isCreation =
        (intent.extras?.getString(Constants.CONNECTION_PURPOSE_EXTRA) == Constants.CREATING_EXTRA)

    if (isCreation) {
      val laoName = intent.extras!!.getString(Constants.LAO_NAME)!!
      val witnessesList: List<String> = intent.getStringArrayListExtra(Constants.WITNESSES)!!
      val isWitnessingEnabled = intent.extras!!.getBoolean(Constants.WITNESSING_FLAG_EXTRA)
      val witnesses: List<PublicKey>

      if (isWitnessingEnabled) {
        witnesses =
            witnessesList
                .stream()
                .map { data: String -> PublicKey(data) }
                .collect(Collectors.toList())

        // Add the organizer to the list of witnesses
        witnesses.add(keyManager.mainPublicKey)
      } else {
        witnesses = emptyList()
      }

      val createLao = CreateLao(laoName, keyManager.mainPublicKey, witnesses)
      val lao = Lao(createLao.id)
      Timber.tag(TAG).d("Creating Lao %s", lao.id)
      disposables.add(
          networkManager.messageSender
              .publish(keyManager.mainKeyPair, Channel.ROOT, createLao)
              .subscribe {
                Timber.tag(TAG).d("got success result for create lao with id %s", lao.id)
                subscribeToLao(lao) // Subscribe to the newly created Lao
              })
    } else { // Joining an existing lao
      val laoId = intent.extras?.getString(Constants.LAO_ID_EXTRA)
      val lao = Lao(laoId)
      subscribeToLao(lao) // Subscribe to the existing lao
    }
  }

  private fun subscribeToLao(lao: Lao) {
    Timber.tag(TAG).d("connecting to lao %s", lao.channel)

    disposables.add(
        networkManager.messageSender
            .subscribe(lao.channel)
            .subscribe(
                {
                  Timber.tag(TAG).d("subscribing to LAO with id %s", lao.id)
                  startActivity(LaoActivity.newIntentForLao(this, lao.id))
                  finish()
                },
                { error: Throwable ->
                  // In case of error, log it and go to home activity
                  logAndShow(application, TAG, error, R.string.error_subscribe_lao)
                  startActivity(HomeActivity.newIntent(this))
                  finish()
                }))
  }

  private fun setupCancelButton() {
    val cancelButton = findViewById<Button>(R.id.button_cancel_connecting)
    cancelButton.setOnClickListener {
      startActivity(HomeActivity.newIntent(this))
      finish()
    }
  }

  companion object {
    val TAG: String = ConnectingActivity::class.java.simpleName

    @JvmStatic
    fun newIntentForJoiningDetail(ctx: Context, laoId: String): Intent {
      val intent = Intent(ctx, ConnectingActivity::class.java)

      intent.putExtra(Constants.LAO_ID_EXTRA, laoId)
      intent.putExtra(Constants.CONNECTION_PURPOSE_EXTRA, Constants.JOINING_EXTRA)
      intent.putExtra(Constants.ACTIVITY_TO_OPEN_EXTRA, Constants.LAO_DETAIL_EXTRA)
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

      return intent
    }

    fun newIntentForCreatingDetail(
        ctx: Context,
        laoName: String,
        witnesses: List<PublicKey>,
        isWitnessingEnabled: Boolean
    ): Intent {
      val intent = Intent(ctx, ConnectingActivity::class.java)

      intent.putExtra(Constants.LAO_NAME, laoName)
      intent.putStringArrayListExtra(
          Constants.WITNESSES,
          ArrayList(witnesses.stream().map(PublicKey::encoded).collect(Collectors.toList())))
      intent.putExtra(Constants.CONNECTION_PURPOSE_EXTRA, Constants.CREATING_EXTRA)
      intent.putExtra(Constants.ACTIVITY_TO_OPEN_EXTRA, Constants.LAO_DETAIL_EXTRA)
      intent.putExtra(Constants.WITNESSING_FLAG_EXTRA, isWitnessingEnabled)
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

      return intent
    }
  }
}
