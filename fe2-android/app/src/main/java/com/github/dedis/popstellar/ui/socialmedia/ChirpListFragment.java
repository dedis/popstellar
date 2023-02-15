package com.github.dedis.popstellar.ui.socialmedia;

import android.os.Bundle;
import android.view.*;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.ChirpListFragmentBinding;
import com.github.dedis.popstellar.ui.lao.LaoActivity;
import com.github.dedis.popstellar.ui.lao.LaoViewModel;

import dagger.hilt.android.AndroidEntryPoint;

/** Fragment displaying the chirp list and the add chirp button */
@AndroidEntryPoint
public class ChirpListFragment extends Fragment {
  public static final String TAG = SocialMediaSendFragment.class.getSimpleName();

  private ChirpListFragmentBinding binding;
  private LaoViewModel viewModel;
  private SocialMediaViewModel socialMediaViewModel;

  public static ChirpListFragment newInstance() {
    return new ChirpListFragment();
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    binding = ChirpListFragmentBinding.inflate(inflater, container, false);

    viewModel = LaoActivity.obtainViewModel(requireActivity());
    socialMediaViewModel =
        LaoActivity.obtainSocialMediaViewModel(requireActivity(), viewModel.getLaoId());
    binding.setLifecycleOwner(getViewLifecycleOwner());

    return binding.getRoot();
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
    viewModel.setPageTitle(R.string.chirp_list);
    viewModel.setIsTab(true);
  }

  private void setupSendButton() {
    binding.socialMediaSendFragmentButton.setOnClickListener(
        v ->
            SocialMediaHomeFragment.setCurrentFragment(
                getParentFragmentManager(),
                R.id.fragment_social_media_send,
                SocialMediaSendFragment::newInstance));
  }

  private void setupListViewAdapter() {
    ListView listView = binding.chirpsList;
    ChirpListAdapter mChirpListAdapter =
        new ChirpListAdapter(requireActivity(), socialMediaViewModel, viewModel);
    listView.setAdapter(mChirpListAdapter);
  }
}
