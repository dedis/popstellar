package com.github.dedis.popstellar.utility

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.lifecycle.Lifecycle
import com.github.dedis.popstellar.model.objects.security.Base64URLData
import io.github.novacrypto.bip39.MnemonicGenerator
import io.github.novacrypto.bip39.wordlists.English
import okio.ByteString.Companion.decodeBase64
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.function.Consumer
import kotlin.math.abs
import timber.log.Timber
import kotlin.random.Random

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

  private fun generateMnemonic(data: ByteArray, defaultOnFailure: Boolean = false): Array<String> {
    return try {
      val digest = MessageDigest.getInstance("SHA-256")
      val sb = StringBuilder()

      MnemonicGenerator(English.INSTANCE).createMnemonic(digest.digest(data)) { s: CharSequence ->
        sb.append(s)
      }

      sb.toString().split(" ").dropLastWhile { it.isEmpty() }.toTypedArray()
    } catch (e: NoSuchAlgorithmException) {
      Timber.tag(TAG)
        .e(e, "Error generating mnemonic for base64 string %s", Base64URLData(data).encoded)
      if (defaultOnFailure) arrayOf("default", "username") else emptyArray()
    }
  }


  /*
  * This function generates a unique and memorable username from a base64 string.
  *
  * @param input base64 string.
  * @return a username composed of truncated mnemonic words and a numerical suffix.
  */
  @JvmStatic
  fun generateUsernameFromBase64(input: String): String {
    val data = input.decodeBase64()
    val words = data?.let { generateMnemonic(it.toByteArray(), true) }
    if (words != null) {
      if (words.size < 2) {
        return "defaultUsername${Random.nextInt(0, 9999999).toString().padStart(4, '0')}"
      }
    }

    val adjective = truncateWord(words?.get(0) ?: return "", 4)
    val noun = truncateWord(words?.get(1) ?: return "", 6)
    val number = getFirstNumberDigits(input, 4)

    return "$adjective$noun$number"
  }


  // this function filters all non digits characters and returns the first nbDigits
  fun getFirstNumberDigits(b64: String, nbDigits: Int): String {
    val digits = b64.filter { it.isDigit() }
    return digits.take(nbDigits).padStart(nbDigits, '0')
  }


  /*
  * Truncates a word to the specified number of characters.
  *
  * @param word the word to truncate.
  * @param length the number of characters to keep.
  * @return the truncated word.
  */
  private fun truncateWord(word: String, length: Int): String {
    return if (word.length > length) word.substring(0, length) else word
  }
}
