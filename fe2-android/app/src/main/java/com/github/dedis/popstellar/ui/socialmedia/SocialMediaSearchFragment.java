package com.github.dedis.popstellar.ui.socialmedia;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.dedis.popstellar.databinding.SocialMediaSearchFragmentBinding;

public class SocialMediaSearchFragment extends Fragment {

  private static final String TAG = SocialMediaSearchFragment.class.getSimpleName();

  private SocialMediaSearchFragmentBinding mSocialMediaSearchFragBinding;
  private SocialMediaViewModel mSocialMediaViewModel;

  public static SocialMediaSearchFragment newInstance() {
    return new SocialMediaSearchFragment();
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    mSocialMediaSearchFragBinding =
        SocialMediaSearchFragmentBinding.inflate(inflater, container, false);

    mSocialMediaViewModel = SocialMediaActivity.obtainViewModel(getActivity());

    mSocialMediaSearchFragBinding.setViewModel(mSocialMediaViewModel);
    mSocialMediaSearchFragBinding.setLifecycleOwner(getActivity());

    return mSocialMediaSearchFragBinding.getRoot();
  }
}
