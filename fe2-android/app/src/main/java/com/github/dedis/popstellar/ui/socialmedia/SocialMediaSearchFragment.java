package com.github.dedis.popstellar.ui.socialmedia;

import android.os.Bundle;
import android.view.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.dedis.popstellar.databinding.SocialMediaSearchFragmentBinding;

import dagger.hilt.android.AndroidEntryPoint;

/** Fragment that let us search for chirps and users */
@AndroidEntryPoint
public class SocialMediaSearchFragment extends Fragment {

  public static SocialMediaSearchFragment newInstance() {
    return new SocialMediaSearchFragment();
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    SocialMediaSearchFragmentBinding mSocialMediaSearchFragBinding;
    SocialMediaViewModel mSocialMediaViewModel;

    mSocialMediaSearchFragBinding =
        SocialMediaSearchFragmentBinding.inflate(inflater, container, false);

    mSocialMediaViewModel = SocialMediaActivity.obtainViewModel(requireActivity());

    mSocialMediaSearchFragBinding.setViewModel(mSocialMediaViewModel);
    mSocialMediaSearchFragBinding.setLifecycleOwner(getViewLifecycleOwner());

    return mSocialMediaSearchFragBinding.getRoot();
  }
}
