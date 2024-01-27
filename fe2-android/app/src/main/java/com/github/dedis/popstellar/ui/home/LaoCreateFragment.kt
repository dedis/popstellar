package com.github.dedis.popstellar.ui.home

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CompoundButton
import androidx.fragment.app.Fragment
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.databinding.LaoCreateFragmentBinding
import com.github.dedis.popstellar.model.objects.security.PublicKey
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager
import com.github.dedis.popstellar.ui.lao.witness.WitnessingViewModel
import com.github.dedis.popstellar.ui.qrcode.QrScannerFragment.Companion.newInstance
import com.github.dedis.popstellar.ui.qrcode.ScanningAction
import dagger.hilt.android.AndroidEntryPoint
import java.util.stream.Collectors
import javax.inject.Inject
import timber.log.Timber

/** Fragment used to display the Launch UI */
@AndroidEntryPoint
class LaoCreateFragment : Fragment() {

  @Inject lateinit var networkManager: GlobalNetworkManager
  private lateinit var viewModel: HomeViewModel
  private lateinit var witnessingViewModel: WitnessingViewModel
  private lateinit var binding: LaoCreateFragmentBinding
  private var initialUrl: String? = null

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    binding = LaoCreateFragmentBinding.inflate(inflater, container, false)
    binding.lifecycleOwner = activity

    initialUrl = networkManager.currentUrl

    viewModel = HomeActivity.obtainViewModel(requireActivity())
    witnessingViewModel = HomeActivity.obtainWitnessingViewModel(requireActivity())

    setupClearButton()
    setupTextFields()
    setupAddWitnesses()
    setupCreateButton()
    setupWitnessingSwitch()
    handleBackNav()

    return binding.root
  }

  override fun onResume() {
    super.onResume()
    viewModel.setPageTitle(R.string.lao_create_title)
    viewModel.setIsHome(false)
  }

  private var launchWatcher: TextWatcher =
      object : TextWatcher {
        override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
          // Do nothing
        }

        override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
          val laoName = binding.laoNameEntry.editText?.text.toString().trim { it <= ' ' }
          val serverUrl = binding.serverUrlEntry.editText?.text.toString().trim { it <= ' ' }
          val areFieldsFilled = laoName.isNotEmpty() && serverUrl.isNotEmpty()
          binding.buttonCreate.isEnabled = areFieldsFilled
        }

        override fun afterTextChanged(editable: Editable) {
          // Do nothing
        }
      }

  private fun setupTextFields() {
    binding.serverUrlEntryEditText.setText(initialUrl)
    binding.serverUrlEntryEditText.addTextChangedListener(launchWatcher)
    binding.laoNameEntryEditText.addTextChangedListener(launchWatcher)
  }

  private fun setupAddWitnesses() {
    binding.addWitnessButton.setOnClickListener {
      Timber.tag(TAG).d("Opening scanner fragment")
      HomeActivity.setCurrentFragment(parentFragmentManager, R.id.fragment_qr_scanner) {
        newInstance(ScanningAction.ADD_WITNESS_AT_START)
      }
    }

    // No need to have a LiveData as the fragment is recreated upon exiting the scanner
    val witnesses =
        witnessingViewModel.scannedWitnesses
            .stream()
            .map(PublicKey::encoded)
            .collect(Collectors.toList())

    val witnessesListAdapter =
        ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, witnesses)
    binding.witnessesList.adapter = witnessesListAdapter
  }

  private fun setupCreateButton() {
    binding.buttonCreate.setOnClickListener {
      val serverAddress = binding.serverUrlEntryEditText.text!!.toString()
      val laoName = binding.laoNameEntryEditText.text!!.toString()
      val isWitnessingEnabled = java.lang.Boolean.TRUE == viewModel.isWitnessingEnabled.value

      Timber.tag(TAG).d("creating lao with name %s", laoName)

      val witnesses = witnessingViewModel.scannedWitnesses
      networkManager.connect(serverAddress)
      requireActivity()
          .startActivity(
              ConnectingActivity.newIntentForCreatingDetail(
                  requireContext(), laoName, witnesses, isWitnessingEnabled))
    }
  }

  private fun setupClearButton() {
    binding.buttonClearLaunch.setOnClickListener {
      binding.laoNameEntryEditText.text?.clear()
      binding.serverUrlEntryEditText.text?.clear()
      binding.enableWitnessingSwitch.isChecked = false
      witnessingViewModel.setWitnesses(emptyList())
    }
  }

  private fun setupWitnessingSwitch() {
    binding.enableWitnessingSwitch.setOnCheckedChangeListener {
        _: CompoundButton?,
        isChecked: Boolean ->
      viewModel.setIsWitnessingEnabled(isChecked)
      if (isChecked) {
        binding.addWitnessButton.visibility = View.VISIBLE
        if (witnessingViewModel.scannedWitnesses.isNotEmpty()) {
          binding.witnessesTitle.visibility = View.VISIBLE
          binding.witnessesList.visibility = View.VISIBLE
        }
      } else {
        binding.addWitnessButton.visibility = View.GONE
        binding.witnessesTitle.visibility = View.GONE
        binding.witnessesList.visibility = View.GONE
      }
    }

    // Use this to save the preference after opening and closing the QR code
    binding.enableWitnessingSwitch.isChecked =
        java.lang.Boolean.TRUE == viewModel.isWitnessingEnabled.value
  }

  private fun handleBackNav() {
    HomeActivity.addBackNavigationCallbackToHome(requireActivity(), viewLifecycleOwner, TAG)
  }

  companion object {
    val TAG: String = LaoCreateFragment::class.java.simpleName

    fun newInstance(): LaoCreateFragment {
      return LaoCreateFragment()
    }
  }
}
