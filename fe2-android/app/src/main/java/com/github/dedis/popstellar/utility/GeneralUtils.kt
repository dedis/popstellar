package com.github.dedis.popstellar.utility

import android.app.Activity
import android.app.Application
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.databinding.InputFieldItemBinding
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
  class ClipboardUtil(val context: Context) {

    fun setupCopyButton(button: View, textView: TextView, label: String) {
      button.setOnClickListener {
        val text = textView.text.toString()
        copyTextToClipboard(label, text)
        Toast.makeText(context, R.string.successful_copy, Toast.LENGTH_SHORT).show()
      }
    }

    fun setupPasteButton(button: View, textView: TextView) {
      button.setOnClickListener { pasteTextFromClipboard(context, textView) }
    }

    private fun copyTextToClipboard(label: String, content: String) {
      val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
      val clip = ClipData.newPlainText(label, content)
      clipboard.setPrimaryClip(clip)
    }

    private fun pasteTextFromClipboard(context: Context, textView: TextView) {
      val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
      if (clipboard.hasPrimaryClip() &&
          clipboard.primaryClipDescription?.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) ==
              true) {
        val item = clipboard.primaryClip?.getItemAt(0)
        textView.text = item?.text.toString()
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

  /**
   * This function sets up the input fields in a RecyclerView.
   *
   * @param recyclerView the RecyclerView to setup
   * @param inputFields the list of input fields to display
   * @param context the context of the application
   * @param clipboardManager the clipboard manager to use
   */
  fun setupInputFields(
      recyclerView: RecyclerView,
      inputFields: List<InputFieldConfig>,
      context: Context,
      clipboardManager: ClipboardUtil
  ) {
    recyclerView.apply {
      layoutManager = LinearLayoutManager(context)
      adapter = InputFieldsAdapter(inputFields, clipboardManager)
    }
  }

  /**
   * Data class modeling the configuration of an input field
   *
   * @param hintResId the resource id of the hint to display
   * @param needsPasteButton whether the input field needs a paste button
   */
  data class InputFieldConfig(@StringRes val hintResId: Int, val needsPasteButton: Boolean)

  /**
   * Adapter for the input fields Only for String inputs for now, but can be extended to other types
   * when needed
   *
   * @param inputFields the list of input fields to display
   * @param clipboardManager the clipboard manager to use
   */
  class InputFieldsAdapter(
      private val inputFields: List<InputFieldConfig>,
      private val clipboardManager: ClipboardUtil
  ) : RecyclerView.Adapter<InputFieldsAdapter.InputFieldViewHolder>() {

    private var inputFieldValues: MutableList<Pair<InputFieldConfig, String>> =
        inputFields.map { it to "" }.toMutableList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InputFieldViewHolder {
      val binding =
          InputFieldItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
      return InputFieldViewHolder(binding, clipboardManager, this::updateText)
    }

    override fun onBindViewHolder(holder: InputFieldViewHolder, position: Int) {
      holder.bind(inputFieldValues[position].first, inputFieldValues[position].second)
    }

    override fun getItemCount(): Int = inputFields.size

    private fun updateText(hint: Int, text: String) {
      val index = inputFieldValues.indexOfFirst { it.first.hintResId == hint }
      if (index != -1) {
        inputFieldValues[index] = inputFieldValues[index].first to text
      } else {
        Timber.tag(TAG).e("Could not find the input field with hint %s", hint)
      }
    }

    fun getCurrentInputData(): Map<Int, String> {
      return inputFieldValues.associate { it.first.hintResId to it.second }
    }

    /**
     * ViewHolder for the input fields
     *
     * @param binding the binding of the input field item
     * @param clipboardManager the clipboard manager to use
     * @param textUpdater the function to update the text of the input field
     */
    class InputFieldViewHolder(
        private val binding: InputFieldItemBinding,
        private val clipboardManager: ClipboardUtil,
        private val textUpdater: (Int, String) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

      /**
       * Binds the input field to the view
       *
       * @param inputFieldConfig the configuration of the input field
       * @param text the text to display
       */
      fun bind(inputFieldConfig: InputFieldConfig, text: String) {
        binding.textInputLayout.hint = binding.root.context.getString(inputFieldConfig.hintResId)
        binding.textInput.setText(text)

        binding.textInput.addTextChangedListener(
            object : TextWatcher {
              override fun beforeTextChanged(
                  s: CharSequence?,
                  start: Int,
                  count: Int,
                  after: Int
              ) {}

              override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

              override fun afterTextChanged(editable: Editable?) {
                if (editable != null) {
                  textUpdater(inputFieldConfig.hintResId, editable.toString())
                }
              }
            })

        if (inputFieldConfig.needsPasteButton) {
          binding.pasteButton.visibility = View.VISIBLE
          clipboardManager.setupPasteButton(binding.pasteButton, binding.textInput)
        } else {
          binding.pasteButton.visibility = View.GONE
        }
      }
    }
  }
}
