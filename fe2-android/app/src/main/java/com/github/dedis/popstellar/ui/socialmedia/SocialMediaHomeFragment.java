package com.github.dedis.popstellar.ui.socialmedia;

import android.os.*;
import android.view.*;
import android.widget.ListView;

import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.SocialMediaHomeFragmentBinding;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.utility.ActivityUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.function.Supplier;

import dagger.hilt.android.AndroidEntryPoint;

/** Fragment of the home feed of the social media */
@AndroidEntryPoint
public class SocialMediaHomeFragment extends Fragment {
  private SocialMediaHomeFragmentBinding mSocialMediaHomeFragBinding;
  private SocialMediaViewModel mSocialMediaViewModel;
  private ChirpListAdapter mChirpListAdapter;

  public static SocialMediaHomeFragment newInstance() {
    return new SocialMediaHomeFragment();
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    mSocialMediaHomeFragBinding =
        SocialMediaHomeFragmentBinding.inflate(inflater, container, false);

    mSocialMediaViewModel = SocialMediaActivity.obtainViewModel(requireActivity());

    mSocialMediaHomeFragBinding.setViewModel(mSocialMediaViewModel);
    mSocialMediaHomeFragBinding.setLifecycleOwner(getViewLifecycleOwner());

    return mSocialMediaHomeFragBinding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    setupSendButton();
    setupListViewAdapter();
    setupListUpdate();
    setupSwipeRefresh();

    // Subscribe to "open send" event
    mSocialMediaViewModel
        .getOpenSendEvent()
        .observe(
            getViewLifecycleOwner(),
            booleanEvent -> {
              Boolean event = booleanEvent.getContentIfNotHandled();
              if (event != null) {
                setupSocialMediaSendFragment();
              }
            });

    // Subscribe to "delete chirp" event
    mSocialMediaViewModel
        .getDeleteChirpEvent()
        .observe(
            getViewLifecycleOwner(),
            messageIDEvent -> {
              MessageID chirpId = messageIDEvent.getContentIfNotHandled();
              if (chirpId != null) {
                deleteChirp(chirpId);
              }
            });
  }

  private void setupSendButton() {
    mSocialMediaHomeFragBinding.socialMediaSendFragmentButton.setOnClickListener(
        v -> mSocialMediaViewModel.openSend());
  }

  private void setupSwipeRefresh() {
    SwipeRefreshLayout swipeRefreshLayout = mSocialMediaHomeFragBinding.swipeRefreshChirps;
    swipeRefreshLayout.setOnRefreshListener(
        () -> {
          mChirpListAdapter.replaceList(
              mSocialMediaViewModel.getChirpList(mSocialMediaViewModel.getLaoId().getValue()));

          final Handler handler = new Handler(Looper.getMainLooper());
          handler.postDelayed(
              () -> {
                if (swipeRefreshLayout.isRefreshing()) {
                  swipeRefreshLayout.setRefreshing(false);
                }
              },
              1000);
        });
  }

  private void setupSocialMediaSendFragment() {
    setCurrentFragment(R.id.fragment_social_media_send, SocialMediaSendFragment::newInstance);
  }

  private void setupListViewAdapter() {
    ListView listView = mSocialMediaHomeFragBinding.chirpsList;
    mChirpListAdapter =
        new ChirpListAdapter(getActivity(), mSocialMediaViewModel, new ArrayList<>());
    listView.setAdapter(mChirpListAdapter);
  }

  private void setupListUpdate() {
    mSocialMediaViewModel
        .getLaoId()
        .observe(
            getViewLifecycleOwner(),
            newLaoId ->
                mChirpListAdapter.replaceList(mSocialMediaViewModel.getChirpList(newLaoId)));
  }

  private void deleteChirp(MessageID chirpId) {
    mSocialMediaViewModel.deleteChirp(chirpId, Instant.now().getEpochSecond());
  }

  /**
   * Set the current fragment in the container of the activity
   *
   * @param id of the fragment
   * @param fragmentSupplier provides the fragment if it is missing
   */
  private void setCurrentFragment(@IdRes int id, Supplier<Fragment> fragmentSupplier) {
    Fragment fragment = requireActivity().getSupportFragmentManager().findFragmentById(id);
    // If the fragment was not created yet, create it now
    if (fragment == null) fragment = fragmentSupplier.get();

    // Set the new fragment in the container
    ActivityUtils.replaceFragmentInActivity(
        requireActivity().getSupportFragmentManager(),
        fragment,
        R.id.fragment_container_social_media);
  }
}
