package com.github.dedis.popstellar.ui.lao.socialmedia

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.databinding.SocialMediaFollowingFragmentBinding
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.addBackNavigationCallback
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.obtainSocialMediaViewModel
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.obtainViewModel
import com.github.dedis.popstellar.ui.lao.LaoViewModel
import com.github.dedis.popstellar.utility.ActivityUtils.buildBackButtonCallback
import dagger.hilt.android.AndroidEntryPoint

/** Fragment that shows people we are subscribed to */
@AndroidEntryPoint
class SocialMediaFollowingFragment : Fragment() {

  private lateinit var laoViewModel: LaoViewModel

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    val binding = SocialMediaFollowingFragmentBinding.inflate(inflater, container, false)

    laoViewModel = obtainViewModel(requireActivity())
    val socialMediaViewModel = obtainSocialMediaViewModel(requireActivity(), laoViewModel.laoId!!)

    binding.viewModel = socialMediaViewModel
    binding.lifecycleOwner = viewLifecycleOwner

    handleBackNav()

    return binding.root
  }

  override fun onResume() {
    super.onResume()
    laoViewModel.setPageTitle(R.string.following)
    laoViewModel.setIsTab(true)
  }

  private fun handleBackNav() {
    addBackNavigationCallback(
        requireActivity(),
        viewLifecycleOwner,
        buildBackButtonCallback(TAG, "social media home") {
          SocialMediaHomeFragment.openFragment(parentFragmentManager)
        })
  }

  companion object {
    val TAG: String = SocialMediaFollowingFragment::class.java.simpleName

    @JvmStatic
    fun newInstance(): SocialMediaFollowingFragment {
      return SocialMediaFollowingFragment()
    }
  }
}
