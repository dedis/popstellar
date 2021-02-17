package com.github.dedis.student20_pop.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.dedis.student20_pop.databinding.FragmentHomeBinding;
import com.github.dedis.student20_pop.home.HomeActivity;
import com.github.dedis.student20_pop.home.HomeViewModel;
import com.github.dedis.student20_pop.home.LAOListAdapter;
import com.github.dedis.student20_pop.model.Lao;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Fragment used to display the Home UI */
public final class HomeFragment extends Fragment {

  public static final String TAG = HomeFragment.class.getSimpleName();
  public static final SimpleDateFormat DATE_FORMAT =
      new SimpleDateFormat("dd/MM/yy", Locale.ENGLISH);

  private List<Lao> laos;
  private String id;

  private FragmentHomeBinding mHomeFragBinding;

  private HomeViewModel mHomeViewModel;

  private LAOListAdapter mListAdapter;

  public HomeFragment() {

  }

  public static HomeFragment newInstance() {
    return new HomeFragment();
  }

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    mHomeFragBinding = FragmentHomeBinding.inflate(inflater, container, false);

    mHomeViewModel = HomeActivity.obtainViewModel(getActivity());

    mHomeFragBinding.setViewmodel(mHomeViewModel);
    mHomeFragBinding.setLifecycleOwner(getActivity());

    return mHomeFragBinding.getRoot();
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);

    ((HomeActivity) getActivity()).setupHomeButton();
    ((HomeActivity) getActivity()).setupConnectButton();
    ((HomeActivity) getActivity()).setupLaunchButton();

    setupListAdapter();
    setupListUpdates();

    mHomeViewModel.setupDummyLAO();
  }

  private void setupListUpdates() {
    mHomeViewModel.getLAOs().observe(getActivity(), laos -> {
      Log.d(TAG, "Got a list update");

      mListAdapter.replaceList(laos);

      if (laos.size() > 0) {
        mHomeFragBinding.welcomeScreen.setVisibility(View.GONE);
        mHomeFragBinding.listScreen.setVisibility(View.VISIBLE);
      }
    });
  }

  private void setupListAdapter() {
    ListView listView = mHomeFragBinding.laoList;

    mListAdapter = new LAOListAdapter(
            new ArrayList<Lao>(0),
            mHomeViewModel,
            getActivity()
    );

    listView.setAdapter(mListAdapter);
  }
}
