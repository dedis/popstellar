package com.github.dedis.popstellar.ui.detail.event;

import android.os.Bundle;
import android.view.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.UpcomingEventsFragmentBinding;
import com.github.dedis.popstellar.ui.detail.event.eventlist.UpcomingEventsAdapter;
import com.github.dedis.popstellar.ui.lao.LaoActivity;
import com.github.dedis.popstellar.ui.lao.LaoViewModel;

public class UpcomingEventsFragment extends Fragment {

  private static final String TAG = UpcomingEventsFragment.class.getSimpleName();
  private LaoViewModel viewModel;
  private EventsViewModel eventsViewModel;

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
    viewModel = LaoActivity.obtainViewModel(requireActivity());
    EventsViewModel eventsViewModel =
        LaoActivity.obtainEventsEventsViewModel(requireActivity(), viewModel.getLaoId());

    binding.upcomingEventsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    binding.upcomingEventsRecyclerView.setAdapter(
        new UpcomingEventsAdapter(eventsViewModel.getEvents(), viewModel, requireActivity(), TAG));

    return binding.getRoot();
  }

  @Override
  public void onResume() {
    super.onResume();
    viewModel.setPageTitle(R.string.future_header_title);
    viewModel.setIsTab(false);
  }
}
