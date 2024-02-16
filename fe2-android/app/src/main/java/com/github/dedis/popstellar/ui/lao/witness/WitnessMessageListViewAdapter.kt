package com.github.dedis.popstellar.ui.lao.witness

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentActivity
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.databinding.WitnessMessageLayoutBinding
import com.github.dedis.popstellar.model.objects.WitnessMessage
import com.github.dedis.popstellar.model.objects.security.PublicKey
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.obtainViewModel
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.obtainWitnessingViewModel
import com.github.dedis.popstellar.ui.lao.LaoViewModel
import com.github.dedis.popstellar.utility.ActivityUtils.handleExpandArrow
import com.github.dedis.popstellar.utility.error.ErrorUtils.logAndShow
import java.util.stream.Collectors
import timber.log.Timber

/** Adapter to show the messages that have to be signed by the witnesses */
class WitnessMessageListViewAdapter(
    messages: List<WitnessMessage>?,
    private val activity: FragmentActivity
) : BaseAdapter() {
  private val laoViewModel: LaoViewModel = obtainViewModel(activity)
  private val witnessingViewModel: WitnessingViewModel =
      obtainWitnessingViewModel(activity, laoViewModel.laoId)
  private val isWitness: Boolean = java.lang.Boolean.TRUE == laoViewModel.isWitness.value

  private var messages: List<WitnessMessage>? = null

  init {
    setList(messages)
  }

  fun replaceList(messages: List<WitnessMessage>?) {
    setList(messages)
  }

  fun deleteSignedMessages() {
    witnessingViewModel.deleteSignedMessages()
  }

  private fun setList(messages: List<WitnessMessage>?) {
    this.messages = messages ?: return
    notifyDataSetChanged()
  }

  /**
   * How many items are in the data set represented by this Adapter.
   *
   * @return Count of items.
   */
  override fun getCount(): Int {
    return messages?.size ?: 0
  }

  override fun getItem(position: Int): WitnessMessage? {
    return messages?.get(position)
  }

  override fun getItemId(position: Int): Long {
    return position.toLong()
  }

  override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
    val binding: WitnessMessageLayoutBinding? =
        if (convertView == null) {
          // inflate
          val inflater = LayoutInflater.from(parent.context)
          WitnessMessageLayoutBinding.inflate(inflater, parent, false)
        } else {
          DataBindingUtil.getBinding(convertView)
        }

    checkNotNull(binding) { "Binding could not be found in the view" }

    binding.lifecycleOwner = activity

    val witnessMessage = messages?.get(position) ?: return binding.root

    // Set message title and description
    binding.messageTitle.text = witnessMessage.title
    binding.messageDescriptionText.text = witnessMessage.description

    // Set witness signatures text
    val formattedSignatures = formatPublicKeys(witnessMessage.witnesses)
    binding.witnessesText.text = formattedSignatures

    // Set message description dropdown
    binding.messageDescriptionCard.setOnClickListener {
      handleExpandArrow(binding.messageDescriptionArrow, binding.messageDescriptionText)
    }

    // Set signatures dropdown
    binding.signaturesCard.setOnClickListener {
      handleExpandArrow(binding.signaturesArrow, binding.witnessesText)
    }

    if (isWitness) {
      val context = parent.context
      val witnessSignatures: Set<PublicKey> = witnessMessage.witnesses
      if (witnessSignatures.contains(laoViewModel.getPublicKey())) {
        // The user already signed the message
        binding.signMessageButton.isEnabled = false
        val signed = activity.getString(R.string.signed)
        binding.signMessageButton.text = signed
      } else {
        binding.signMessageButton.isEnabled = true
        val sign = activity.getString(R.string.sign)
        binding.signMessageButton.text = sign
        val listener =
            setUpSignButtonClickListener(context, witnessMessage, binding.signMessageButton)
        binding.signMessageButton.setOnClickListener(listener)
      }
    } else {
      // Don't show the sign button if the user is not a witness
      binding.signMessageButton.visibility = View.GONE
    }

    return binding.root
  }

  private fun setUpSignButtonClickListener(
      context: Context,
      message: WitnessMessage,
      button: Button
  ): View.OnClickListener {
    return View.OnClickListener {
      val dialogBuilder = AlertDialog.Builder(context)
      dialogBuilder.setTitle(R.string.sign_message)
      dialogBuilder.setMessage(
          String.format(context.getString(R.string.confirm_to_sign), message.messageId.encoded))
      dialogBuilder.setNegativeButton(R.string.cancel, null)
      dialogBuilder.setPositiveButton(R.string.confirm) { _: DialogInterface?, _: Int ->
        laoViewModel.addDisposable(
            witnessingViewModel
                .signMessage(message)
                .subscribe(
                    {
                      Timber.tag(TAG).d("Witness message successfully signed")

                      button.isEnabled = false
                      val signed = activity.getString(R.string.signed)
                      button.text = signed
                    },
                    { error: Throwable ->
                      logAndShow(activity, TAG, error, R.string.error_sign_message)
                    }))
      }
      dialogBuilder.show()
    }
  }

  companion object {
    private val TAG = WitnessMessageListViewAdapter::class.java.simpleName
    private const val NO_SIGNATURES = "No signatures yet"

    private fun formatPublicKeys(witnesses: Set<PublicKey>?): String {
      return if (witnesses.isNullOrEmpty()) {
        NO_SIGNATURES
      } else {
        witnesses.stream().map(PublicKey::encoded).collect(Collectors.joining("\n"))
      }
    }
  }
}
