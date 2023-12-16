package com.github.dedis.popstellar.ui.lao.token;

import android.content.*;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.*;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.TokenFragmentBinding;
import com.github.dedis.popstellar.model.objects.RollCall;
import com.github.dedis.popstellar.model.objects.security.PoPToken;
import com.github.dedis.popstellar.model.qrcode.PopTokenData;
import com.github.dedis.popstellar.repository.RollCallRepository;
import com.github.dedis.popstellar.ui.home.LaoCreateFragment;
import com.github.dedis.popstellar.ui.lao.LaoActivity;
import com.github.dedis.popstellar.ui.lao.LaoViewModel;
import com.github.dedis.popstellar.utility.ActivityUtils;
import com.github.dedis.popstellar.utility.Constants;
import com.github.dedis.popstellar.utility.error.ErrorUtils;
import com.github.dedis.popstellar.utility.error.UnknownRollCallException;
import com.github.dedis.popstellar.utility.error.keys.KeyException;
import com.github.dedis.popstellar.utility.security.KeyManager;
import com.google.gson.Gson;

import net.glxn.qrgen.android.QRCode;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import timber.log.Timber;

@AndroidEntryPoint
public class TokenFragment extends Fragment {

  private static final String TAG = TokenFragment.class.getSimpleName();

  @Inject Gson gson;
  @Inject RollCallRepository rollCallRepo;
  @Inject KeyManager keyManager;

  private LaoViewModel laoViewModel;

  public TokenFragment() {
    // Required empty public constructor
  }

  public static TokenFragment newInstance(String rcId) {
    TokenFragment fragment = new TokenFragment();
    Bundle args = new Bundle();
    args.putString(Constants.ROLL_CALL_ID, rcId);
    fragment.setArguments(args);
    return fragment;
  }

  @Override
  public void onResume() {
    super.onResume();
    laoViewModel.setPageTitle(R.string.token);
    laoViewModel.setIsTab(false);
  }

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    TokenFragmentBinding binding = TokenFragmentBinding.inflate(inflater, container, false);
    laoViewModel = LaoActivity.obtainViewModel(requireActivity());

    try {
      RollCall rollCall =
          rollCallRepo.getRollCallWithPersistentId(
              laoViewModel.getLaoId(), requireArguments().getString(Constants.ROLL_CALL_ID));
      Timber.tag(TAG).d("token displayed from roll call: %s", rollCall);

      PoPToken poPToken = keyManager.getValidPoPToken(laoViewModel.getLaoId(), rollCall);
      PopTokenData data = new PopTokenData(poPToken.publicKey);
      Bitmap bitmap =
          QRCode.from(gson.toJson(data))
              .withSize(Constants.QR_SIDE, Constants.QR_SIDE)
              .withColor(ActivityUtils.getQRCodeColor(requireContext()), Color.TRANSPARENT)
              .bitmap();
      binding.tokenQrCode.setImageBitmap(bitmap);
      binding.tokenTextView.setText(poPToken.publicKey.getEncoded());

      binding.tokenCopyButton.setOnClickListener(
          v -> {
            copyTextToClipboard(poPToken.publicKey.getEncoded());
            Toast.makeText(requireContext(), R.string.successful_copy, Toast.LENGTH_SHORT).show();
          });

    } catch (UnknownRollCallException | KeyException e) {
      ErrorUtils.logAndShow(requireContext(), TAG, e, R.string.error_retrieve_own_token);
      LaoActivity.setCurrentFragment(
          getParentFragmentManager(), R.id.fragment_event_list, LaoCreateFragment::new);
      return null;
    }

    handleBackNav();
    return binding.getRoot();
  }

  private void copyTextToClipboard(String token) {
    ClipboardManager clipboard =
        (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
    ClipData clip = ClipData.newPlainText(token, token);
    clipboard.setPrimaryClip(clip);
  }

  private void handleBackNav() {
    LaoActivity.addBackNavigationCallback(
        requireActivity(),
        getViewLifecycleOwner(),
        ActivityUtils.buildBackButtonCallback(
            TAG, "token list", () -> TokenListFragment.openFragment(getParentFragmentManager())));
  }
}
