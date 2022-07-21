package com.github.dedis.popstellar.ui.detail.witness;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.ui.detail.LaoDetailActivity;
import com.github.dedis.popstellar.ui.detail.LaoDetailViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class WitnessesFragment extends Fragment {

  public WitnessesFragment() {
    // Required empty public constructor
  }

  public static WitnessesFragment newInstance() {
    return new WitnessesFragment();
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    // Inflate the layout for this fragment
    View view = inflater.inflate(R.layout.witnesses_fragment, container, false);
    LaoDetailViewModel viewModel = LaoDetailActivity.obtainViewModel(requireActivity());

    FloatingActionButton fab = view.findViewById(R.id.add_witness_button);
    fab.setOnClickListener(v -> viewModel.openAddWitness());

    RecyclerView recyclerView = view.findViewById(R.id.witness_list);

    WitnessListAdapter adapter = new WitnessListAdapter(viewModel.getWitnesses().getValue());
    LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
    recyclerView.setLayoutManager(layoutManager);
    DividerItemDecoration itemDecoration =
        new DividerItemDecoration(getContext(), layoutManager.getOrientation());

    recyclerView.setLayoutManager(layoutManager);
    recyclerView.addItemDecoration(itemDecoration);
    recyclerView.setAdapter(adapter);

    viewModel.getWitnesses().observe(getViewLifecycleOwner(), adapter::replaceList);

    return view;
  }
}
