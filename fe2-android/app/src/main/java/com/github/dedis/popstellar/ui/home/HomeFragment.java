package com.github.dedis.popstellar.ui.home;

import android.os.Bundle;
import android.view.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.*;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.HomeFragmentBinding;
import com.github.dedis.popstellar.ui.qrcode.*;
import com.github.dedis.popstellar.utility.ActivityUtils;

import dagger.hilt.android.AndroidEntryPoint;
import timber.log.Timber;

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
    binding.setLifecycleOwner(getActivity());
    viewModel = HomeActivity.obtainViewModel(requireActivity());

    setupListAdapter();
    setupListUpdates();
    setupButtonsActions();

    handleBackNav();
    return binding.getRoot();
  }

  @Override
  public void onResume() {
    super.onResume();
    viewModel.setPageTitle(R.string.home_title);
    viewModel.setIsHome(true);
  }

  private void setupButtonsActions() {
    binding.homeCreateButton.setOnClickListener(
        v -> {
          Timber.tag(TAG).d("Opening Create fragment");
          HomeActivity.setCurrentFragment(
              getParentFragmentManager(), R.id.fragment_lao_create, LaoCreateFragment::newInstance);
        });

    binding.homeJoinButton.setOnClickListener(
        v -> {
          Timber.tag(TAG).d("Opening join fragment");
          HomeActivity.setCurrentFragment(
              getParentFragmentManager(),
              R.id.fragment_qr_scanner,
              () -> QrScannerFragment.newInstance(ScanningAction.ADD_LAO_PARTICIPANT));
          viewModel.setIsHome(false);
        });

    binding.homeQrButton.setOnClickListener(
        v -> {
          Timber.tag(TAG).d("Opening QR fragment");
          HomeActivity.setCurrentFragment(
              getParentFragmentManager(), R.id.fragment_qr, QrFragment::newInstance);
        });
  }

  private void setupListUpdates() {
    viewModel
        .getLaoIdList()
        .observe(
            requireActivity(),
            laoIds -> {
              Timber.tag(TAG).d("Got a list update");
              laoListAdapter.setList(laoIds);

              if (!laoIds.isEmpty()) {
                binding.homeNoLaoText.setVisibility(View.GONE);
                binding.laoList.setVisibility(View.VISIBLE);
              }
            });
  }

  private void setupListAdapter() {
    RecyclerView recyclerView = binding.laoList;

    laoListAdapter = new LAOListAdapter(viewModel, requireActivity());

    LinearLayoutManager mLayoutManager = new LinearLayoutManager(getContext());
    recyclerView.setLayoutManager(mLayoutManager);

    DividerItemDecoration dividerItemDecoration =
        new DividerItemDecoration(requireContext(), mLayoutManager.getOrientation());
    recyclerView.addItemDecoration(dividerItemDecoration);

    recyclerView.setAdapter(laoListAdapter);
  }

  private void handleBackNav() {
    HomeActivity.addBackNavigationCallback(
        requireActivity(),
        getViewLifecycleOwner(),
        ActivityUtils.buildBackButtonCallback(
            TAG, "put the app in background", () -> requireActivity().moveTaskToBack(true)));
  }
}
