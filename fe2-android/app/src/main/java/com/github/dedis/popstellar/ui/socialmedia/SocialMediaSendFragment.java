package com.github.dedis.popstellar.ui.socialmedia;

import android.os.Bundle;
import android.view.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.SocialMediaSendFragmentBinding;
import com.github.dedis.popstellar.utility.error.ErrorUtils;

import java.time.Instant;

import dagger.hilt.android.AndroidEntryPoint;

/** Fragment where we can write and send a chirp */
@AndroidEntryPoint
public class SocialMediaSendFragment extends Fragment {
  private SocialMediaSendFragmentBinding mSocialMediaSendFragBinding;
  private SocialMediaViewModel mSocialMediaViewModel;

  public static final String TAG = SocialMediaSendFragment.class.getSimpleName();

  public static SocialMediaSendFragment newInstance() {
    return new SocialMediaSendFragment();
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    mSocialMediaSendFragBinding =
        SocialMediaSendFragmentBinding.inflate(inflater, container, false);

    mSocialMediaViewModel = SocialMediaActivity.obtainViewModel(requireActivity());

    mSocialMediaSendFragBinding.setViewModel(mSocialMediaViewModel);
    mSocialMediaSendFragBinding.setLifecycleOwner(getViewLifecycleOwner());

    return mSocialMediaSendFragBinding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    setupSendChirpButton();
  }

  private void setupSendChirpButton() {
    mSocialMediaSendFragBinding.sendChirpButton.setOnClickListener(v -> sendNewChirp());
  }

  private void sendNewChirp() {
    // Trying to send a chirp when no LAO has been chosen in the application will not send it, it
    // will
    // make a toast appear and it will log the error
    if (mSocialMediaViewModel.getLaoId().getValue() == null) {
      ErrorUtils.logAndShow(getContext(), TAG, R.string.error_no_lao);
    } else {
      mSocialMediaViewModel.sendChirp(
          mSocialMediaSendFragBinding.entryBoxChirp.getText().toString(),
          null,
          Instant.now().getEpochSecond());
      mSocialMediaViewModel.setCurrentTab(SocialMediaTab.HOME);
    }
  }
}
