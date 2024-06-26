package com.github.dedis.popstellar.ui.lao.socialmedia

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.databinding.ChirpListFragmentBinding
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.obtainSocialMediaViewModel
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.obtainViewModel
import com.github.dedis.popstellar.ui.lao.LaoViewModel
import dagger.hilt.android.AndroidEntryPoint

/** Fragment displaying the chirp list and the add chirp button */
@AndroidEntryPoint
class ChirpListFragment : Fragment() {
  private lateinit var binding: ChirpListFragmentBinding
  private lateinit var laoViewModel: LaoViewModel
  private lateinit var socialMediaViewModel: SocialMediaViewModel

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    binding = ChirpListFragmentBinding.inflate(inflater, container, false)

    laoViewModel = obtainViewModel(requireActivity())
    socialMediaViewModel = obtainSocialMediaViewModel(requireActivity(), laoViewModel.laoId!!)

    binding.lifecycleOwner = viewLifecycleOwner

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    setupSendButton()
    setupListViewAdapter()
  }

  override fun onResume() {
    super.onResume()
    laoViewModel.setPageTitle(R.string.chirp_list)
    laoViewModel.setIsTab(true)
  }

  private fun setupSendButton() {
    binding.socialMediaSendFragmentButton.setOnClickListener {
      SocialMediaHomeFragment.setCurrentFragment(
          parentFragmentManager, R.id.fragment_social_media_send) {
            SocialMediaSendFragment.newInstance()
          }
    }
  }

  private fun setupListViewAdapter() {
    val listView = binding.chirpsList
    val mChirpListAdapter = ChirpListAdapter(requireActivity(), socialMediaViewModel, laoViewModel)
    listView.adapter = mChirpListAdapter
  }

  companion object {
    val TAG: String = SocialMediaSendFragment::class.java.simpleName

    @JvmStatic
    fun newInstance(): ChirpListFragment {
      return ChirpListFragment()
    }

    fun openFragment(manager: FragmentManager) {
      SocialMediaHomeFragment.setCurrentFragment(manager, R.id.fragment_chirp_list) {
        newInstance()
      }
    }
  }
}
