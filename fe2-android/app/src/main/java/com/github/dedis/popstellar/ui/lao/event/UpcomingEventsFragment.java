package com.github.dedis.popstellar.ui.lao.event;

import android.os.Bundle;
import android.view.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.UpcomingEventsFragmentBinding;
import com.github.dedis.popstellar.ui.lao.LaoActivity;
import com.github.dedis.popstellar.ui.lao.LaoViewModel;
import com.github.dedis.popstellar.ui.lao.event.eventlist.UpcomingEventsAdapter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class UpcomingEventsFragment extends Fragment {

  private static final Logger logger = LogManager.getLogger(UpcomingEventsFragment.class);
  private LaoViewModel laoViewModel;

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
    laoViewModel = LaoActivity.obtainViewModel(requireActivity());
    EventsViewModel eventsViewModel =
        LaoActivity.obtainEventsEventsViewModel(requireActivity(), laoViewModel.getLaoId());

    binding.upcomingEventsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    binding.upcomingEventsRecyclerView.setAdapter(
        new UpcomingEventsAdapter(
            eventsViewModel.getEvents(), laoViewModel, requireActivity(), logger));

    handleBackNav();
    return binding.getRoot();
  }

  @Override
  public void onResume() {
    super.onResume();
    laoViewModel.setPageTitle(R.string.future_header_title);
    laoViewModel.setIsTab(false);
  }

  private void handleBackNav() {
    LaoActivity.addBackNavigationCallbackToEvents(
        requireActivity(), getViewLifecycleOwner(), logger);
  }
}
