package com.github.dedis.popstellar.ui.digitalcash;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.dedis.popstellar.R;

public class DigitalCashHistoryFragment extends Fragment {
  private static final String TAG = DigitalCashHistoryFragment.class.getSimpleName();

  public static DigitalCashHistoryFragment newInstance() {
    return new DigitalCashHistoryFragment();
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    // Inflate the layout for this fragment
    View view = inflater.inflate(R.layout.digital_cash_history_fragment, container, false);
    DigitalCashViewModel viewModel = DigitalCashActivity.obtainViewModel(getActivity());
    RecyclerView transactionList = view.findViewById(R.id.transaction_history_list);
    RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
    RecyclerView.ItemDecoration decoration =
        new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL);
    HistoryListAdapter adapter =
        new HistoryListAdapter(
            viewModel.getTransactionHistory().getValue(), viewModel.getTokens().getValue());

    transactionList.setLayoutManager(layoutManager);
    transactionList.addItemDecoration(decoration);
    transactionList.setAdapter(adapter);

    // Update dynamically the events in History
    viewModel
        .getTransactionHistory()
        .observe(
            getActivity(),
            transactionObjects -> {
              if (transactionObjects != null) {
                transactionObjects.forEach(object -> Log.d(TAG, "Object is " + object.toString()));
                Log.d(TAG, "Transaction got updated " + transactionObjects.size());
                adapter.replaceList(transactionObjects, viewModel.getTokens().getValue());
              }
            });
    return view;
  }
}
