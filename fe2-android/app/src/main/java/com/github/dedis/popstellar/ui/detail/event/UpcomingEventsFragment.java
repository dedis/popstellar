package com.github.dedis.popstellar.ui.detail.event;

import android.os.Bundle;
import android.view.*;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.github.dedis.popstellar.databinding.UpcomingEventsFragmentBinding;
import com.github.dedis.popstellar.ui.detail.LaoDetailActivity;
import com.github.dedis.popstellar.ui.detail.LaoDetailViewModel;

import java.util.ArrayList;

public class UpcomingEventsFragment extends Fragment {

  public static UpcomingEventsFragment newInstance() {
    return new UpcomingEventsFragment();
  }

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    // Inflate the layout for this fragment
    UpcomingEventsFragmentBinding binding =
        UpcomingEventsFragmentBinding.inflate(inflater, container, false);
    LaoDetailViewModel viewModel = LaoDetailActivity.obtainViewModel(requireActivity());
    // Todo add the bar title when PR is merged

    binding.upcomingEventsRv.setLayoutManager(new LinearLayoutManager(getContext()));

    UpcomingEventsAdapter adapter =
        new UpcomingEventsAdapter(new ArrayList<>(), viewModel, requireActivity());
    binding.upcomingEventsRv.setAdapter(adapter);
    return binding.getRoot();
  }
}
