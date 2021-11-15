package com.github.dedis.popstellar.ui.socialmedia;

import androidx.fragment.app.Fragment;

import com.github.dedis.popstellar.databinding.SocialMediaProfileFragmentBinding;

public class SocialMediaProfileFragment extends Fragment {
  private SocialMediaProfileFragmentBinding mSocialMediaProfileFragBinding;
  private SocialMediaViewModel mSocialMediaViewModel;

  public static SocialMediaProfileFragment newInstance() {
    return new SocialMediaProfileFragment();
  }
}
