package com.github.dedis.popstellar.ui.detail;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.model.qrcode.MainPublicKeyData;
import com.google.gson.Gson;

import net.glxn.qrgen.android.QRCode;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Represents the identity of a user within an organization (which allows users to “wear different
 * hats” in different organizations) TODO : For the moment, the goal of this UI is just to show a QR
 * code, but in the future, it will be needed to store identity information somewhere to make it
 * dependent of the current user and LAO
 */
@AndroidEntryPoint
public class IdentityFragment extends Fragment {

  public static final String TAG = IdentityFragment.class.getSimpleName();

  @Inject Gson gson;

  public static final String PUBLIC_KEY = "public key";
  private EditText identityNameEditText;
  private EditText identityTitleEditText;
  private EditText identityOrganizationEditText;
  private EditText identityEmailEditText;
  private EditText identityPhoneEditText;
  private ImageView qrCode;

  public static IdentityFragment newInstance(PublicKey publicKey) {
    IdentityFragment identityFragment = new IdentityFragment();
    Bundle bundle = new Bundle(1);
    bundle.putString(PUBLIC_KEY, publicKey.getEncoded());
    identityFragment.setArguments(bundle);
    return identityFragment;
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.identity_fragment, container, false);
    // TODO :   The QR code does not appear at all unless the Name field is non-empty
    //  and not all whitespace.
    CheckBox anonymousCheckBox = view.findViewById(R.id.checkbox_anonymous);
    qrCode = view.findViewById(R.id.qr_code);
    identityEmailEditText = view.findViewById(R.id.identity_email);
    identityNameEditText = view.findViewById(R.id.identity_name);
    identityOrganizationEditText = view.findViewById(R.id.identity_organization);
    identityPhoneEditText = view.findViewById(R.id.identity_phone);
    identityTitleEditText = view.findViewById(R.id.identity_title);

    hideIdentityInformation();

    anonymousCheckBox.setOnCheckedChangeListener(
        (buttonView, isChecked) -> {
          if (isChecked) {
            hideIdentityInformation();
          } else {
            qrCode.setVisibility(View.VISIBLE);
            identityEmailEditText.setVisibility(View.VISIBLE);
            identityNameEditText.setVisibility(View.VISIBLE);
            identityOrganizationEditText.setVisibility(View.VISIBLE);
            identityPhoneEditText.setVisibility(View.VISIBLE);
            identityTitleEditText.setVisibility(View.VISIBLE);
          }
        });

    // for now we use the user's public key to generate the QR code
    // TODO: In the future use Wallet with user's token
    String pk = this.requireArguments().getString(PUBLIC_KEY);
    identityNameEditText.setText(pk);

    MainPublicKeyData data = new MainPublicKeyData(new PublicKey(pk));
    Bitmap myBitmap = QRCode.from(gson.toJson(data)).bitmap();
    qrCode.setImageBitmap(myBitmap);

    return view;
  }

  @Override
  public void onResume() {
    super.onResume();
    LaoDetailViewModel viewModel = LaoDetailActivity.obtainViewModel(requireActivity());
    viewModel.setPageTitle(R.string.tab_identity);
  }

  /** Hide fields when user wants to be anonymous */
  private void hideIdentityInformation() {
    qrCode.setVisibility(View.GONE);
    identityEmailEditText.setVisibility(View.GONE);
    identityNameEditText.setVisibility(View.GONE);
    identityOrganizationEditText.setVisibility(View.GONE);
    identityPhoneEditText.setVisibility(View.GONE);
    identityTitleEditText.setVisibility(View.GONE);
  }
}
