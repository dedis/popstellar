package com.github.dedis.popstellar.ui.detail.event;

import android.os.Bundle;
import android.view.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.UpcomingEventsFragmentBinding;
import com.github.dedis.popstellar.ui.detail.LaoDetailActivity;
import com.github.dedis.popstellar.ui.detail.LaoDetailViewModel;
import com.github.dedis.popstellar.ui.detail.event.eventlist.UpcomingEventsAdapter;

public class UpcomingEventsFragment extends Fragment {

  private static final String TAG = UpcomingEventsFragment.class.getSimpleName();
  private LaoDetailViewModel viewModel;

  public static UpcomingEventsFragment newInstance() {
    return new UpcomingEventsFragment();
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {

    UpcomingEventsFragmentBinding binding =
        UpcomingEventsFragmentBinding.inflate(inflater, container, false);
    viewModel = LaoDetailActivity.obtainViewModel(requireActivity());

    binding.upcomingEventsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    binding.upcomingEventsRecyclerView.setAdapter(
        new UpcomingEventsAdapter(viewModel.getEvents(), viewModel, requireActivity(), TAG));

    return binding.getRoot();
  }

  @Override
  public void onResume() {
    super.onResume();
    viewModel.setPageTitle(R.string.future_header_title);
    viewModel.setIsTab(false);
  }
}
