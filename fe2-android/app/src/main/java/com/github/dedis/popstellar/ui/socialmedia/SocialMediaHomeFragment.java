package com.github.dedis.popstellar.ui.socialmedia;

import android.os.Bundle;
import android.view.*;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.SocialMediaHomeFragmentBinding;

import dagger.hilt.android.AndroidEntryPoint;

/** Fragment of the home feed of the social media */
@AndroidEntryPoint
public class SocialMediaHomeFragment extends Fragment {
  public static final String TAG = SocialMediaSendFragment.class.getSimpleName();

  private SocialMediaHomeFragmentBinding mSocialMediaHomeFragBinding;
  private SocialMediaViewModel viewModel;

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

    viewModel = SocialMediaActivity.obtainViewModel(requireActivity());

    mSocialMediaHomeFragBinding.setViewModel(viewModel);
    mSocialMediaHomeFragBinding.setLifecycleOwner(getViewLifecycleOwner());

    return mSocialMediaHomeFragBinding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    setupSendButton();
    setupListViewAdapter();
  }

  @Override
  public void onResume() {
    super.onResume();
    viewModel.setPageTitle(R.string.home);
  }

  private void setupSendButton() {
    mSocialMediaHomeFragBinding.socialMediaSendFragmentButton.setOnClickListener(
        v ->
            SocialMediaActivity.setCurrentFragment(
                getParentFragmentManager(),
                R.id.fragment_social_media_send,
                SocialMediaSendFragment::newInstance));
  }

  private void setupListViewAdapter() {
    ListView listView = mSocialMediaHomeFragBinding.chirpsList;
    ChirpListAdapter mChirpListAdapter = new ChirpListAdapter(requireActivity(), viewModel);
    listView.setAdapter(mChirpListAdapter);
  }
}
