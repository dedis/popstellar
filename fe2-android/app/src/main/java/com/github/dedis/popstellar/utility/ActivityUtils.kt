package com.github.dedis.popstellar.utility

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import com.github.dedis.popstellar.model.objects.Wallet
import com.github.dedis.popstellar.model.objects.security.Base64URLData
import com.github.dedis.popstellar.repository.database.subscriptions.SubscriptionsDao
import com.github.dedis.popstellar.repository.database.subscriptions.SubscriptionsEntity
import com.github.dedis.popstellar.repository.database.wallet.WalletDao
import com.github.dedis.popstellar.repository.database.wallet.WalletEntity
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager
import io.github.novacrypto.bip39.MnemonicGenerator
import io.github.novacrypto.bip39.wordlists.English
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.security.GeneralSecurityException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.Arrays
import java.util.Collections
import java.util.function.Consumer
import java.util.function.Supplier
import kotlin.math.abs

object ActivityUtils {
    private val TAG = ActivityUtils::class.java.simpleName

    @JvmStatic
    fun setFragmentInContainer(
        manager: FragmentManager, containerId: Int, id: Int, fragmentSupplier: Supplier<Fragment?>
    ) {
        var fragment = manager.findFragmentById(id)
        // If the fragment was not created yet, create it now
        if (fragment == null) {
            fragment = fragmentSupplier.get()
        }

        // Set the new fragment in the container
        replaceFragmentInActivity(manager, fragment!!, containerId)
    }

    @JvmStatic
    fun setFragmentInContainer(
        manager: FragmentManager, containerId: Int, fragmentSupplier: Supplier<Fragment>
    ) {
        val fragment = fragmentSupplier.get()

        // Set the new fragment in the container
        replaceFragmentInActivity(manager, fragment, containerId)
    }

    private fun replaceFragmentInActivity(
        fragmentManager: FragmentManager, fragment: Fragment, frameId: Int
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
            WalletEntity(0, Collections.unmodifiableList(listOf(*seed)))

        // Save in the database the state
        return walletDao
            .insert(walletEntity)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { Timber.tag(TAG).d("Persisted wallet seed: %s", Arrays.toString(seed)) }
            ) { err: Throwable? -> Timber.tag(TAG).e(err, "Error persisting the wallet") }
    }

    /**
     * This function performs a saving routing of the connection information of a given lao. Each lao
     * saves its own set of subscriptions, such that it's possible to restore connections also with
     * old laos.
     *
     * @param laoId identifier of the lao to persist
     * @param networkManager network manager containing the message sender with the url and
     * subscriptions of the lao
     * @param subscriptionsDao interface to query the subscriptions table
     */
    @JvmStatic
    fun saveSubscriptionsRoutine(
        laoId: String?, networkManager: GlobalNetworkManager, subscriptionsDao: SubscriptionsDao
    ): Disposable? {
        val currentServerAddress = networkManager.currentUrl ?: return null
        val subscriptions = networkManager.messageSender.subscriptions
        val subscriptionsEntity = SubscriptionsEntity(laoId!!, currentServerAddress, subscriptions)

        // Save in the db the connections
        return subscriptionsDao
            .insert(subscriptionsEntity)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { Timber.tag(TAG).d("Persisted connections for lao %s : %s", laoId, subscriptions) }
            ) { err: Throwable? ->
                Timber.tag(TAG).e(err, "Error persisting the connections for lao %s", laoId)
            }
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
        tag: String?, message: String?, callback: Runnable
    ): OnBackPressedCallback {
        return object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Timber.tag(tag!!).d("Back pressed, going to %s", message)
                callback.run()
            }
        }
    }

    /**
     * This function returns a callback to be registered to the application lifecycle.
     *
     * @param consumers map that has as key the method to override, as value the consumer to apply
     * @return the lifecycle callback
     */
    @JvmStatic
    fun buildLifecycleCallback(
        consumers: Map<Lifecycle.Event?, Consumer<Activity?>?>
    ): Application.ActivityLifecycleCallbacks {
        return object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(
                activity: Activity, savedInstanceState: Bundle?
            ) {
                val consumer = consumers[Lifecycle.Event.ON_CREATE]
                consumer?.accept(activity)
            }

            override fun onActivityStarted(activity: Activity) {
                val consumer = consumers[Lifecycle.Event.ON_START]
                consumer?.accept(activity)
            }

            override fun onActivityResumed(activity: Activity) {
                val consumer = consumers[Lifecycle.Event.ON_RESUME]
                consumer?.accept(activity)
            }

            override fun onActivityPaused(activity: Activity) {
                val consumer = consumers[Lifecycle.Event.ON_PAUSE]
                consumer?.accept(activity)
            }

            override fun onActivityStopped(activity: Activity) {
                val consumer = consumers[Lifecycle.Event.ON_STOP]
                consumer?.accept(activity)
            }

            override fun onActivitySaveInstanceState(
                activity: Activity, outState: Bundle
            ) {
                // Do nothing here
            }

            override fun onActivityDestroyed(activity: Activity) {
                val consumer = consumers[Lifecycle.Event.ON_DESTROY]
                consumer?.accept(activity)
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

    /** Callback function for the card listener to expand and shrink a text box  */
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

    /**
     * This function converts a base64 string into some mnemonic words.
     *
     *
     * Disclaimer: there's no guarantee that different base64 inputs map to 2 different words. The
     * reason is that the representation space is limited. However, since the amount of messages is
     * low is practically improbable to have conflicts
     *
     * @param input base64 string
     * @param numberOfWords number of mnemonic words we want to generate
     * @return two mnemonic words
     */
    @JvmStatic
    fun generateMnemonicWordFromBase64(input: String?, numberOfWords: Int): String {
        return generateMnemonicFromBase64(Base64URLData(input).data, numberOfWords)
    }

    private fun generateMnemonicFromBase64(data: ByteArray, numberOfWords: Int): String {
        // Generate the mnemonic words from the input data
        val mnemonicWords = generateMnemonic(data)
        if (mnemonicWords.isEmpty()) {
            return "none"
        }
        val stringBuilder = StringBuilder()
        for (i in 0 until numberOfWords) {
            val wordIndex = abs(data.contentHashCode() + i) % mnemonicWords.size
            stringBuilder.append(" ").append(mnemonicWords[wordIndex])
        }
        return stringBuilder.substring(1, stringBuilder.length)
    }

    private fun generateMnemonic(data: ByteArray): Array<String?> {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val sb = StringBuilder()
            MnemonicGenerator(English.INSTANCE).createMnemonic(digest.digest(data)) { s: CharSequence? ->
                sb.append(
                    s
                )
            }
            sb.toString().split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        } catch (e: NoSuchAlgorithmException) {
            Timber.tag(TAG)
                .e(
                    e,
                    "Error generating the mnemonic for the base64 string %s",
                    Base64URLData(data).encoded
                )
            arrayOfNulls(0)
        }
    }
}