package com.github.dedis.popstellar.ui.detail.event.rollcall;

import android.Manifest;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.*;

import androidx.annotation.VisibleForTesting;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.widget.ImageViewCompat;
import androidx.fragment.app.Fragment;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.model.objects.RollCall;
import com.github.dedis.popstellar.model.objects.event.EventState;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.ui.detail.*;
import com.github.dedis.popstellar.ui.qrcode.CameraPermissionFragment;
import com.github.dedis.popstellar.ui.qrcode.QRCodeScanningFragment;
import com.github.dedis.popstellar.utility.Constants;
import com.github.dedis.popstellar.utility.error.ErrorUtils;

import net.glxn.qrgen.android.QRCode;

import java.text.SimpleDateFormat;
import java.util.*;

import dagger.hilt.android.AndroidEntryPoint;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static androidx.core.content.ContextCompat.checkSelfPermission;
import static com.github.dedis.popstellar.ui.detail.LaoDetailActivity.setCurrentFragment;
import static com.github.dedis.popstellar.utility.Constants.ID_NULL;

@AndroidEntryPoint
public class RollCallFragment extends Fragment {
  public static final String TAG = RollCallFragment.class.getSimpleName();
  private final SimpleDateFormat dateFormat =
      new SimpleDateFormat("dd/MM/yyyy HH:mm z", Locale.ENGLISH);
  private LaoDetailViewModel laoDetailViewModel;
  private RollCall rollCall;
  private Button managementButton;
  private TextView title;
  private TextView statusText;
  private ImageView statusIcon;
  private TextView startTimeDisplay;
  private TextView endTimeDisplay;

  private final EnumMap<EventState, Integer> managementTextMap = buildManagementTextMap();
  private final EnumMap<EventState, Integer> statusTextMap = buildStatusTextMap();
  private final EnumMap<EventState, Integer> statusIconMap = buildStatusIconMap();
  private final EnumMap<EventState, Integer> managementIconMap = buildManagementIconMap();
  private final EnumMap<EventState, Integer> statusColorMap = buildStatusColorMap();

  public RollCallFragment() {
    // Required empty public constructor
  }

  public static RollCallFragment newInstance(PublicKey pk) {
    RollCallFragment fragment = new RollCallFragment();
    Bundle bundle = new Bundle(1);
    bundle.putString(Constants.RC_PK_EXTRA, pk.getEncoded());
    fragment.setArguments(bundle);
    return fragment;
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    // Inflate the layout for this fragment
    View view = inflater.inflate(R.layout.roll_call_fragment, container, false);

    laoDetailViewModel = LaoDetailActivity.obtainViewModel(requireActivity());
    if (rollCall == null) {
      rollCall = laoDetailViewModel.getCurrentRollCall();
    }
    managementButton = view.findViewById(R.id.roll_call_management_button);
    title = view.findViewById(R.id.roll_call_fragment_title);
    statusText = view.findViewById(R.id.roll_call_fragment_status);
    statusIcon = view.findViewById(R.id.roll_call_fragment_status_icon);
    startTimeDisplay = view.findViewById(R.id.roll_call_fragment_start_time);
    endTimeDisplay = view.findViewById(R.id.roll_call_fragment_end_time);

    setUpStateDependantContent();

    View.OnClickListener listener =
        v -> {
          EventState state = rollCall.getState();
          switch (state) {
            case CLOSED:
            case CREATED:
              laoDetailViewModel.addDisposable(
                  laoDetailViewModel
                      .openRollCall(rollCall.getId())
                      .subscribe(
                          this::openScanning,
                          error ->
                              ErrorUtils.logAndShow(
                                  requireContext(), TAG, error, R.string.error_open_rollcall)));
              break;
            case OPENED:
              // will add the scan to this fragment in the future
              laoDetailViewModel.addDisposable(
                  laoDetailViewModel
                      .closeRollCall()
                      .subscribe(
                          () ->
                              setCurrentFragment(
                                  getParentFragmentManager(),
                                  R.id.fragment_lao_detail,
                                  LaoDetailFragment::newInstance),
                          error ->
                              ErrorUtils.logAndShow(
                                  requireContext(), TAG, error, R.string.error_close_rollcall)));
              break;
            default:
              throw new IllegalStateException("Roll-Call should not be in a " + state + " state");
          }
        };

    managementButton.setOnClickListener(listener);

    laoDetailViewModel
        .getLaoEvents()
        .observe(getViewLifecycleOwner(), eventState -> setUpStateDependantContent());

    retrieveAndDisplayPublicKey(view);

    return view;
  }

  private void openScanning() {
    if (checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PERMISSION_GRANTED) {
      setCurrentFragment(
          getParentFragmentManager(), R.id.add_attendee_layout, QRCodeScanningFragment::new);
    } else {
      setCurrentFragment(
          getParentFragmentManager(),
          R.id.fragment_camera_perm,
          () ->
              CameraPermissionFragment.newInstance(requireActivity().getActivityResultRegistry()));
    }
  }

  private void setUpStateDependantContent() {
    rollCall = laoDetailViewModel.getCurrentRollCall();
    setupTime(); // Suggested time is updated in case of early/late close/open/reopen

    EventState rcState = rollCall.getState();
    boolean isOrganizer = laoDetailViewModel.isOrganizer().getValue();

    title.setText(rollCall.getName());
    managementButton.setVisibility(isOrganizer ? View.VISIBLE : View.GONE);

    managementButton.setText(managementTextMap.getOrDefault(rcState, ID_NULL));

    Drawable imgManagement =
        AppCompatResources.getDrawable(
            getContext(), managementIconMap.getOrDefault(rcState, ID_NULL));
    managementButton.setCompoundDrawablesWithIntrinsicBounds(imgManagement, null, null, null);

    Drawable imgStatus = getDrawableFromContext(statusIconMap.getOrDefault(rcState, ID_NULL));
    statusIcon.setImageDrawable(imgStatus);
    setImageColor(statusIcon, statusColorMap.getOrDefault(rcState, ID_NULL));

    statusText.setText(statusTextMap.getOrDefault(rcState, ID_NULL));
    statusText.setTextColor(
        getResources().getColor(statusColorMap.getOrDefault(rcState, ID_NULL), null));
  }

  private void setupTime() {
    Date startTime = new Date(rollCall.getStartTimestampInMillis());
    Date endTime = new Date(rollCall.getEndTimestampInMillis());

    startTimeDisplay.setText(dateFormat.format(startTime));
    endTimeDisplay.setText(dateFormat.format(endTime));
  }

  private Drawable getDrawableFromContext(int id) {
    return AppCompatResources.getDrawable(getContext(), id);
  }

  private void setImageColor(ImageView imageView, int colorId) {
    ImageViewCompat.setImageTintList(imageView, getResources().getColorStateList(colorId, null));
  }

  private void retrieveAndDisplayPublicKey(View view) {
    String pk = requireArguments().getString(Constants.RC_PK_EXTRA);
    ImageView qrCode = view.findViewById(R.id.roll_call_pk_qr_code);
    Log.d(TAG, "key displayed is " + pk);
    Bitmap myBitmap = QRCode.from(pk).bitmap();
    qrCode.setImageBitmap(myBitmap);
    qrCode.setVisibility(
        Boolean.TRUE.equals(laoDetailViewModel.isOrganizer().getValue())
            ? View.INVISIBLE
            : View.VISIBLE);
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
