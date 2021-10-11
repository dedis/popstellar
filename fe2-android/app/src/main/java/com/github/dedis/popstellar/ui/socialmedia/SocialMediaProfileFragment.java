package com.github.dedis.popstellar.ui.socialmedia;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.dedis.popstellar.databinding.SocialMediaProfileFragmentBinding;

public class SocialMediaProfileFragment extends Fragment {

  private static final String TAG = SocialMediaProfileFragment.class.getSimpleName();

  private SocialMediaProfileFragmentBinding mSocialMediaProfileFragBinding;
  private SocialMediaViewModel mSocialMediaViewModel;

  public static SocialMediaProfileFragment newInstance() {
    return new SocialMediaProfileFragment();
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    mSocialMediaProfileFragBinding =
        SocialMediaProfileFragmentBinding.inflate(inflater, container, false);

    mSocialMediaViewModel = SocialMediaActivity.obtainViewModel(getActivity());

    mSocialMediaProfileFragBinding.setViewModel(mSocialMediaViewModel);
    mSocialMediaProfileFragBinding.setLifecycleOwner(getActivity());

    return mSocialMediaProfileFragBinding.getRoot();
  }
}
