package com.github.dedis.popstellar.ui.lao.digitalcash;

import android.os.Bundle;
import android.util.Log;
import android.view.*;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.*;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.ui.lao.LaoActivity;
import com.github.dedis.popstellar.ui.lao.LaoViewModel;
import com.github.dedis.popstellar.utility.ActivityUtils;

import io.reactivex.android.schedulers.AndroidSchedulers;

public class DigitalCashHistoryFragment extends Fragment {
  private static final String TAG = DigitalCashHistoryFragment.class.getSimpleName();

  private LaoViewModel laoViewModel;

  public static DigitalCashHistoryFragment newInstance() {
    return new DigitalCashHistoryFragment();
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    // Inflate the layout for this fragment
    View view = inflater.inflate(R.layout.digital_cash_history_fragment, container, false);
    laoViewModel = LaoActivity.obtainViewModel(requireActivity());
    DigitalCashViewModel digitalCashViewModel =
        LaoActivity.obtainDigitalCashViewModel(getActivity(), laoViewModel.getLaoId());

    RecyclerView transactionList = view.findViewById(R.id.transaction_history_list);
    RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
    RecyclerView.ItemDecoration decoration =
        new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL);

    HistoryListAdapter adapter = new HistoryListAdapter(digitalCashViewModel, requireActivity());

    transactionList.setLayoutManager(layoutManager);
    transactionList.addItemDecoration(decoration);
    transactionList.setAdapter(adapter);

    // Update dynamically the events in History
    laoViewModel.addDisposable(
        digitalCashViewModel
            .getTransactionsObservable()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                adapter::setList, error -> Log.d(TAG, "error with history update " + error)));

    handleBackNav();
    return view;
  }

  @Override
  public void onResume() {
    super.onResume();
    laoViewModel.setPageTitle(R.string.digital_cash_history);
    laoViewModel.setIsTab(false);
  }

  private void handleBackNav() {
    LaoActivity.addBackNavigationCallback(
        requireActivity(),
        getViewLifecycleOwner(),
        ActivityUtils.buildBackButtonCallback(
            TAG,
            "digital cash home",
            () -> DigitalCashHomeFragment.openFragment(getParentFragmentManager())));
  }
}
