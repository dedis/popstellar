package com.github.dedis.popstellar.ui.socialmedia;

import android.os.Bundle;
import android.view.*;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.SocialMediaSendFragmentBinding;

import java.time.Instant;

import dagger.hilt.android.AndroidEntryPoint;

/** Fragment where we can write and send a chirp */
@AndroidEntryPoint
public class SocialMediaSendFragment extends Fragment {
  private SocialMediaSendFragmentBinding mSocialMediaSendFragBinding;
  private SocialMediaViewModel mSocialMediaViewModel;

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

    // Subscribe to "send chirp" event
    mSocialMediaViewModel
        .getSendNewChirpEvent()
        .observe(
            getViewLifecycleOwner(),
            booleanEvent -> {
              Boolean event = booleanEvent.getContentIfNotHandled();
              if (event != null) {
                sendNewChirp();
              }
            });
  }

  private void setupSendChirpButton() {
    mSocialMediaSendFragBinding.sendChirpButton.setOnClickListener(
        v -> mSocialMediaViewModel.sendNewChirpEvent());
  }

  private void sendNewChirp() {
    // Trying to send a chirp when no LAO has been chosen in the application will not send it and
    // make a toast appear
    if (mSocialMediaViewModel.getLaoId().getValue() == null) {
      Toast.makeText(
              requireContext().getApplicationContext(), R.string.error_no_lao, Toast.LENGTH_LONG)
          .show();
    } else {
      mSocialMediaViewModel.sendChirp(
          mSocialMediaSendFragBinding.entryBoxChirp.getText().toString(),
          null,
          Instant.now().getEpochSecond());
      mSocialMediaViewModel.openHome();
    }
  }
}
