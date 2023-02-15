package com.github.dedis.popstellar.ui.socialmedia;

import android.os.Bundle;
import android.util.Log;
import android.view.*;

import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.SocialMediaHomeFragmentBinding;
import com.github.dedis.popstellar.ui.lao.LaoActivity;
import com.github.dedis.popstellar.ui.lao.LaoViewModel;
import com.github.dedis.popstellar.utility.ActivityUtils;

import java.util.function.Supplier;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * The purpose of this fragment is to provide a bottom nav bar and fragment container to social
 * media fragments. In itself this fragment does not provide any social media feature
 */
@AndroidEntryPoint
public class SocialMediaHomeFragment extends Fragment {

  private SocialMediaViewModel socialMediaViewModel;
  private SocialMediaHomeFragmentBinding binding;

  public static final String TAG = SocialMediaHomeFragment.class.getSimpleName();

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    binding = SocialMediaHomeFragmentBinding.inflate(inflater, container, false);

    LaoViewModel viewModel = LaoActivity.obtainViewModel(requireActivity());
    socialMediaViewModel =
        LaoActivity.obtainSocialMediaViewModel(requireActivity(), viewModel.getLaoId());

    setupBottomNavBar();
    openChirpList();
    return binding.getRoot();
  }

  private void setupBottomNavBar() {
    socialMediaViewModel
        .getBottomNavigationTab()
        .observe(
            getViewLifecycleOwner(),
            tab -> binding.socialMediaNavBar.setSelectedItemId(tab.getMenuId()));

    binding.socialMediaNavBar.setOnItemSelectedListener(
        item -> {
          SocialMediaTab tab = SocialMediaTab.findByMenu(item.getItemId());
          Log.i(TAG, "Opening tab : " + tab.getName());
          openBottomTab(tab);
          return true;
        });

    binding.socialMediaNavBar.setOnItemReselectedListener(item -> {});
    socialMediaViewModel.setBottomNavigationTab(SocialMediaTab.HOME);
  }

  private void openBottomTab(SocialMediaTab tab) {
    switch (tab) {
      case HOME:
        openChirpList();
        break;
      case SEARCH:
        openSearchTab();
        break;
      case FOLLOWING:
        openFollowingTab();
        break;
      case PROFILE:
        openProfileTab();
        break;
    }
  }

  private void openChirpList() {
    setCurrentFragment(
        getParentFragmentManager(), R.id.fragment_chirp_list, ChirpListFragment::newInstance);
  }

  private void openSearchTab() {
    setCurrentFragment(
        getParentFragmentManager(),
        R.id.fragment_social_media_search,
        SocialMediaSearchFragment::newInstance);
  }

  private void openFollowingTab() {
    setCurrentFragment(
        getParentFragmentManager(),
        R.id.fragment_social_media_following,
        SocialMediaFollowingFragment::newInstance);
  }

  private void openProfileTab() {
    setCurrentFragment(
        getParentFragmentManager(),
        R.id.fragment_social_media_profile,
        SocialMediaProfileFragment::newInstance);
  }

  /**
   * Set the current fragment in the container of the home fragment
   *
   * @param id of the fragment
   * @param fragmentSupplier provides the fragment if it is missing
   */
  public static void setCurrentFragment(
      FragmentManager manager, @IdRes int id, Supplier<Fragment> fragmentSupplier) {
    ActivityUtils.setFragmentInContainer(
        manager, R.id.fragment_container_social_media, id, fragmentSupplier);
  }
}
