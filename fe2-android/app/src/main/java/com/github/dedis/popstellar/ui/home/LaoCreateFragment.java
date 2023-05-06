package com.github.dedis.popstellar.ui.home;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.LaoCreateFragmentBinding;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager;
import com.github.dedis.popstellar.ui.lao.witness.WitnessingViewModel;
import com.github.dedis.popstellar.ui.qrcode.QrScannerFragment;
import com.github.dedis.popstellar.ui.qrcode.ScanningAction;

import java.util.List;
import java.util.Objects;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import timber.log.Timber;

/** Fragment used to display the Launch UI */
@AndroidEntryPoint
public final class LaoCreateFragment extends Fragment {

  public static final String TAG = LaoCreateFragment.class.getSimpleName();

  @Inject GlobalNetworkManager networkManager;

  private HomeViewModel viewModel;
  private WitnessingViewModel witnessingViewModel;
  private LaoCreateFragmentBinding binding;
  private String initialUrl;

  public static LaoCreateFragment newInstance() {
    return new LaoCreateFragment();
  }

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {

    binding = LaoCreateFragmentBinding.inflate(inflater, container, false);
    binding.setLifecycleOwner(getActivity());
    initialUrl = networkManager.getCurrentUrl();
    viewModel = HomeActivity.obtainViewModel(requireActivity());
    witnessingViewModel = HomeActivity.obtainWitnessingViewModel(requireActivity());

    setupCancelButton();
    setupTextFields();
    setupAddWitnesses();
    setupCreateButton();

    handleBackNav();
    return binding.getRoot();
  }

  @Override
  public void onResume() {
    super.onResume();
    viewModel.setPageTitle(R.string.lao_create_title);
    viewModel.setIsHome(false);
  }

  TextWatcher launchWatcher =
      new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
          // Do nothing
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
          String laoName =
              Objects.requireNonNull(binding.laoNameEntry.getEditText())
                  .getText()
                  .toString()
                  .trim();
          String serverUrl =
              Objects.requireNonNull(binding.serverUrlEntry.getEditText())
                  .getText()
                  .toString()
                  .trim();

          boolean areFieldsFilled = !laoName.isEmpty() && !serverUrl.isEmpty();
          binding.buttonCreate.setEnabled(areFieldsFilled);
        }

        @Override
        public void afterTextChanged(Editable editable) {
          // Do nothing
        }
      };

  private void setupTextFields() {
    binding.serverUrlEntryEditText.setText(initialUrl);
    binding.serverUrlEntryEditText.addTextChangedListener(launchWatcher);
    binding.laoNameEntryEditText.addTextChangedListener(launchWatcher);
  }

  private void setupAddWitnesses() {
    binding.addWitnessButton.setOnClickListener(
        v -> {
          Timber.tag(TAG).d("Opening scanner fragment");
          HomeActivity.setCurrentFragment(
              getParentFragmentManager(),
              R.id.fragment_qr_scanner,
              () -> QrScannerFragment.newInstance(ScanningAction.ADD_WITNESS_AT_START));
        });

    WitnessesListAdapter witnessesListAdapter = new WitnessesListAdapter();

    LinearLayoutManager mLayoutManager = new LinearLayoutManager(getContext());
    binding.witnessesList.setLayoutManager(mLayoutManager);

    DividerItemDecoration dividerItemDecoration =
        new DividerItemDecoration(requireContext(), mLayoutManager.getOrientation());
    binding.witnessesList.addItemDecoration(dividerItemDecoration);

    binding.witnessesList.setAdapter(witnessesListAdapter);
    List<PublicKey> witnesses = witnessingViewModel.getScannedWitnesses();
    if (!witnesses.isEmpty()) {
      binding.witnessesTitle.setVisibility(View.VISIBLE);
    }
    witnessesListAdapter.setList(witnesses);
  }

  private void setupCreateButton() {
    binding.buttonCreate.setOnClickListener(
        v -> {
          String serverAddress =
              Objects.requireNonNull(binding.serverUrlEntryEditText.getText()).toString();
          String laoName =
              Objects.requireNonNull(binding.laoNameEntryEditText.getText()).toString();
          Timber.tag(TAG).d("creating lao with name %s", laoName);
          List<PublicKey> witnesses = witnessingViewModel.getScannedWitnesses();

          networkManager.connect(serverAddress);
          requireActivity()
              .startActivity(
                  ConnectingActivity.newIntentForCreatingDetail(
                      requireContext(), laoName, witnesses));
        });
  }

  private void setupCancelButton() {
    binding.buttonCancelLaunch.setOnClickListener(
        v -> {
          Objects.requireNonNull(binding.laoNameEntryEditText.getText()).clear();
          HomeActivity.setCurrentFragment(
              getParentFragmentManager(), R.id.fragment_home, HomeFragment::newInstance);
        });
  }

  private void handleBackNav() {
    HomeActivity.addBackNavigationCallbackToHome(requireActivity(), getViewLifecycleOwner(), TAG);
  }
}
