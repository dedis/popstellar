package com.github.dedis.popstellar.ui.socialmedia;

import android.os.Bundle;
import android.view.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.SocialMediaFollowingFragmentBinding;

import dagger.hilt.android.AndroidEntryPoint;

/** Fragment that shows people we are subscribed to */
@AndroidEntryPoint
public class SocialMediaFollowingFragment extends Fragment {

  public static SocialMediaFollowingFragment newInstance() {
    return new SocialMediaFollowingFragment();
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    SocialMediaFollowingFragmentBinding mSocialMediaFollowingFragBinding;
    SocialMediaViewModel mSocialMediaViewModel;

    mSocialMediaFollowingFragBinding =
        SocialMediaFollowingFragmentBinding.inflate(inflater, container, false);

    mSocialMediaViewModel = SocialMediaActivity.obtainViewModel(requireActivity());

    mSocialMediaFollowingFragBinding.setViewModel(mSocialMediaViewModel);
    mSocialMediaFollowingFragBinding.setLifecycleOwner(getViewLifecycleOwner());

    return mSocialMediaFollowingFragBinding.getRoot();
  }

  @Override
  public void onResume() {
    super.onResume();
    SocialMediaViewModel viewModel = SocialMediaActivity.obtainViewModel(requireActivity());
    viewModel.setPageTitle(R.string.following);
  }
}
