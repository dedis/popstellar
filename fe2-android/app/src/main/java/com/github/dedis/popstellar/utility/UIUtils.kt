package com.github.dedis.popstellar.utility

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.databinding.InputFieldItemBinding
import timber.log.Timber

/**
 * This object serves as a container for utility functions that help generate/manipulate UI
 * elements.
 */
object UIUtils {
  private val TAG = UIUtils::class.java.simpleName

  /** This function hides the keyboard when called. */
  fun hideKeyboard(context: Context?, binding: View) {
    val inputMethodManager =
        context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
    inputMethodManager?.hideSoftInputFromWindow(binding.windowToken, 0)
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
   * Data class modeling the configuration of an input field
   *
   * @param hintResId the resource id of the hint to display
   * @param needsPasteButton whether the input field needs a paste button
   */
  data class InputFieldConfig(@StringRes val hintResId: Int, val needsPasteButton: Boolean)

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
              override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Not needed
              }

              override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Not needed
              }

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
