package com.github.dedis.popstellar.ui.socialmedia;

import android.os.*;
import android.view.*;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.SocialMediaHomeFragmentBinding;

import java.util.ArrayList;

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
  }

  private void setupSendButton() {
    mSocialMediaHomeFragBinding.socialMediaSendFragmentButton.setOnClickListener(
        v ->
            SocialMediaActivity.setCurrentFragment(
                getParentFragmentManager(),
                R.id.fragment_social_media_send,
                SocialMediaSendFragment::newInstance));
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

  private void setupListViewAdapter() {
    ListView listView = mSocialMediaHomeFragBinding.chirpsList;
    mChirpListAdapter =
        new ChirpListAdapter(requireActivity(), mSocialMediaViewModel, new ArrayList<>());
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
}
