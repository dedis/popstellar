package com.github.dedis.popstellar.ui.lao.socialmedia;

import android.os.Bundle;
import android.view.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.SocialMediaSendFragmentBinding;
import com.github.dedis.popstellar.ui.lao.LaoActivity;
import com.github.dedis.popstellar.ui.lao.LaoViewModel;
import com.github.dedis.popstellar.utility.ActivityUtils;
import com.github.dedis.popstellar.utility.error.ErrorUtils;
import com.github.dedis.popstellar.utility.error.keys.KeyException;

import java.security.GeneralSecurityException;
import java.time.Instant;

import dagger.hilt.android.AndroidEntryPoint;

/** Fragment where we can write and send a chirp */
@AndroidEntryPoint
public class SocialMediaSendFragment extends Fragment {

  public static final String TAG = SocialMediaSendFragment.class.getSimpleName();

  private SocialMediaSendFragmentBinding binding;
  private LaoViewModel laoViewModel;
  private SocialMediaViewModel socialMediaViewModel;

  public static SocialMediaSendFragment newInstance() {
    return new SocialMediaSendFragment();
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    binding = SocialMediaSendFragmentBinding.inflate(inflater, container, false);

    laoViewModel = LaoActivity.obtainViewModel(requireActivity());
    socialMediaViewModel =
        LaoActivity.obtainSocialMediaViewModel(requireActivity(), laoViewModel.getLaoId());

    binding.setViewModel(socialMediaViewModel);
    binding.setLifecycleOwner(getViewLifecycleOwner());

    handleBackNav();
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    setupSendChirpButton();
  }

  @Override
  public void onResume() {
    super.onResume();
    laoViewModel.setPageTitle(R.string.send);
    laoViewModel.setIsTab(false);
  }

  private void setupSendChirpButton() {
    binding.sendChirpButton.setOnClickListener(v -> sendNewChirp());
  }

  private void sendNewChirp() {
    // Trying to send a chirp when no LAO has been chosen in the application will not send it, it
    // will make a toast appear and it will log the error
    if (laoViewModel.getLaoId() == null) {
      ErrorUtils.logAndShow(requireContext(), TAG, R.string.error_no_lao);
    } else {
      laoViewModel.addDisposable(
          socialMediaViewModel
              .sendChirp(
                  binding.entryBoxChirp.getText().toString(), null, Instant.now().getEpochSecond())
              .subscribe(
                  msg ->
                      SocialMediaHomeFragment.setCurrentFragment(
                          getParentFragmentManager(),
                          R.id.fragment_chirp_list,
                          ChirpListFragment::newInstance),
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
      socialMediaViewModel.setBottomNavigationTab(SocialMediaTab.HOME);
    }
  }

  private void handleBackNav() {
    LaoActivity.addBackNavigationCallback(
        requireActivity(),
        getViewLifecycleOwner(),
        ActivityUtils.buildBackButtonCallback(
            TAG, "chirp list", () -> ChirpListFragment.OpenFragment(getParentFragmentManager())));
  }
}
