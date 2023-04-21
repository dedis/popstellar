package com.github.dedis.popstellar.ui.lao.socialmedia;

import android.os.Bundle;
import android.view.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.SocialMediaProfileFragmentBinding;
import com.github.dedis.popstellar.ui.lao.LaoActivity;
import com.github.dedis.popstellar.ui.lao.LaoViewModel;
import com.github.dedis.popstellar.utility.ActivityUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dagger.hilt.android.AndroidEntryPoint;

/** Fragment of the user's profile page */
@AndroidEntryPoint
public class SocialMediaProfileFragment extends Fragment {
  private static final Logger logger = LogManager.getLogger(SocialMediaProfileFragment.class);
  private LaoViewModel laoViewModel;

  public static SocialMediaProfileFragment newInstance() {
    return new SocialMediaProfileFragment();
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {

    SocialMediaProfileFragmentBinding binding =
        SocialMediaProfileFragmentBinding.inflate(inflater, container, false);

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
    laoViewModel.setPageTitle(R.string.profile);
    laoViewModel.setIsTab(true);
  }

  private void handleBackNav() {
    LaoActivity.addBackNavigationCallback(
        requireActivity(),
        getViewLifecycleOwner(),
        ActivityUtils.buildBackButtonCallback(
            logger,
            "social media home",
            () -> SocialMediaHomeFragment.openFragment(getParentFragmentManager())));
  }
}
