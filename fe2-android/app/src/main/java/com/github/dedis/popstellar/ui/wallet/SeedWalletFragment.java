package com.github.dedis.popstellar.ui.wallet;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.*;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.WalletSeedFragmentBinding;
import com.github.dedis.popstellar.model.objects.Wallet;
import com.github.dedis.popstellar.ui.home.*;
import com.github.dedis.popstellar.utility.error.ErrorUtils;
import com.github.dedis.popstellar.utility.error.keys.SeedValidationException;

import java.security.GeneralSecurityException;
import java.util.Objects;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/** Fragment used to display the new seed UI */
@AndroidEntryPoint
public class SeedWalletFragment extends Fragment {

  public static final String TAG = SeedWalletFragment.class.getSimpleName();
  private WalletSeedFragmentBinding binding;
  private HomeViewModel viewModel;

  @Inject Wallet wallet;

  public static SeedWalletFragment newInstance() {
    return new SeedWalletFragment();
  }

  private AlertDialog seedAlert;

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {

    binding = WalletSeedFragmentBinding.inflate(inflater, container, false);
    HomeActivity activity = (HomeActivity) getActivity();
    viewModel = HomeActivity.obtainViewModel(activity);
    binding.setLifecycleOwner(activity);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    binding.seedWalletText.setText(wallet.newSeed());
    setupConfirmSeedButton();
    setupImportPart();
  }

  private void setupConfirmSeedButton() {
    binding.buttonConfirmSeed.setOnClickListener(
        v -> {
          if (seedAlert != null && seedAlert.isShowing()) {
            seedAlert.dismiss();
          }
          AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
          builder.setTitle("You are sure you have saved the words somewhere?");
          builder.setPositiveButton(
              "Yes",
              (dialog, which) -> {
                try {
                  viewModel.importSeed(binding.seedWalletText.getText().toString());
                  HomeActivity.setCurrentFragment(
                      getParentFragmentManager(), R.id.fragment_home, HomeFragment::newInstance);
                  viewModel.setPageTitle(R.string.home_title);

                } catch (GeneralSecurityException | SeedValidationException e) {
                  Log.e(TAG, "Error importing key", e);
                  Toast.makeText(
                          requireContext().getApplicationContext(),
                          "Error importing key : " + e.getMessage() + "\ntry again",
                          Toast.LENGTH_LONG)
                      .show();
                }
              });
          builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
          seedAlert = builder.create();
          seedAlert.show();
        });
  }

  private void setupImportPart() {
    TextWatcher importSeedWatcher =
        new TextWatcher() {
          @Override
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // Do nothing
          }

          @Override
          public void onTextChanged(CharSequence s, int start, int before, int count) {
            binding.importSeedButton.setEnabled(!s.toString().isEmpty());
          }

          @Override
          public void afterTextChanged(Editable s) {
            // Do nothing
          }
        };

    binding.importSeedEntryEditText.addTextChangedListener(importSeedWatcher);

    binding.importSeedButton.setOnClickListener(
        v -> {
          try {
            viewModel.importSeed(
                Objects.requireNonNull(binding.importSeedEntryEditText.getText()).toString());
          } catch (GeneralSecurityException | SeedValidationException e) {
            ErrorUtils.logAndShow(requireContext(), TAG, e, R.string.seed_validation_exception);
            return;
          }
          Toast.makeText(requireContext(), R.string.seed_import_success, Toast.LENGTH_SHORT).show();
          HomeActivity.setCurrentFragment(
              getParentFragmentManager(), R.id.fragment_home, HomeFragment::new);
          viewModel.setPageTitle(R.string.home_title);
        });
  }
}
