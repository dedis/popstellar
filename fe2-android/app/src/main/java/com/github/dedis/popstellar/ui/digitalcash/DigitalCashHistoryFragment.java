package com.github.dedis.popstellar.ui.digitalcash;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.dedis.popstellar.R;

/**
 * A simple {@link Fragment} subclass. Use the {@link DigitalCashHistoryFragment#newInstance}
 * factory method to create an instance of this fragment.
 */
public class DigitalCashHistoryFragment extends Fragment {

  public DigitalCashHistoryFragment() {
    // not implemented yet
  }

  /**
   * Use this factory method to create a new instance of this fragment using the provided
   * parameters.
   *
   * @return A new instance of fragment DigitalCashHistoryFragment.
   */
  public static DigitalCashHistoryFragment newInstance() {
    return new DigitalCashHistoryFragment();
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    // Inflate the layout for this fragment
    View view = inflater.inflate(R.layout.digital_cash_history_fragment, container, false);
    DigitalCashViewModel viewModel = DigitalCashMain.obtainViewModel(getActivity());
    RecyclerView transactionList = view.findViewById(R.id.transaction_history_list);
    RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
    RecyclerView.ItemDecoration decoration =
        new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL);
    HistoryListAdapter adapter =
        new HistoryListAdapter(viewModel.getTransactionHistory().getValue(), viewModel);

    transactionList.setLayoutManager(layoutManager);
    transactionList.addItemDecoration(decoration);
    transactionList.setAdapter(adapter);

    // Update dynamically the events in History
    viewModel.getTransactionHistory().observe(getActivity(), adapter::replaceList);
    return view;
  }
}
