package com.github.dedis.student20_pop.home.fragments;




import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.github.dedis.student20_pop.R;
import com.github.dedis.student20_pop.home.HomeActivity;
import com.github.dedis.student20_pop.home.HomeViewModel;
import com.github.dedis.student20_pop.model.Wallet;
import com.github.dedis.student20_pop.utility.ActivityUtils;
import com.tinder.scarlet.Message.Text;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.StringJoiner;
import javax.crypto.ShortBufferException;
import net.i2p.crypto.eddsa.Utils;

/** Fragment used to display the Launch UI */
public class WalletFragment extends Fragment {
  public static final String TAG = WalletFragment.class.getSimpleName();


  public Wallet wallet;
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
    View view =  inflater.inflate(R.layout.fragment_wallet, container, false);
    return view;
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    setupOwnSeedButton(view);
    setupNewWalletButton(view);

  }

  private void setupOwnSeedButton(View view) {
    String defaultSeed = "elbow six card empty next sight turn quality capital please vocal indoor";
    Button ownSeedButton = (Button) view.findViewById(R.id.button_own_seed);
    ownSeedButton.setOnClickListener(v ->{
      AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
      builder.setTitle("Type the 12 word seed:");

      final EditText input = new EditText(getActivity());

      input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
      input.setText(defaultSeed);
      builder.setView(input);

      builder.setPositiveButton("Set up wallet", new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          String errorMessage = "Error import key, try again:  ";
          try {
            if(wallet.ImportSeed(input.getText().toString(), new HashMap<>()) == null){
              Toast.makeText(getContext().getApplicationContext(), errorMessage, Toast.LENGTH_LONG).show();
            } else {
              setupContentWalletFragment();
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
    } );
  }

  private void setupNewWalletButton(View view) {
    Button newWalletButton = (Button) view.findViewById(R.id.button_new_wallet);
    newWalletButton.setOnClickListener(v -> {
      AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
      builder.setTitle("This is the only backup for your PoP tokens - store it securely");

      final TextView input = new TextView(getActivity());

      input.setSingleLine(false);
      String[] exp_str = wallet.ExportSeed();
      StringJoiner joiner = new StringJoiner(" ");
      for(String i: exp_str) joiner.add(i);
      input.setText(joiner.toString());
      builder.setView(input);

      builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

              Toast.makeText(getContext().getApplicationContext(), "Successfully initialize a wallet ",
                  Toast.LENGTH_LONG).show();

            }
          });
      builder.show();
      });
  }

  private void setupContentWalletFragment() {
    ContentWalletFragment contentWalletFragment =
        (ContentWalletFragment) getActivity().getSupportFragmentManager().findFragmentById(R.id.fragment_home);
    if (contentWalletFragment == null) {
      contentWalletFragment = ContentWalletFragment.newInstance();
      ActivityUtils.replaceFragmentInActivity(
          getActivity().getSupportFragmentManager(), contentWalletFragment, R.id.fragment_container_home);
    }
  }
}




