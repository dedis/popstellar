package com.github.dedis.popstellar.ui.digitalcash;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.DigitalCashIssueFragmentBinding;
import com.github.dedis.popstellar.utility.ActivityUtils;

import java.util.ArrayList;
import java.util.function.Supplier;

/**
 * A simple {@link Fragment} subclass. Use the {@link DigitalCashIssueFragment#newInstance} factory
 * method to create an instance of this fragment.
 */
public class DigitalCashIssueFragment extends Fragment {

  private DigitalCashIssueFragmentBinding digitalCashIssueFragmentBinding;
  private DigitalCashViewModel digitalCashViewModel;
  private CoinListAdapter coinListAdapter;

  public DigitalCashIssueFragment() {
    // not implemented yet
  }

  /**
   * Use this factory method to create a new instance of this fragment using the provided
   * parameters.
   *
   * @return A new instance of fragment DigitalCashIssueFragment.
   */
  public static DigitalCashIssueFragment newInstance() {
    return new DigitalCashIssueFragment();
  }

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    digitalCashIssueFragmentBinding =
        DigitalCashIssueFragmentBinding.inflate(inflater, container, false);
    digitalCashViewModel = DigitalCashMain.obtainViewModel(requireActivity());
    digitalCashIssueFragmentBinding.setViewModel(digitalCashViewModel);
    digitalCashIssueFragmentBinding.setLifecycleOwner(getViewLifecycleOwner());

    return digitalCashIssueFragmentBinding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    setupListUpdate();
    setupListViewAdapter();
    setupSwipeRefresh();
  }

  private void setupSwipeRefresh() {
    SwipeRefreshLayout swipeRefreshLayout = digitalCashIssueFragmentBinding.swipeRefreshCoin;
    swipeRefreshLayout.setOnRefreshListener(
        () -> {
          coinListAdapter.replaceList(
              digitalCashViewModel.getAttendeeList(digitalCashViewModel.getLaoId().getValue()));
          final Handler handler = new Handler(Looper.getMainLooper());
          handler.postDelayed(
              () -> {
                if (swipeRefreshLayout.isRefreshing()) {
                  swipeRefreshLayout.setRefreshing(false);
                }
              },
              1000);
        });
  }

  private void setupListViewAdapter() {
    ListView listView = digitalCashIssueFragmentBinding.coinList;
    coinListAdapter = new CoinListAdapter(getActivity(), digitalCashViewModel, new ArrayList<>());
    listView.setAdapter(coinListAdapter);
  }

  private void setupListUpdate() {
    digitalCashViewModel
        .getLaoId()
        .observe(
            getViewLifecycleOwner(),
            newLaoId ->
                coinListAdapter.replaceList(digitalCashViewModel.getAttendeeList(newLaoId)));
  }

  private void setCurrentFragment(@IdRes int id, Supplier<Fragment> fragmentSupplier) {
    Fragment fragment = requireActivity().getSupportFragmentManager().findFragmentById(id);
    // If the fragment was not created yet, create it now
    if (fragment == null) fragment = fragmentSupplier.get();

    // Set the new fragment in the container
    ActivityUtils.replaceFragmentInActivity(
        requireActivity().getSupportFragmentManager(),
        fragment,
        R.id.fragment_container_digital_cash);
  }

}
