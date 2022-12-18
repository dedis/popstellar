package com.github.dedis.popstellar.ui.socialmedia;

import android.os.Bundle;
import android.view.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.SocialMediaProfileFragmentBinding;

import dagger.hilt.android.AndroidEntryPoint;

/** Fragment of the user's profile page */
@AndroidEntryPoint
public class SocialMediaProfileFragment extends Fragment {

  public static SocialMediaProfileFragment newInstance() {
    return new SocialMediaProfileFragment();
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    SocialMediaProfileFragmentBinding mSocialMediaProfileFragBinding;
    SocialMediaViewModel mSocialMediaViewModel;

    mSocialMediaProfileFragBinding =
        SocialMediaProfileFragmentBinding.inflate(inflater, container, false);

    mSocialMediaViewModel = SocialMediaActivity.obtainViewModel(requireActivity());

    mSocialMediaProfileFragBinding.setViewModel(mSocialMediaViewModel);
    mSocialMediaProfileFragBinding.setLifecycleOwner(getViewLifecycleOwner());

    return mSocialMediaProfileFragBinding.getRoot();
  }

  @Override
  public void onResume() {
    super.onResume();
    SocialMediaViewModel viewModel = SocialMediaActivity.obtainViewModel(requireActivity());
    viewModel.setPageTitle(R.string.profile);
  }
}
