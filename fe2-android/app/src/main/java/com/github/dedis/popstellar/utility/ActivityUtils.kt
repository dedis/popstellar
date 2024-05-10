package com.github.dedis.popstellar.utility

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.github.dedis.popstellar.model.objects.Wallet
import com.github.dedis.popstellar.repository.database.subscriptions.SubscriptionsDao
import com.github.dedis.popstellar.repository.database.subscriptions.SubscriptionsEntity
import com.github.dedis.popstellar.repository.database.wallet.WalletDao
import com.github.dedis.popstellar.repository.database.wallet.WalletEntity
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.security.GeneralSecurityException
import java.util.Arrays
import java.util.Collections
import java.util.function.Supplier
import timber.log.Timber

/** This object serves as a container for utility functions used in Activity or Fragment classes */
object ActivityUtils {
  private val TAG = ActivityUtils::class.java.simpleName

  @JvmStatic
  fun setFragmentInContainer(
      manager: FragmentManager,
      containerId: Int,
      id: Int,
      fragmentSupplier: Supplier<Fragment>
  ) {
    var fragment = manager.findFragmentById(id)
    // If the fragment was not created yet, create it now
    if (fragment == null) {
      fragment = fragmentSupplier.get()
    }

    // Set the new fragment in the container
    replaceFragmentInActivity(manager, fragment, containerId)
  }

  @JvmStatic
  fun setFragmentInContainer(
      manager: FragmentManager,
      containerId: Int,
      fragmentSupplier: Supplier<Fragment>
  ) {
    val fragment = fragmentSupplier.get()

    // Set the new fragment in the container
    replaceFragmentInActivity(manager, fragment, containerId)
  }

  private fun replaceFragmentInActivity(
      fragmentManager: FragmentManager,
      fragment: Fragment,
      frameId: Int
  ) {
    val transaction = fragmentManager.beginTransaction()

    transaction.replace(frameId, fragment)
    transaction.commit()
  }

  /**
   * This performs the persistent storage of the wallet.
   *
   * @param wallet the singleton wallet used to store PoP tokens
   * @param walletDao interface to query the database
   */
  @JvmStatic
  @Throws(GeneralSecurityException::class)
  fun saveWalletRoutine(wallet: Wallet, walletDao: WalletDao): Disposable {
    val seed = wallet.exportSeed()
    val walletEntity =
        // Constant id as we need to store only 1 entry (next insert must replace)
        WalletEntity(0, Collections.unmodifiableList(seed.toList()))

    // Save in the database the state
    return walletDao
        .insert(walletEntity)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            { Timber.tag(TAG).d("Persisted wallet seed: %s", Arrays.toString(seed)) },
            { err: Throwable -> Timber.tag(TAG).e(err, "Error persisting the wallet") })
  }

  /**
   * This function performs a saving routing of the connection information of a given lao. Each lao
   * saves its own set of subscriptions, such that it's possible to restore connections also with
   * old laos.
   *
   * @param laoId identifier of the lao to persist
   * @param networkManager network manager containing the message sender with the url and
   *   subscriptions of the lao
   * @param subscriptionsDao interface to query the subscriptions table
   */
  @JvmStatic
  fun saveSubscriptionsRoutine(
      laoId: String,
      networkManager: GlobalNetworkManager,
      subscriptionsDao: SubscriptionsDao
  ): Disposable? {
    val currentServerAddress = networkManager.currentUrl ?: return null
    val subscriptions = networkManager.messageSender.subscriptions
    val subscriptionsEntity = SubscriptionsEntity(laoId, currentServerAddress, subscriptions)

    // Save in the db the connections
    return subscriptionsDao
        .insert(subscriptionsEntity)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            { Timber.tag(TAG).d("Persisted connections for lao %s : %s", laoId, subscriptions) },
            { err: Throwable ->
              Timber.tag(TAG).e(err, "Error persisting the connections for lao %s", laoId)
            })
  }

    /**
     * This function hides the keyboard when called.
     */
    fun hideKeyboard(context : Context?, binding : View) {
        val inputMethodManager = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        inputMethodManager?.hideSoftInputFromWindow(binding.windowToken, 0)
    }

  /**
   * The following function creates an object of type OnBackPressedCallback given a specific
   * callback function. This avoids code repetitions.
   *
   * @param tag String tag for the log
   * @param message String message for the log
   * @param callback Runnable function to use * as callback
   * @return the callback object
   */
  @JvmStatic
  fun buildBackButtonCallback(
      tag: String,
      message: String,
      callback: Runnable
  ): OnBackPressedCallback {
    return object : OnBackPressedCallback(true) {

      override fun handleOnBackPressed() {
        Timber.tag(tag).d("Back pressed, going to %s", message)
        callback.run()
      }
    }
  }

  /**
   * Gets the color of the QR code based on the night mode configuration of the current context.
   *
   * @return the color of the QR code (either Color.WHITE or Color.BLACK)
   */
  @JvmStatic
  fun getQRCodeColor(context: Context): Int {
    val configuration = context.resources.configuration
    val nightModeFlags = configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK

    return if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
      Color.WHITE
    } else Color.BLACK
  }

  /** Callback function for the card listener to expand and shrink a text box */
  @JvmStatic
  fun handleExpandArrow(arrow: ImageView, text: TextView) {
    val newRotation: Float
    val visibility: Int

    // If the arrow is pointing up, then rotate down and make visible the text
    if (arrow.rotation == Constants.ORIENTATION_UP) {
      newRotation = Constants.ORIENTATION_DOWN
      visibility = View.VISIBLE
    } else { // Otherwise rotate up and hide the text
      newRotation = Constants.ORIENTATION_UP
      visibility = View.GONE
    }

    // Use an animation to rotate smoothly
    arrow.animate().rotation(newRotation).setDuration(300).start()
    text.visibility = visibility
  }
}
