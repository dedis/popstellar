package com.github.dedis.popstellar.utility

import android.app.Activity
import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.model.objects.security.Base64URLData
import io.github.novacrypto.bip39.MnemonicGenerator
import io.github.novacrypto.bip39.wordlists.English
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.function.Consumer
import kotlin.math.abs
import timber.log.Timber

/** Object containing general purpose utility functions */
object GeneralUtils {
  private val TAG = GeneralUtils::class.java.simpleName

  /**
   * This function returns a callback to be registered to the application lifecycle.
   *
   * @param consumers map that has as key the method to override, as value the consumer to apply
   * @return the lifecycle callback
   */
  @JvmStatic
  fun buildLifecycleCallback(
      consumers: Map<Lifecycle.Event, Consumer<Activity>>
  ): Application.ActivityLifecycleCallbacks {
    return object : Application.ActivityLifecycleCallbacks {

      override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
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

      override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        // Do nothing here
      }

      override fun onActivityDestroyed(activity: Activity) {
        val consumer = consumers[Lifecycle.Event.ON_DESTROY]
        consumer?.accept(activity)
      }
    }
  }

  /**
   * This class is a utility to setup a copy button that copies the text of a TextView to the
   * clipboard.
   *
   * @param context the context of the application
   */
  class ClipboardUtil(val context : Context) {

    fun setupCopyButton(button: View, textView: TextView, label: String) {
      button.setOnClickListener {
        val text = textView.text.toString()
        copyTextToClipboard(label, text)
        Toast.makeText(context, R.string.successful_copy, Toast.LENGTH_SHORT).show()
      }
    }

    private fun copyTextToClipboard(label: String, content: String) {
      val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
      val clip = ClipData.newPlainText(label, content)
      clipboard.setPrimaryClip(clip)
    }
  }


  /**
   * This function converts a base64 string into some mnemonic words.
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
  fun generateMnemonicWordFromBase64(input: String, numberOfWords: Int): String {
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

  private fun generateMnemonic(data: ByteArray): Array<String> {
    return try {
      val digest = MessageDigest.getInstance("SHA-256")
      val sb = StringBuilder()

      MnemonicGenerator(English.INSTANCE).createMnemonic(digest.digest(data)) { s: CharSequence ->
        sb.append(s)
      }

      sb.toString().split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
    } catch (e: NoSuchAlgorithmException) {
      Timber.tag(TAG)
          .e(
              e,
              "Error generating the mnemonic for the base64 string %s",
              Base64URLData(data).encoded)
      emptyArray()
    }
  }
}
