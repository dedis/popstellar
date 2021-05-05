package com.github.dedis.student20_pop.home.fragments;


import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
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
import com.github.dedis.student20_pop.databinding.FragmentWalletBinding;
import com.github.dedis.student20_pop.home.HomeActivity;
import com.github.dedis.student20_pop.home.HomeViewModel;
import com.github.dedis.student20_pop.model.Wallet;
import java.util.HashMap;

/** Fragment used to display the Launch UI */
public class WalletFragment extends Fragment {
  public static final String TAG = WalletFragment.class.getSimpleName();

  private FragmentWalletBinding mWalletFragBinding;
  private HomeViewModel mHomeViewModel;
  private Wallet wallet;
  public static WalletFragment newInstance() {
    return new WalletFragment();
  }


  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    wallet = Wallet.getInstance();
    mWalletFragBinding = FragmentWalletBinding.inflate(inflater, container, false);

    FragmentActivity activity = getActivity();
    if (activity instanceof HomeActivity) {
      mHomeViewModel = HomeActivity.obtainViewModel(activity);
    } else {
      throw new IllegalArgumentException("Cannot obtain view model for " + TAG);
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
    mWalletFragBinding.buttonOwnSeed.setOnClickListener(v ->{
      AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
      builder.setTitle("Type the 12 word seed:");

      final EditText input = new EditText(getActivity());
      input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
      input.setText(defaultSeed); //for facilitate test we set a default seed for login in the Wallet
      builder.setView(input);

      final boolean[] checked = new boolean[] {false};
      builder.setMultiChoiceItems(new String[]{"show password"}, checked, (dialogInterface, i, b)-> {
        checked[i] = b;
        if(b == true) {
          input.setInputType(InputType.TYPE_CLASS_TEXT);
        }else{
          input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
      });

      builder.setPositiveButton("Set up wallet", (dialog,which) -> {
          String errorMessage = "Error import key, try again";
          try {
            if(wallet.importSeed(input.getText().toString(), new HashMap<>()) == null){
              Toast.makeText(getContext().getApplicationContext(), errorMessage, Toast.LENGTH_LONG).show();
            } else {
              mHomeViewModel.setIsWalletSetUp(true);
              mHomeViewModel.openWallet();
            }
          } catch (IllegalArgumentException e) {
            Toast.makeText(getContext().getApplicationContext(),
                errorMessage +" : "+ e.getMessage(), Toast.LENGTH_LONG).show();
          }
        }
      );
      builder.setNegativeButton("Cancel",  (dialog,which) -> dialog.cancel());
      builder.show();
    } );
  }

  private void setupNewWalletButton() {
    mWalletFragBinding.buttonNewWallet.setOnClickListener(v -> mHomeViewModel.openSeed());
  }
}




