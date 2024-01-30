package com.github.dedis.popstellar.ui.lao.digitalcash

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.SingleEvent
import com.github.dedis.popstellar.databinding.DigitalCashReceiptFragmentBinding
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.addBackNavigationCallback
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.obtainDigitalCashViewModel
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.obtainViewModel
import com.github.dedis.popstellar.ui.lao.LaoViewModel
import com.github.dedis.popstellar.utility.ActivityUtils.buildBackButtonCallback

/**
 * A simple [Fragment] subclass. Use the [DigitalCashReceiptFragment] factory method to create an
 * instance of this fragment.
 */
class DigitalCashReceiptFragment : Fragment() {

  private lateinit var binding: DigitalCashReceiptFragmentBinding
  private lateinit var laoViewModel: LaoViewModel
  private lateinit var digitalCashViewModel: DigitalCashViewModel

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    binding = DigitalCashReceiptFragmentBinding.inflate(inflater, container, false)

    laoViewModel = obtainViewModel(requireActivity())
    digitalCashViewModel = obtainDigitalCashViewModel(requireActivity(), laoViewModel.laoId!!)

    handleBackNav()

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    digitalCashViewModel.getUpdateReceiptAmountEvent().observe(viewLifecycleOwner) {
        stringEvent: SingleEvent<String> ->
      val amount = stringEvent.contentIfNotHandled
      if (amount != null) {
        binding.digitalCashReceiptAmount.text = amount
      }
    }

    digitalCashViewModel.getUpdateReceiptAddressEvent().observe(viewLifecycleOwner) {
        stringEvent: SingleEvent<String> ->
      val address = stringEvent.contentIfNotHandled
      if (address != null) {
        binding.digitalCashReceiptBeneficiary.text =
            String.format(resources.getString(R.string.digital_cash_beneficiary_address), address)
      }
    }
  }

  override fun onResume() {
    super.onResume()
    laoViewModel.setPageTitle(R.string.digital_cash_receipt)
    laoViewModel.setIsTab(false)
  }

  private fun handleBackNav() {
    addBackNavigationCallback(
        requireActivity(),
        viewLifecycleOwner,
        buildBackButtonCallback(TAG, "digital cash home") {
          DigitalCashHomeFragment.openFragment(parentFragmentManager)
        })
  }

  companion object {
    val TAG: String = DigitalCashReceiptFragment::class.java.simpleName

    /**
     * Use this factory method to create a new instance of this fragment using the provided
     * parameters.
     *
     * @return A new instance of fragment DigitalCashReceiveFragment.
     */
    fun newInstance(): DigitalCashReceiptFragment {
      return DigitalCashReceiptFragment()
    }
  }
}
