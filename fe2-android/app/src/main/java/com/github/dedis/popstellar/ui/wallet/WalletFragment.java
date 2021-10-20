package com.github.dedis.popstellar.ui.wallet;

import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.github.dedis.popstellar.databinding.WalletFragmentBinding;
import com.github.dedis.popstellar.model.objects.Wallet;
import com.github.dedis.popstellar.ui.home.HomeActivity;
import com.github.dedis.popstellar.ui.home.HomeViewModel;

import java.io.IOException;
import java.security.GeneralSecurityException;

/** Fragment used to display the wallet UI */
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
    try {
      Wallet.getInstance().initKeysManager(getContext().getApplicationContext());
    } catch (IOException | GeneralSecurityException e) {
      Toast.makeText(
              getContext().getApplicationContext(),
              "Error import key, try again",
              Toast.LENGTH_LONG)
          .show();
      Log.d(TAG, e.getMessage());
    }
    mWalletFragBinding.setViewModel(mHomeViewModel);
    mWalletFragBinding.setLifecycleOwner(activity);

    return mWalletFragBinding.getRoot();
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    setupOwnSeedButton();
    setupNewWalletButton();
  }

  private void setupOwnSeedButton() {
    String defaultSeed = "elbow six card empty next sight turn quality capital please vocal indoor";
    mWalletFragBinding.buttonOwnSeed.setOnClickListener(
        v -> {
          if (seedAlert != null && seedAlert.isShowing()) {
            seedAlert.dismiss();
          }
          AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
          builder.setTitle("Type the 12 word seed:");

          final EditText input = new EditText(getActivity());
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
                if (!mHomeViewModel.importSeed(input.getText().toString())) {
                  Toast.makeText(
                          getContext().getApplicationContext(),
                          "Error import key, try again",
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
    mWalletFragBinding.buttonNewWallet.setOnClickListener(v -> mHomeViewModel.openSeed());
  }
}
