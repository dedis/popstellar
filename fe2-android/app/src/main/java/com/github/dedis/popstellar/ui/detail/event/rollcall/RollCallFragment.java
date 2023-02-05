package com.github.dedis.popstellar.ui.detail.event.rollcall;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.widget.ImageViewCompat;
import androidx.fragment.app.Fragment;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.RollCallFragmentBinding;
import com.github.dedis.popstellar.model.objects.RollCall;
import com.github.dedis.popstellar.model.objects.event.EventState;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.model.qrcode.PopTokenData;
import com.github.dedis.popstellar.ui.detail.LaoDetailActivity;
import com.github.dedis.popstellar.ui.detail.LaoDetailViewModel;
import com.github.dedis.popstellar.ui.detail.event.eventlist.EventListFragment;
import com.github.dedis.popstellar.ui.lao.LaoActivity;
import com.github.dedis.popstellar.ui.qrcode.QRCodeScanningFragment;
import com.github.dedis.popstellar.utility.Constants;
import com.github.dedis.popstellar.utility.error.ErrorUtils;
import com.github.dedis.popstellar.utility.error.UnknownRollCallException;
import com.google.gson.Gson;

import net.glxn.qrgen.android.QRCode;

import java.text.SimpleDateFormat;
import java.util.*;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

import static com.github.dedis.popstellar.utility.Constants.ID_NULL;
import static com.github.dedis.popstellar.utility.Constants.ROLL_CALL_ID;

@AndroidEntryPoint
public class RollCallFragment extends Fragment {

  public static final String TAG = RollCallFragment.class.getSimpleName();

  @Inject Gson gson;

  private final SimpleDateFormat dateFormat =
      new SimpleDateFormat("dd/MM/yyyy HH:mm z", Locale.ENGLISH);

  private RollCallFragmentBinding binding;

  private LaoDetailViewModel viewModel;
  private RollCall rollCall;

  private final EnumMap<EventState, Integer> managementTextMap = buildManagementTextMap();
  private final EnumMap<EventState, Integer> statusTextMap = buildStatusTextMap();
  private final EnumMap<EventState, Integer> statusIconMap = buildStatusIconMap();
  private final EnumMap<EventState, Integer> managementIconMap = buildManagementIconMap();
  private final EnumMap<EventState, Integer> statusColorMap = buildStatusColorMap();

  public RollCallFragment() {
    // Required empty public constructor
  }

  public static RollCallFragment newInstance(PublicKey pk, String persistentId) {
    RollCallFragment fragment = new RollCallFragment();
    Bundle bundle = new Bundle(1);
    bundle.putString(Constants.RC_PK_EXTRA, pk.getEncoded());
    bundle.putString(Constants.ROLL_CALL_ID, persistentId);
    fragment.setArguments(bundle);
    return fragment;
  }

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    // Inflate the layout for this fragment
    binding = RollCallFragmentBinding.inflate(inflater, container, false);
    viewModel = LaoDetailActivity.obtainViewModel(requireActivity());
    try {
      rollCall = viewModel.getRollCall(requireArguments().getString(ROLL_CALL_ID));
      viewModel.setCurrentRollCallId(rollCall.getPersistentId());
    } catch (UnknownRollCallException e) {
      ErrorUtils.logAndShow(requireContext(), TAG, e, R.string.unknown_roll_call_exception);
      return null;
    }

    setUpStateDependantContent();

    binding.rollCallManagementButton.setOnClickListener(
        v -> {
          EventState state = rollCall.getState();
          switch (state) {
            case CLOSED:
            case CREATED:
              viewModel.addDisposable(
                  viewModel
                      .openRollCall(rollCall.getId())
                      .subscribe(
                          () ->
                              LaoActivity.setCurrentFragment(
                                  getParentFragmentManager(),
                                  R.id.fragment_qrcode,
                                  QRCodeScanningFragment::new),
                          error ->
                              ErrorUtils.logAndShow(
                                  requireContext(), TAG, error, R.string.error_open_rollcall)));
              break;
            case OPENED:
              // will add the scan to this fragment in the future
              viewModel.addDisposable(
                  viewModel
                      .closeRollCall()
                      .subscribe(
                          () ->
                              LaoActivity.setCurrentFragment(
                                  getParentFragmentManager(),
                                  R.id.fragment_event_list,
                                  EventListFragment::newInstance),
                          error ->
                              ErrorUtils.logAndShow(
                                  requireContext(), TAG, error, R.string.error_close_rollcall)));
              break;
            default:
              throw new IllegalStateException("Roll-Call should not be in a " + state + " state");
          }
        });

    binding.rollCallScanningButton.setOnClickListener(
        b ->
            LaoActivity.setCurrentFragment(
                getParentFragmentManager(), R.id.fragment_qrcode, QRCodeScanningFragment::new));

    viewModel.addDisposable(
        viewModel
            .getRollCallObservable(rollCall.getPersistentId())
            .subscribe(
                rc -> {
                  Log.d(TAG, "Received rc update: " + rc);
                  rollCall = rc;
                  setUpStateDependantContent();
                },
                error ->
                    ErrorUtils.logAndShow(
                        requireContext(), TAG, error, R.string.unknown_roll_call_exception)));

    retrieveAndDisplayPublicKey();

    return binding.getRoot();
  }

  @Override
  public void onResume() {
    super.onResume();
    viewModel.setPageTitle(R.string.roll_call_title);
    viewModel.setIsTab(false);
    try {
      rollCall = viewModel.getRollCall(requireArguments().getString(ROLL_CALL_ID));
    } catch (UnknownRollCallException e) {
      ErrorUtils.logAndShow(requireContext(), TAG, e, R.string.unknown_roll_call_exception);
    }
  }

  private void setUpStateDependantContent() {
    setupTime(); // Suggested time is updated in case of early/late close/open/reopen

    EventState rcState = rollCall.getState();
    boolean isOrganizer = viewModel.isOrganizer();

    binding.rollCallFragmentTitle.setText(rollCall.getName());
    binding.rollCallManagementButton.setVisibility(isOrganizer ? View.VISIBLE : View.GONE);

    binding.rollCallManagementButton.setText(managementTextMap.getOrDefault(rcState, ID_NULL));

    Drawable imgManagement =
        AppCompatResources.getDrawable(
            requireContext(), managementIconMap.getOrDefault(rcState, ID_NULL));
    binding.rollCallManagementButton.setCompoundDrawablesWithIntrinsicBounds(
        imgManagement, null, null, null);

    Drawable imgStatus = getDrawableFromContext(statusIconMap.getOrDefault(rcState, ID_NULL));
    binding.rollCallStatusIcon.setImageDrawable(imgStatus);
    setImageColor(binding.rollCallStatusIcon, statusColorMap.getOrDefault(rcState, ID_NULL));

    binding.rollCallStatus.setText(statusTextMap.getOrDefault(rcState, ID_NULL));
    binding.rollCallStatus.setTextColor(
        getResources().getColor(statusColorMap.getOrDefault(rcState, ID_NULL), null));

    // Show scanning button only if the current state is Opened
    if (rcState == EventState.OPENED && isOrganizer) {
      binding.rollCallScanningButton.setVisibility(View.VISIBLE);
    } else {
      binding.rollCallScanningButton.setVisibility(View.GONE);
    }
  }

  private void setupTime() {
    if (rollCall == null) {
      return;
    }
    Date startTime = new Date(rollCall.getStartTimestampInMillis());
    Date endTime = new Date(rollCall.getEndTimestampInMillis());

    binding.rollCallStartTime.setText(dateFormat.format(startTime));
    binding.rollCallEndTime.setText(dateFormat.format(endTime));
  }

  private Drawable getDrawableFromContext(int id) {
    return AppCompatResources.getDrawable(requireContext(), id);
  }

  private void setImageColor(ImageView imageView, int colorId) {
    ImageViewCompat.setImageTintList(imageView, getResources().getColorStateList(colorId, null));
  }

  private void retrieveAndDisplayPublicKey() {
    String pk = requireArguments().getString(Constants.RC_PK_EXTRA);
    Log.d(TAG, "key displayed is " + pk);

    PopTokenData data = new PopTokenData(new PublicKey(pk));
    Bitmap myBitmap = QRCode.from(gson.toJson(data)).bitmap();
    binding.rollCallPkQrCode.setImageBitmap(myBitmap);
    binding.rollCallPkQrCode.setVisibility(viewModel.isOrganizer() ? View.INVISIBLE : View.VISIBLE);
  }

  private EnumMap<EventState, Integer> buildManagementTextMap() {
    EnumMap<EventState, Integer> map = new EnumMap<>(EventState.class);
    map.put(EventState.CREATED, R.string.open);
    map.put(EventState.OPENED, R.string.close);
    map.put(EventState.CLOSED, R.string.reopen_rollcall);
    return map;
  }

  private EnumMap<EventState, Integer> buildStatusTextMap() {
    EnumMap<EventState, Integer> map = new EnumMap<>(EventState.class);
    map.put(EventState.CREATED, R.string.closed);
    map.put(EventState.OPENED, R.string.open);
    map.put(EventState.CLOSED, R.string.closed);
    return map;
  }

  private EnumMap<EventState, Integer> buildStatusIconMap() {
    EnumMap<EventState, Integer> map = new EnumMap<>(EventState.class);
    map.put(EventState.CREATED, R.drawable.ic_lock);
    map.put(EventState.OPENED, R.drawable.ic_unlock);
    map.put(EventState.CLOSED, R.drawable.ic_lock);
    return map;
  }

  private EnumMap<EventState, Integer> buildStatusColorMap() {
    EnumMap<EventState, Integer> map = new EnumMap<>(EventState.class);
    map.put(EventState.CREATED, R.color.red);
    map.put(EventState.OPENED, R.color.green);
    map.put(EventState.CLOSED, R.color.red);
    return map;
  }

  private EnumMap<EventState, Integer> buildManagementIconMap() {
    EnumMap<EventState, Integer> map = new EnumMap<>(EventState.class);
    map.put(EventState.CREATED, R.drawable.ic_unlock);
    map.put(EventState.OPENED, R.drawable.ic_lock);
    map.put(EventState.CLOSED, R.drawable.ic_unlock);
    return map;
  }

  /**
   * The following is only for testing purposes. Production should never pass arguments to a
   * fragment instantiation but should rather use arguments
   */
  @VisibleForTesting(otherwise = VisibleForTesting.NONE)
  public static RollCallFragment newInstance(RollCall rollCall) {
    return new RollCallFragment(rollCall);
  }

  @VisibleForTesting(otherwise = VisibleForTesting.NONE)
  public RollCallFragment(RollCall rollCall) {
    this.rollCall = rollCall;
  }
}
