package com.github.dedis.popstellar.ui.socialmedia;

import android.os.Bundle;
import android.view.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.SocialMediaSendFragmentBinding;
import com.github.dedis.popstellar.utility.error.ErrorUtils;
import com.github.dedis.popstellar.utility.error.keys.KeyException;

import java.security.GeneralSecurityException;
import java.time.Instant;

import dagger.hilt.android.AndroidEntryPoint;

/** Fragment where we can write and send a chirp */
@AndroidEntryPoint
public class SocialMediaSendFragment extends Fragment {

  public static final String TAG = SocialMediaSendFragment.class.getSimpleName();

  private SocialMediaSendFragmentBinding mSocialMediaSendFragBinding;
  private SocialMediaViewModel viewModel;

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

    viewModel = SocialMediaActivity.obtainViewModel(requireActivity());

    mSocialMediaSendFragBinding.setViewModel(viewModel);
    mSocialMediaSendFragBinding.setLifecycleOwner(getViewLifecycleOwner());

    return mSocialMediaSendFragBinding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    setupSendChirpButton();
  }

  @Override
  public void onResume() {
    super.onResume();
    viewModel.setPageTitle(R.string.send);
  }

  private void setupSendChirpButton() {
    mSocialMediaSendFragBinding.sendChirpButton.setOnClickListener(v -> sendNewChirp());
  }

  private void sendNewChirp() {
    // Trying to send a chirp when no LAO has been chosen in the application will not send it, it
    // will
    // make a toast appear and it will log the error
    if (viewModel.getLaoId() == null) {
      ErrorUtils.logAndShow(requireContext(), TAG, R.string.error_no_lao);
    } else {
      viewModel.addDisposable(
          viewModel
              .sendChirp(
                  mSocialMediaSendFragBinding.entryBoxChirp.getText().toString(),
                  null,
                  Instant.now().getEpochSecond())
              .subscribe(
                  msg ->
                      SocialMediaActivity.setCurrentFragment(
                          getParentFragmentManager(),
                          R.id.fragment_social_media_home,
                          SocialMediaHomeFragment::newInstance),
                  error -> {
                    if (error instanceof KeyException
                        || error instanceof GeneralSecurityException) {
                      ErrorUtils.logAndShow(
                          requireContext(), TAG, error, R.string.error_retrieve_own_token);
                    } else {
                      ErrorUtils.logAndShow(
                          requireContext(), TAG, error, R.string.error_sending_chirp);
                    }
                  }));
      viewModel.setBottomNavigationTab(SocialMediaTab.HOME);
    }
  }
}
