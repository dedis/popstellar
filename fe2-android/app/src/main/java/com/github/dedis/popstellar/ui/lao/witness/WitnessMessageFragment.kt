package com.github.dedis.popstellar.ui.lao.witness

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.databinding.WitnessMessageFragmentBinding
import com.github.dedis.popstellar.model.objects.WitnessMessage
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.obtainViewModel
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.obtainWitnessingViewModel
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class WitnessMessageFragment : Fragment() {

  private lateinit var binding: WitnessMessageFragmentBinding
  private lateinit var witnessingViewModel: WitnessingViewModel
  private lateinit var adapter: WitnessMessageListViewAdapter

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    binding = WitnessMessageFragmentBinding.inflate(inflater, container, false)

    val viewModel = obtainViewModel(requireActivity())
    witnessingViewModel = obtainWitnessingViewModel(requireActivity(), viewModel.laoId)

    binding.lifecycleOwner = activity

    setupListAdapter()
    setupListUpdates()
    setupDeleteButton()

    return binding.root
  }

  private fun setupListAdapter() {
    val listView = binding.witnessMessageList
    adapter = WitnessMessageListViewAdapter(ArrayList(), requireActivity())
    listView.adapter = adapter
  }

  private fun setupListUpdates() {
    witnessingViewModel.witnessMessages.observe(requireActivity()) { messages: List<WitnessMessage>?
      ->
      Timber.tag(TAG).d("witness messages updated")
      adapter.replaceList(messages)
    }
  }

  private fun setupDeleteButton() {
    binding.witnessDeleteSignedMessage.setOnClickListener {
      AlertDialog.Builder(context)
          .setTitle(R.string.confirm_title)
          .setMessage(R.string.confirm_delete_witnessed_messages)
          .setPositiveButton(R.string.yes) { _: DialogInterface?, _: Int ->
            adapter.deleteSignedMessages()
          }
          .setNegativeButton(R.string.no) { dialogInterface: DialogInterface, _: Int ->
            dialogInterface.dismiss()
          }
          .show()
    }
  }

  companion object {
    val TAG: String = WitnessMessageFragment::class.java.simpleName

    fun newInstance(): WitnessMessageFragment {
      return WitnessMessageFragment()
    }
  }
}
