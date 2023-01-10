package com.github.dedis.popstellar.ui.detail.event.election.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.ElectionResultFragmentBinding;
import com.github.dedis.popstellar.model.objects.Election;
import com.github.dedis.popstellar.model.objects.view.LaoView;
import com.github.dedis.popstellar.ui.detail.LaoDetailActivity;
import com.github.dedis.popstellar.ui.detail.LaoDetailViewModel;
import com.github.dedis.popstellar.ui.detail.event.election.adapters.ElectionResultPagerAdapter;
import com.github.dedis.popstellar.utility.error.ErrorUtils;
import com.github.dedis.popstellar.utility.error.UnknownLaoException;

import dagger.hilt.android.AndroidEntryPoint;
import me.relex.circleindicator.CircleIndicator3;

@AndroidEntryPoint
public class ElectionResultFragment extends Fragment {
  private static final String TAG = ElectionResultFragment.class.getSimpleName();

  public ElectionResultFragment() {
    // Required empty public constructor
  }

  public static ElectionResultFragment newInstance() {
    return new ElectionResultFragment();
  }

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    // Inflate the layout for this fragment
    ElectionResultFragmentBinding mElectionResultFragBinding =
        ElectionResultFragmentBinding.inflate(inflater, container, false);
    LaoDetailViewModel mLaoDetailViewModel = LaoDetailActivity.obtainViewModel(requireActivity());

    TextView laoNameView = mElectionResultFragBinding.electionResultLaoName;
    TextView electionNameView = mElectionResultFragBinding.electionResultElectionTitle;

    // Getting LAO
    LaoView laoView;
    try {
      laoView = mLaoDetailViewModel.getLaoView();
    } catch (UnknownLaoException e) {
      ErrorUtils.logAndShow(requireContext(), TAG, R.string.error_no_lao);
      return null;
    }

    // Getting election
    Election election = mLaoDetailViewModel.getCurrentElection();
    if (election == null) {
      Log.e(TAG, "No election in view model");
      return null;
    }

    // Setting the Lao Name
    laoNameView.setText(laoView.getName());

    // Setting election name
    electionNameView.setText(election.getName());

    ElectionResultPagerAdapter adapter = new ElectionResultPagerAdapter(mLaoDetailViewModel);
    ViewPager2 viewPager2 = mElectionResultFragBinding.electionResultPager;
    viewPager2.setAdapter(adapter);

    // Setting the circle indicator
    CircleIndicator3 circleIndicator = mElectionResultFragBinding.swipeIndicatorElectionResults;
    circleIndicator.setViewPager(viewPager2);

    mElectionResultFragBinding.setLifecycleOwner(getActivity());
    return mElectionResultFragBinding.getRoot();
  }

  @Override
  public void onResume() {
    super.onResume();
    LaoDetailViewModel viewModel = LaoDetailActivity.obtainViewModel(requireActivity());
    viewModel.setPageTitle(R.string.election_result_title);
  }
}
