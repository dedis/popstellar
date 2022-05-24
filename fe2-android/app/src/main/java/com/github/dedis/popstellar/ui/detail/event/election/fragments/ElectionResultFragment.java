package com.github.dedis.popstellar.ui.detail.event.election.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.github.dedis.popstellar.databinding.ElectionResultFragmentBinding;
import com.github.dedis.popstellar.model.objects.Election;
import com.github.dedis.popstellar.ui.detail.LaoDetailActivity;
import com.github.dedis.popstellar.ui.detail.LaoDetailViewModel;
import com.github.dedis.popstellar.ui.detail.event.election.adapters.ElectionResultPagerAdapter;

import dagger.hilt.android.AndroidEntryPoint;
import me.relex.circleindicator.CircleIndicator3;

@AndroidEntryPoint
public class ElectionResultFragment extends Fragment {

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
    TextView electionNameView = mElectionResultFragBinding.electionResultPresentationTitle;

    // Getting election
    Election election = mLaoDetailViewModel.getCurrentElection();

    // Setting the Lao Name
    laoNameView.setText(mLaoDetailViewModel.getCurrentLaoName().getValue());

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
}
