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

  private HomeFragmentBinding binding;
  private HomeViewModel viewModel;
  private LAOListAdapter laoListAdapter;

  public static HomeFragment newInstance() {
    return new HomeFragment();
  }

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    binding = HomeFragmentBinding.inflate(inflater, container, false);

    viewModel = HomeActivity.obtainViewModel(requireActivity());

    binding.setViewmodel(viewModel);
    binding.setLifecycleOwner(getActivity());

    return binding.getRoot();
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);

    setupListAdapter();
    setupListUpdates();
  }

  private void setupListUpdates() {
    viewModel
        .getLAOs()
        .observe(
            requireActivity(),
            laos -> {
              Log.d(TAG, "Got a list update");

              laoListAdapter.setList(laos);

              if (!laos.isEmpty()) {
                binding.welcomeScreen.setVisibility(View.GONE);
                binding.listScreen.setVisibility(View.VISIBLE);
                binding.homeTitle.setVisibility(View.VISIBLE);
              }
            });
  }

  private void setupListAdapter() {
    RecyclerView recyclerView = binding.laoList;

    laoListAdapter = new LAOListAdapter(new ArrayList<>(0), requireActivity(), true);

    LinearLayoutManager mLayoutManager = new LinearLayoutManager(getContext());
    recyclerView.setLayoutManager(mLayoutManager);

    DividerItemDecoration dividerItemDecoration =
        new DividerItemDecoration(requireContext(), mLayoutManager.getOrientation());
    recyclerView.addItemDecoration(dividerItemDecoration);

    recyclerView.setAdapter(laoListAdapter);
  }
}
