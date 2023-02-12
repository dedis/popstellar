package com.github.dedis.popstellar.ui.detail.witness;

import android.os.Bundle;
import android.view.*;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.*;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.ui.detail.LaoDetailActivity;
import com.github.dedis.popstellar.ui.detail.LaoDetailViewModel;
import com.github.dedis.popstellar.ui.qrcode.QrScannerFragment;
import com.github.dedis.popstellar.ui.qrcode.ScanningAction;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import static com.github.dedis.popstellar.ui.detail.LaoDetailActivity.setCurrentFragment;

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
    fab.setOnClickListener(v -> openAddWitness());

    RecyclerView recyclerView = view.findViewById(R.id.witness_list);

    WitnessListAdapter adapter = new WitnessListAdapter(viewModel.getWitnesses().getValue());
    LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
    recyclerView.setLayoutManager(layoutManager);
    DividerItemDecoration itemDecoration =
        new DividerItemDecoration(requireContext(), layoutManager.getOrientation());

    recyclerView.setLayoutManager(layoutManager);
    recyclerView.addItemDecoration(itemDecoration);
    recyclerView.setAdapter(adapter);

    viewModel.getWitnesses().observe(getViewLifecycleOwner(), adapter::replaceList);

    return view;
  }

  private void openAddWitness() {
    System.out.println("openwitness");
    setCurrentFragment(
        getParentFragmentManager(),
        R.id.fragment_qr_scanner,
        () -> QrScannerFragment.newInstance(ScanningAction.ADD_WITNESS));
  }
}
