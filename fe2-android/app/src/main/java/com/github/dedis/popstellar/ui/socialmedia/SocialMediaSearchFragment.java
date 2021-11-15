package com.github.dedis.popstellar.ui.socialmedia;

import androidx.fragment.app.Fragment;

import com.github.dedis.popstellar.databinding.SocialMediaSearchFragmentBinding;

public class SocialMediaSearchFragment extends Fragment {
  private SocialMediaSearchFragmentBinding mSocialMediaSearchFragBinding;
  private SocialMediaViewModel mSocialMediaViewModel;

  public static SocialMediaSearchFragment newInstance() {
    return new SocialMediaSearchFragment();
  }
}
