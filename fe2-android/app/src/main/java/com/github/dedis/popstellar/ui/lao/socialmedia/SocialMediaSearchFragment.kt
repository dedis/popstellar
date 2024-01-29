package com.github.dedis.popstellar.ui.lao.socialmedia

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.databinding.SocialMediaSearchFragmentBinding
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.addBackNavigationCallback
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.obtainSocialMediaViewModel
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.obtainViewModel
import com.github.dedis.popstellar.ui.lao.LaoViewModel
import com.github.dedis.popstellar.utility.ActivityUtils.buildBackButtonCallback
import dagger.hilt.android.AndroidEntryPoint

/** Fragment that let us search for chirps and users */
@AndroidEntryPoint
class SocialMediaSearchFragment : Fragment() {

  private lateinit var laoViewModel: LaoViewModel

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    val binding: SocialMediaSearchFragmentBinding =
        SocialMediaSearchFragmentBinding.inflate(inflater, container, false)

    laoViewModel = obtainViewModel(requireActivity())
    val socialMediaViewModel = obtainSocialMediaViewModel(requireActivity(), laoViewModel.laoId!!)

    binding.viewModel = socialMediaViewModel
    binding.lifecycleOwner = viewLifecycleOwner

    handleBackNav()

    return binding.root
  }

  override fun onResume() {
    super.onResume()
    laoViewModel.setPageTitle(R.string.search)
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
    val TAG: String = SocialMediaSearchFragment::class.java.simpleName

    @JvmStatic
    fun newInstance(): SocialMediaSearchFragment {
      return SocialMediaSearchFragment()
    }
  }
}
