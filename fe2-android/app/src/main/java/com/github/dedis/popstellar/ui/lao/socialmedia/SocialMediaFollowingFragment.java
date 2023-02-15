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

import dagger.hilt.android.AndroidEntryPoint;

/** Fragment that shows people we are subscribed to */
@AndroidEntryPoint
public class SocialMediaFollowingFragment extends Fragment {

  private LaoViewModel viewModel;

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

    viewModel = LaoActivity.obtainViewModel(requireActivity());
    SocialMediaViewModel socialMediaViewModel =
        LaoActivity.obtainSocialMediaViewModel(requireActivity(), viewModel.getLaoId());

    binding.setViewModel(socialMediaViewModel);
    binding.setLifecycleOwner(getViewLifecycleOwner());

    return binding.getRoot();
  }

  @Override
  public void onResume() {
    super.onResume();
    viewModel.setPageTitle(R.string.following);
    viewModel.setIsTab(true);
  }
}
