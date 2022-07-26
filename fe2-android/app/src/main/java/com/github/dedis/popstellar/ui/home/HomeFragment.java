package com.github.dedis.popstellar.ui.home;

import android.os.Bundle;
import android.util.Log;
import android.view.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.*;

import com.github.dedis.popstellar.databinding.HomeFragmentBinding;

import java.util.ArrayList;

import dagger.hilt.android.AndroidEntryPoint;

/** Fragment used to display the Home UI */
@AndroidEntryPoint
public final class HomeFragment extends Fragment {

  public static final String TAG = HomeFragment.class.getSimpleName();

  private HomeFragmentBinding mHomeFragBinding;
  private HomeViewModel mHomeViewModel;
  private LAOListAdapter mListAdapter;

  public static HomeFragment newInstance() {
    return new HomeFragment();
  }

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    mHomeFragBinding = HomeFragmentBinding.inflate(inflater, container, false);

    mHomeViewModel = HomeActivity.obtainViewModel(requireActivity());

    mHomeFragBinding.setViewmodel(mHomeViewModel);
    mHomeFragBinding.setLifecycleOwner(getActivity());

    return mHomeFragBinding.getRoot();
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);

    setupListAdapter();
    setupListUpdates();
  }

  private void setupListUpdates() {
    mHomeViewModel
        .getLAOs()
        .observe(
            requireActivity(),
            laos -> {
              Log.d(TAG, "Got a list update");

              mListAdapter.replaceList(laos);

              if (!laos.isEmpty()) {
                mHomeFragBinding.welcomeScreen.setVisibility(View.GONE);
                mHomeFragBinding.listScreen.setVisibility(View.VISIBLE);
                mHomeFragBinding.homeTitle.setVisibility(View.VISIBLE);
              }
            });
  }

  private void setupListAdapter() {
    RecyclerView recyclerView = mHomeFragBinding.laoList;

    mListAdapter = new LAOListAdapter(new ArrayList<>(0), mHomeViewModel, true);

    LinearLayoutManager mLayoutManager = new LinearLayoutManager(getContext());
    recyclerView.setLayoutManager(mLayoutManager);

    DividerItemDecoration dividerItemDecoration =
        new DividerItemDecoration(getContext(), mLayoutManager.getOrientation());
    recyclerView.addItemDecoration(dividerItemDecoration);

    recyclerView.setAdapter(mListAdapter);
  }
}
