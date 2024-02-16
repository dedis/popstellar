package com.github.dedis.popstellar.ui.lao.popcha

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.SingleEvent
import com.github.dedis.popstellar.databinding.PopchaHomeFragmentBinding
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.addBackNavigationCallbackToEvents
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.obtainPoPCHAViewModel
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.obtainViewModel
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.setCurrentFragment
import com.github.dedis.popstellar.ui.lao.LaoViewModel
import com.github.dedis.popstellar.ui.qrcode.QrScannerFragment.Companion.newInstance
import com.github.dedis.popstellar.ui.qrcode.ScanningAction

class PoPCHAHomeFragment : Fragment() {
  private lateinit var laoViewModel: LaoViewModel

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    laoViewModel = obtainViewModel(requireActivity())
    val popCHAViewModel = obtainPoPCHAViewModel(requireActivity(), laoViewModel.laoId)

    val binding = PopchaHomeFragmentBinding.inflate(inflater, container, false)

    binding.popchaHeader.text =
        String.format(resources.getString(R.string.popcha_header), popCHAViewModel.laoId)
    binding.popchaScanner.setOnClickListener { openScanner() }

    popCHAViewModel.textDisplayed.observe(viewLifecycleOwner) {
        stringSingleEvent: SingleEvent<String> ->
      val url = stringSingleEvent.contentIfNotHandled
      if (url != null) {
        binding.popchaText.text = url
      }
    }

    popCHAViewModel.isRequestCompleted.observe(viewLifecycleOwner) {
        booleanSingleEvent: SingleEvent<Boolean> ->
      val finished = booleanSingleEvent.contentIfNotHandled
      if (finished == true) {
        closeScanner()
        popCHAViewModel.deactivateRequestCompleted()
      }
    }

    handleBackNav()

    return binding.root
  }

  override fun onResume() {
    super.onResume()
    laoViewModel.setPageTitle(R.string.popcha)
    laoViewModel.setIsTab(true)
  }

  private fun openScanner() {
    laoViewModel.setIsTab(false)
    setCurrentFragment(parentFragmentManager, R.id.fragment_qr_scanner) {
      newInstance(ScanningAction.ADD_POPCHA)
    }
  }

  private fun closeScanner() {
    laoViewModel.setIsTab(true)
    parentFragmentManager.popBackStack()
  }

  private fun handleBackNav() {
    addBackNavigationCallbackToEvents(requireActivity(), viewLifecycleOwner, TAG)
  }

  companion object {
    @JvmStatic
    fun newInstance(): PoPCHAHomeFragment {
      return PoPCHAHomeFragment()
    }

    val TAG: String = PoPCHAHomeFragment::class.java.simpleName

    fun openFragment(manager: FragmentManager) {
      setCurrentFragment(manager, R.id.fragment_popcha_home) { PoPCHAHomeFragment() }
    }
  }
}
