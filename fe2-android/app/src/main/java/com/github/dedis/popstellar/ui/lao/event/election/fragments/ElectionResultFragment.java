package com.github.dedis.popstellar.ui.lao.event.election.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.*;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.ElectionResultFragmentBinding;
import com.github.dedis.popstellar.model.objects.Election;
import com.github.dedis.popstellar.model.objects.view.LaoView;
import com.github.dedis.popstellar.repository.ElectionRepository;
import com.github.dedis.popstellar.ui.lao.LaoActivity;
import com.github.dedis.popstellar.ui.lao.LaoViewModel;
import com.github.dedis.popstellar.ui.lao.event.election.adapters.ElectionResultPagerAdapter;
import com.github.dedis.popstellar.utility.error.*;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import me.relex.circleindicator.CircleIndicator3;

import static com.github.dedis.popstellar.utility.error.ErrorUtils.logAndShow;

@AndroidEntryPoint
public class ElectionResultFragment extends Fragment {
  private static final String TAG = ElectionResultFragment.class.getSimpleName();

  private static final String ELECTION_ID = "election_id";
  private LaoViewModel viewModel;

  @Inject ElectionRepository electionRepository;

  public ElectionResultFragment() {
    // Required empty public constructor
  }

  public static ElectionResultFragment newInstance(String electionId) {
    ElectionResultFragment fragment = new ElectionResultFragment();

    Bundle bundle = new Bundle();
    bundle.putString(ELECTION_ID, electionId);
    fragment.setArguments(bundle);

    return fragment;
  }

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    // Inflate the layout for this fragment
    ElectionResultFragmentBinding binding =
        ElectionResultFragmentBinding.inflate(inflater, container, false);
    viewModel = LaoActivity.obtainViewModel(requireActivity());
    String electionId = requireArguments().getString(ELECTION_ID);
    try {
      LaoView laoView = viewModel.getLao();
      Election election = electionRepository.getElection(viewModel.getLaoId(), electionId);

      // Setting the Lao Name
      binding.electionResultLaoName.setText(laoView.getName());
      // Setting election name
      binding.electionResultElectionTitle.setText(election.getName());

      ElectionResultPagerAdapter adapter =
          new ElectionResultPagerAdapter(viewModel, electionRepository, election.getId());
      ViewPager2 viewPager2 = binding.electionResultPager;
      viewPager2.setAdapter(adapter);

      // Setting the circle indicator
      CircleIndicator3 circleIndicator = binding.swipeIndicatorElectionResults;
      circleIndicator.setViewPager(viewPager2);
    } catch (UnknownLaoException e) {
      ErrorUtils.logAndShow(requireContext(), TAG, R.string.error_no_lao);
      return null;
    } catch (UnknownElectionException e) {
      logAndShow(requireContext(), TAG, R.string.error_no_election);
      return null;
    }

    binding.setLifecycleOwner(getViewLifecycleOwner());

    handleBackNav();
    return binding.getRoot();
  }

  @Override
  public void onResume() {
    super.onResume();
    viewModel.setPageTitle(R.string.election_result_title);
    viewModel.setIsTab(false);
  }

  private void handleBackNav() {
    LaoActivity.addBackNavigationInstruction(
        requireActivity(),
        getViewLifecycleOwner(),
        new OnBackPressedCallback(true) {
          @Override
          public void handleOnBackPressed() {
            Log.d(TAG, "Back pressed, going to election");
            ElectionFragment.openFragment(
                getParentFragmentManager(), requireArguments().getString(ELECTION_ID));
          }
        });
  }
}
