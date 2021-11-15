package com.github.dedis.popstellar.ui.socialmedia;

import androidx.fragment.app.Fragment;

import com.github.dedis.popstellar.databinding.SocialMediaFollowingFragmentBinding;

public class SocialMediaFollowingFragment extends Fragment {
  private SocialMediaFollowingFragmentBinding mSocialMediaFollowingFragBinding;
  private SocialMediaViewModel mSocialMediaViewModel;

  public static SocialMediaFollowingFragment newInstance() {
    return new SocialMediaFollowingFragment();
  }
}
