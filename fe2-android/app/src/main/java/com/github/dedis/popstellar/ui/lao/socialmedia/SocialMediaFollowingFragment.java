package com.github.dedis.popstellar.ui.lao.socialmedia;

import android.os.Bundle;
import android.view.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.SocialMediaFollowingFragmentBinding;
import com.github.dedis.popstellar.ui.lao.LaoActivity;
import com.github.dedis.popstellar.ui.lao.LaoViewModel;
import com.github.dedis.popstellar.utility.ActivityUtils;

import dagger.hilt.android.AndroidEntryPoint;

/** Fragment that shows people we are subscribed to */
@AndroidEntryPoint
public class SocialMediaFollowingFragment extends Fragment {

  public static final String TAG = SocialMediaFollowingFragment.class.getSimpleName();
  private LaoViewModel laoViewModel;

  public static SocialMediaFollowingFragment newInstance() {
    return new SocialMediaFollowingFragment();
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {

    SocialMediaFollowingFragmentBinding binding =
        SocialMediaFollowingFragmentBinding.inflate(inflater, container, false);

    laoViewModel = LaoActivity.obtainViewModel(requireActivity());
    SocialMediaViewModel socialMediaViewModel =
        LaoActivity.obtainSocialMediaViewModel(requireActivity(), laoViewModel.getLaoId());

    binding.setViewModel(socialMediaViewModel);
    binding.setLifecycleOwner(getViewLifecycleOwner());
    handleBackNav();
    return binding.getRoot();
  }

  @Override
  public void onResume() {
    super.onResume();
    laoViewModel.setPageTitle(R.string.following);
    laoViewModel.setIsTab(true);
  }

  private void handleBackNav() {
    LaoActivity.addBackNavigationCallback(
        requireActivity(),
        getViewLifecycleOwner(),
        ActivityUtils.buildBackButtonCallback(
            TAG,
            "social media home",
            () -> SocialMediaHomeFragment.openFragment(getParentFragmentManager())));
  }
}
