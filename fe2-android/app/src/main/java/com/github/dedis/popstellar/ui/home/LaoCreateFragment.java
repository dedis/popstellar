package com.github.dedis.popstellar.ui.home;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.ArrayAdapter;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.LaoCreateFragmentBinding;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager;
import com.github.dedis.popstellar.ui.lao.witness.WitnessingViewModel;
import com.github.dedis.popstellar.ui.qrcode.QrScannerFragment;
import com.github.dedis.popstellar.ui.qrcode.ScanningAction;
import dagger.hilt.android.AndroidEntryPoint;
import java.util.*;
import java.util.stream.Collectors;
import javax.inject.Inject;
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

    setupClearButton();
    setupTextFields();
    setupAddWitnesses();
    setupCreateButton();
    setupWitnessingSwitch();

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

    // No need to have a LiveData as the fragment is recreated upon exiting the scanner
    List<String> witnesses =
        witnessingViewModel.getScannedWitnesses().stream()
            .map(PublicKey::getEncoded)
            .collect(Collectors.toList());

    ArrayAdapter<String> witnessesListAdapter =
        new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, witnesses);
    binding.witnessesList.setAdapter(witnessesListAdapter);
  }

  private void setupCreateButton() {
    binding.buttonCreate.setOnClickListener(
        v -> {
          String serverAddress =
              Objects.requireNonNull(binding.serverUrlEntryEditText.getText()).toString();
          String laoName =
              Objects.requireNonNull(binding.laoNameEntryEditText.getText()).toString();
          boolean isWitnessingEnabled =
              Boolean.TRUE.equals(viewModel.isWitnessingEnabled().getValue());
          Timber.tag(TAG).d("creating lao with name %s", laoName);
          List<PublicKey> witnesses = witnessingViewModel.getScannedWitnesses();

          networkManager.connect(serverAddress);
          requireActivity()
              .startActivity(
                  ConnectingActivity.newIntentForCreatingDetail(
                      requireContext(), laoName, witnesses, isWitnessingEnabled));
        });
  }

  private void setupClearButton() {
    binding.buttonClearLaunch.setOnClickListener(
        v -> {
          Objects.requireNonNull(binding.laoNameEntryEditText.getText()).clear();
          Objects.requireNonNull(binding.serverUrlEntryEditText.getText()).clear();
          binding.enableWitnessingSwitch.setChecked(false);
          witnessingViewModel.setWitnesses(Collections.emptyList());
        });
  }

  private void setupWitnessingSwitch() {
    binding.enableWitnessingSwitch.setOnCheckedChangeListener(
        (button, isChecked) -> {
          viewModel.setIsWitnessingEnabled(isChecked);
          if (isChecked) {
            binding.addWitnessButton.setVisibility(View.VISIBLE);
            if (!witnessingViewModel.getScannedWitnesses().isEmpty()) {
              binding.witnessesTitle.setVisibility(View.VISIBLE);
              binding.witnessesList.setVisibility(View.VISIBLE);
            }
          } else {
            binding.addWitnessButton.setVisibility(View.GONE);
            binding.witnessesTitle.setVisibility(View.GONE);
            binding.witnessesList.setVisibility(View.GONE);
          }
        });
    // Use this to save the preference after opening and closing the QR code
    binding.enableWitnessingSwitch.setChecked(
        Boolean.TRUE.equals(viewModel.isWitnessingEnabled().getValue()));
  }

  private void handleBackNav() {
    HomeActivity.addBackNavigationCallbackToHome(requireActivity(), getViewLifecycleOwner(), TAG);
  }
}
