package com.github.dedis.popstellar.ui.wallet;

import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.*;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.*;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.WalletFragmentBinding;
import com.github.dedis.popstellar.ui.home.HomeActivity;
import com.github.dedis.popstellar.ui.home.HomeViewModel;
import com.github.dedis.popstellar.utility.error.ErrorUtils;
import com.github.dedis.popstellar.utility.error.keys.SeedValidationException;

import java.security.GeneralSecurityException;

import dagger.hilt.android.AndroidEntryPoint;

/** Fragment used to display the wallet UI */
@AndroidEntryPoint
public class WalletFragment extends Fragment {

  public static final String TAG = WalletFragment.class.getSimpleName();

  private WalletFragmentBinding mWalletFragBinding;
  private HomeViewModel mHomeViewModel;

  public static WalletFragment newInstance() {
    return new WalletFragment();
  }

  private AlertDialog seedAlert;

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    mWalletFragBinding = WalletFragmentBinding.inflate(inflater, container, false);

    FragmentActivity activity = getActivity();
    if (activity instanceof HomeActivity) {
      mHomeViewModel = HomeActivity.obtainViewModel(activity);
    } else {
      throw new IllegalArgumentException("Cannot obtain view model for " + TAG);
    }

    mWalletFragBinding.setViewModel(mHomeViewModel);
    mWalletFragBinding.setLifecycleOwner(activity);

    setupOwnSeedButton();
    setupNewWalletButton();

    return mWalletFragBinding.getRoot();
  }

  private void setupOwnSeedButton() {
    String defaultSeed = "elbow six card empty next sight turn quality capital please vocal indoor";
    mWalletFragBinding.buttonOwnSeed.setOnClickListener(
        v -> {
          if (seedAlert != null && seedAlert.isShowing()) {
            seedAlert.dismiss();
          }
          AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
          builder.setTitle("Type the 12 word seed:");

          final EditText input = new EditText(requireActivity());
          input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
          input.setText(
              defaultSeed); // for facilitate test we set a default seed for login in the Wallet
          builder.setView(input);

          final boolean[] checked = new boolean[] {false};
          builder.setMultiChoiceItems(
              new String[] {"show password"},
              checked,
              (dialogInterface, i, b) -> {
                checked[i] = b;
                if (b) {
                  input.setInputType(InputType.TYPE_CLASS_TEXT);
                } else {
                  input.setInputType(
                      InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                }
              });

          builder.setPositiveButton(
              "Set up wallet",
              (dialog, which) -> {
                try {
                  mHomeViewModel.importSeed(input.getText().toString());
                  openWallet(getParentFragmentManager(), mHomeViewModel.isWalletSetUp());
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

  private void setupNewWalletButton() {
    mWalletFragBinding.buttonNewWallet.setOnClickListener(
        v -> {
          try {
            mHomeViewModel.newSeed();
          } catch (GeneralSecurityException e) {
            ErrorUtils.logAndShow(getContext(), TAG, e, R.string.seed_generation_error);
          }
          HomeActivity.setCurrentFragment(
              getParentFragmentManager(), R.id.fragment_seed_wallet, SeedWalletFragment::new);
        });
  }

  public static void openWallet(FragmentManager manager, boolean isWalletSetup) {
    if (isWalletSetup) {
      HomeActivity.setCurrentFragment(
          manager, R.id.fragment_content_wallet, ContentWalletFragment::newInstance);
    } else {
      HomeActivity.setCurrentFragment(manager, R.id.fragment_wallet, WalletFragment::newInstance);
    }
  }
}
