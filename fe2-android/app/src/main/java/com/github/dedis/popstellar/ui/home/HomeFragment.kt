package com.github.dedis.popstellar.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.databinding.HomeFragmentBinding
import com.github.dedis.popstellar.ui.qrcode.QrScannerFragment.Companion.newInstance
import com.github.dedis.popstellar.ui.qrcode.ScanningAction
import com.github.dedis.popstellar.utility.ActivityUtils.buildBackButtonCallback
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

/** Fragment used to display the Home UI */
@AndroidEntryPoint
class HomeFragment : Fragment() {
  private lateinit var binding: HomeFragmentBinding
  private lateinit var viewModel: HomeViewModel
  private lateinit var laoListAdapter: LAOListAdapter

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    binding = HomeFragmentBinding.inflate(inflater, container, false)

    binding.lifecycleOwner = activity
    viewModel = HomeActivity.obtainViewModel(requireActivity())

    setupListAdapter()
    setupListUpdates()
    setupButtonsActions()
    handleBackNav()

    return binding.root
  }

  override fun onResume() {
    super.onResume()
    viewModel.setPageTitle(R.string.home_title)
    viewModel.setIsHome(true)
  }

  private fun setupButtonsActions() {
    binding.homeCreateButton.setOnClickListener {
      Timber.tag(TAG).d("Opening Create fragment")
      HomeActivity.setCurrentFragment(parentFragmentManager, R.id.fragment_lao_create) {
        LaoCreateFragment.newInstance()
      }
    }

    binding.homeJoinButton.setOnClickListener {
      Timber.tag(TAG).d("Opening join fragment")
      HomeActivity.setCurrentFragment(parentFragmentManager, R.id.fragment_qr_scanner) {
        newInstance(ScanningAction.ADD_LAO_PARTICIPANT)
      }
      viewModel.setIsHome(false)
    }

    binding.homeQrButton.setOnClickListener {
      Timber.tag(TAG).d("Opening QR fragment")
      HomeActivity.setCurrentFragment(parentFragmentManager, R.id.fragment_qr) {
        QrFragment.newInstance()
      }
    }
  }

  private fun setupListUpdates() {
    viewModel.laoIdList.observe(requireActivity()) { laoIds: List<String> ->
      Timber.tag(TAG).d("Got a list update")

      laoListAdapter.setList(laoIds)
      if (laoIds.isNotEmpty()) {
        binding.homeNoLaoText.visibility = View.GONE
        binding.laoList.visibility = View.VISIBLE
      }
    }
  }

  private fun setupListAdapter() {
    val recyclerView = binding.laoList

    laoListAdapter = LAOListAdapter(viewModel, requireActivity())

    val mLayoutManager = LinearLayoutManager(context)
    recyclerView.layoutManager = mLayoutManager

    val dividerItemDecoration = DividerItemDecoration(requireContext(), mLayoutManager.orientation)
    recyclerView.addItemDecoration(dividerItemDecoration)

    recyclerView.adapter = laoListAdapter
  }

  private fun handleBackNav() {
    HomeActivity.addBackNavigationCallback(
        requireActivity(),
        viewLifecycleOwner,
        buildBackButtonCallback(TAG, "put the app in background") {
          requireActivity().moveTaskToBack(true)
        })
  }

  companion object {
    val TAG: String = HomeFragment::class.java.simpleName

    @JvmStatic
    fun newInstance(): HomeFragment {
      return HomeFragment()
    }
  }
}
