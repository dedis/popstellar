package com.github.dedis.popstellar.ui.lao.socialmedia

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.databinding.SocialMediaHomeFragmentBinding
import com.github.dedis.popstellar.ui.lao.LaoActivity
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.addBackNavigationCallbackToEvents
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.obtainSocialMediaViewModel
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.obtainViewModel
import com.github.dedis.popstellar.ui.lao.socialmedia.SocialMediaTab.Companion.findByMenu
import com.github.dedis.popstellar.utility.ActivityUtils.setFragmentInContainer
import dagger.hilt.android.AndroidEntryPoint
import java.util.function.Supplier
import timber.log.Timber

/**
 * The purpose of this fragment is to provide a bottom nav bar and fragment container to social
 * media fragments. In itself this fragment does not provide any social media feature
 */
@AndroidEntryPoint
class SocialMediaHomeFragment : Fragment() {

  private lateinit var socialMediaViewModel: SocialMediaViewModel
  private lateinit var binding: SocialMediaHomeFragmentBinding

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    binding = SocialMediaHomeFragmentBinding.inflate(inflater, container, false)

    val viewModel = obtainViewModel(requireActivity())
    socialMediaViewModel = obtainSocialMediaViewModel(requireActivity(), viewModel.laoId!!)
    socialMediaViewModel.checkValidPoPToken()

    setupBottomNavBar()
    openChirpList()
    handleBackNav()

    return binding.root
  }

  private fun setupBottomNavBar() {
    socialMediaViewModel.bottomNavigationTab.observe(viewLifecycleOwner) { tab: SocialMediaTab ->
      binding.socialMediaNavBar.selectedItemId = tab.menuId
    }

    binding.socialMediaNavBar.setOnItemSelectedListener { item: MenuItem ->
      val tab = findByMenu(item.itemId)
      Timber.tag(TAG).i("Opening tab : %s", tab.name)
      openBottomTab(tab)
      true
    }

    binding.socialMediaNavBar.setOnItemReselectedListener {}
    socialMediaViewModel.setBottomNavigationTab(SocialMediaTab.HOME)
  }

  private fun openBottomTab(tab: SocialMediaTab) {
    when (tab) {
      SocialMediaTab.HOME -> openChirpList()
      SocialMediaTab.SEARCH -> openSearchTab()
      SocialMediaTab.FOLLOWING -> openFollowingTab()
      SocialMediaTab.PROFILE -> openProfileTab()
    }
  }

  private fun openChirpList() {
    setCurrentFragment(parentFragmentManager, R.id.fragment_chirp_list) {
      ChirpListFragment.newInstance()
    }
  }

  private fun openSearchTab() {
    setCurrentFragment(parentFragmentManager, R.id.fragment_social_media_search) {
      SocialMediaSearchFragment.newInstance()
    }
  }

  private fun openFollowingTab() {
    setCurrentFragment(parentFragmentManager, R.id.fragment_social_media_following) {
      SocialMediaFollowingFragment.newInstance()
    }
  }

  private fun openProfileTab() {
    setCurrentFragment(parentFragmentManager, R.id.fragment_social_media_profile) {
      SocialMediaProfileFragment.newInstance()
    }
  }

  private fun handleBackNav() {
    addBackNavigationCallbackToEvents(requireActivity(), viewLifecycleOwner, TAG)
  }

  companion object {
    val TAG: String = SocialMediaHomeFragment::class.java.simpleName

    fun newInstance(): SocialMediaHomeFragment {
      return SocialMediaHomeFragment()
    }

    /**
     * Set the current fragment in the container of the home fragment
     *
     * @param id of the fragment
     * @param fragmentSupplier provides the fragment if it is missing
     */
    fun setCurrentFragment(
        manager: FragmentManager,
        @IdRes id: Int,
        fragmentSupplier: Supplier<Fragment>
    ) {
      setFragmentInContainer(manager, R.id.fragment_container_social_media, id, fragmentSupplier)
    }

    fun openFragment(manager: FragmentManager) {
      LaoActivity.setCurrentFragment(manager, R.id.fragment_social_media_home) {
        SocialMediaHomeFragment()
      }
    }
  }
}
