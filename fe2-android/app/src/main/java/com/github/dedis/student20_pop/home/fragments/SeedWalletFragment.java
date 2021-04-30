package com.github.dedis.student20_pop.home.fragments;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import com.github.dedis.student20_pop.databinding.FragmentSeedWalletBinding;
import com.github.dedis.student20_pop.home.HomeActivity;
import com.github.dedis.student20_pop.home.HomeViewModel;
import com.github.dedis.student20_pop.model.Wallet;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.StringJoiner;
import javax.crypto.ShortBufferException;

public class SeedWalletFragment extends Fragment {
  public static final String TAG = SeedWalletFragment.class.getSimpleName();
  private FragmentSeedWalletBinding mSeedWalletFragBinding;
  private HomeViewModel mHomeViewModel;
  private Wallet wallet;
  public static SeedWalletFragment newInstance() {
    return new SeedWalletFragment();
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    wallet = Wallet.getInstance();

    mSeedWalletFragBinding = FragmentSeedWalletBinding.inflate(inflater, container, false);

    FragmentActivity activity = getActivity();
    if (activity instanceof HomeActivity) {
      mHomeViewModel = HomeActivity.obtainViewModel(activity);
    } else {
      throw new IllegalArgumentException("Cannot obtain view model for " + TAG);
    }

    mSeedWalletFragBinding.setViewModel(mHomeViewModel);
    mSeedWalletFragBinding.setLifecycleOwner(activity);

    return mSeedWalletFragBinding.getRoot();
  }
  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    setupDisplaySeed();
    setupConfirmSeedButton();
  }

  private void setupDisplaySeed(){
    String[] exp_str = wallet.ExportSeed();
    StringJoiner joiner = new StringJoiner(" ");
    for(String i: exp_str) joiner.add(i);
    mSeedWalletFragBinding.seedWallet.setText(joiner.toString());
  }

  private void setupConfirmSeedButton() {
    mSeedWalletFragBinding.buttonConfirmSeed.setOnClickListener(v -> {
      AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
      builder.setTitle("You are sure you have saved the words somewhere?");

      builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          String errorMessage = "Error import key, try again:  ";
          try {
            String seed = mSeedWalletFragBinding.seedWallet.getText().toString();
            if(wallet.ImportSeed(seed, new HashMap<>()) == null){
              Toast.makeText(getContext().getApplicationContext(), errorMessage, Toast.LENGTH_LONG).show();
            } else {
              mHomeViewModel.openWallet(true);
            }
          } catch (NoSuchAlgorithmException e) {
            Toast.makeText(getContext().getApplicationContext(), errorMessage + e.getMessage(), Toast.LENGTH_LONG).show();
          } catch (InvalidKeyException e) {
            Toast.makeText(getContext().getApplicationContext(), errorMessage + e.getMessage(), Toast.LENGTH_LONG).show();
          } catch (ShortBufferException e) {
            Toast.makeText(getContext().getApplicationContext(), errorMessage + e.getMessage(), Toast.LENGTH_LONG).show();
          }
        }
      });
      builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          dialog.cancel();
        }
      });
      builder.show();
    });
  }
}