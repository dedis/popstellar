package com.github.dedis.popstellar.ui.lao.event.rollcall;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.widget.ImageViewCompat;
import androidx.fragment.app.Fragment;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.RollCallFragmentBinding;
import com.github.dedis.popstellar.model.objects.RollCall;
import com.github.dedis.popstellar.model.objects.event.EventState;
import com.github.dedis.popstellar.model.objects.security.PoPToken;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.model.qrcode.PopTokenData;
import com.github.dedis.popstellar.repository.RollCallRepository;
import com.github.dedis.popstellar.ui.lao.LaoActivity;
import com.github.dedis.popstellar.ui.lao.LaoViewModel;
import com.github.dedis.popstellar.ui.lao.event.eventlist.EventListFragment;
import com.github.dedis.popstellar.ui.qrcode.QrScannerFragment;
import com.github.dedis.popstellar.ui.qrcode.ScanningAction;
import com.github.dedis.popstellar.utility.ActivityUtils;
import com.github.dedis.popstellar.utility.Constants;
import com.github.dedis.popstellar.utility.error.*;
import com.github.dedis.popstellar.utility.error.keys.KeyException;
import com.google.gson.Gson;

import net.glxn.qrgen.android.QRCode;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

import static com.github.dedis.popstellar.utility.Constants.ID_NULL;
import static com.github.dedis.popstellar.utility.Constants.ROLL_CALL_ID;

@AndroidEntryPoint
public class RollCallFragment extends Fragment {

  public static final String TAG = RollCallFragment.class.getSimpleName();

  @Inject Gson gson;
  @Inject RollCallRepository rollCallRepo;

  private final SimpleDateFormat dateFormat =
      new SimpleDateFormat("dd/MM/yyyy HH:mm z", Locale.ENGLISH);

  private RollCallFragmentBinding binding;

  private LaoViewModel laoViewModel;
  private RollCall rollCall;

  private RollCallViewModel rollCallViewModel;

  private final EnumMap<EventState, Integer> managementTextMap = buildManagementTextMap();
  private final EnumMap<EventState, Integer> statusTextMap = buildStatusTextMap();
  private final EnumMap<EventState, Integer> statusIconMap = buildStatusIconMap();
  private final EnumMap<EventState, Integer> managementIconMap = buildManagementIconMap();
  private final EnumMap<EventState, Integer> statusColorMap = buildStatusColorMap();

  public RollCallFragment() {
    // Required empty public constructor
  }

  public static RollCallFragment newInstance(String persistentId) {
    RollCallFragment fragment = new RollCallFragment();
    Bundle bundle = new Bundle(1);
    bundle.putString(Constants.ROLL_CALL_ID, persistentId);
    fragment.setArguments(bundle);
    return fragment;
  }

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    // Inflate the layout for this fragment
    binding = RollCallFragmentBinding.inflate(inflater, container, false);
    laoViewModel = LaoActivity.obtainViewModel(requireActivity());
    rollCallViewModel =
        LaoActivity.obtainRollCallViewModel(requireActivity(), laoViewModel.getLaoId());

    try {
      rollCall =
          rollCallRepo.getRollCallWithPersistentId(
              laoViewModel.getLaoId(), requireArguments().getString(ROLL_CALL_ID));
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
              laoViewModel.addDisposable(
                  rollCallViewModel
                      .openRollCall(rollCall.getId())
                      .subscribe(
                          () -> {},
                          error ->
                              ErrorUtils.logAndShow(
                                  requireContext(), TAG, error, R.string.error_open_rollcall)));
              break;
            case OPENED:
              // will add the scan to this fragment in the future
              laoViewModel.addDisposable(
                  rollCallViewModel
                      .closeRollCall(rollCall.getId())
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
                getParentFragmentManager(),
                R.id.fragment_qr_scanner,
                () ->
                    QrScannerFragment.newInstance(
                        ScanningAction.ADD_ROLL_CALL_ATTENDEE,
                        requireArguments().getString(ROLL_CALL_ID))));

    laoViewModel.addDisposable(
        rollCallViewModel
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

    handleBackNav();
    return binding.getRoot();
  }

  @Override
  public void onResume() {
    super.onResume();
    laoViewModel.setPageTitle(R.string.roll_call_title);
    laoViewModel.setIsTab(false);
    try {
      rollCall =
          rollCallRepo.getRollCallWithPersistentId(
              laoViewModel.getLaoId(), requireArguments().getString(ROLL_CALL_ID));
    } catch (UnknownRollCallException e) {
      ErrorUtils.logAndShow(requireContext(), TAG, e, R.string.unknown_roll_call_exception);
    }
  }

  private PoPToken getPopToken() {
    try {
      return laoViewModel.getCurrentPopToken(rollCall);
    } catch (KeyException e) {
      ErrorUtils.logAndShow(requireContext(), TAG, e, R.string.key_generation_exception);
      return null;
    } catch (UnknownLaoException e) {
      ErrorUtils.logAndShow(requireContext(), TAG, e, R.string.unknown_lao_exception);
      return null;
    }
  }

  private void setUpStateDependantContent() {
    setupTime(); // Suggested time is updated in case of early/late close/open/reopen

    EventState rcState = rollCall.getState();
    boolean isOrganizer = laoViewModel.isOrganizer();

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

    setupListOfAttendees();
    retrieveAndDisplayPublicKey();
  }

  /**
   * This function sets the visibility logic of both the header and the list of attendees/scanned
   * tokens, depending on the roll call state and whether the user is the organizer. The adapter of
   * the ViewList is set accordingly, as the proper content is displayed.
   */
  private void setupListOfAttendees() {
    boolean isOrganizer = laoViewModel.isOrganizer();
    // Set the visibility of the list
    // It is set to visible only if the roll call is closed
    // Or also if the user is the organizer and roll call is opened
    // Otherwise do not display the list
    int visibility =
        (rollCall.isClosed() || (isOrganizer && rollCall.isOpen())) ? View.VISIBLE : View.INVISIBLE;
    binding.rollCallAttendeesText.setVisibility(visibility);
    binding.listViewAttendees.setVisibility(visibility);

    List<String> attendeesList = null;
    // Show the list of scanned attendees if the roll call is opened and the user is the organizer
    if (isOrganizer && rollCall.isOpen()) {
      binding.rollCallAttendeesText.setText(R.string.roll_call_scanned);
      attendeesList =
          rollCallViewModel.getAttendees().stream()
              .map(PublicKey::getEncoded)
              .collect(Collectors.toList());
    } else if (rollCall.isClosed()) { // Show the list of attendees if the roll call has ended
      binding.rollCallAttendeesText.setText(R.string.roll_call_attendees);
      attendeesList =
          rollCall.getAttendees().stream().map(PublicKey::getEncoded).collect(Collectors.toList());
    }
    if (attendeesList != null) {
      binding.listViewAttendees.setAdapter(
          new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, attendeesList));
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
    PoPToken popToken = getPopToken();
    if (popToken == null) {
      return;
    }

    String pk = popToken.getPublicKey().getEncoded();
    Log.d(TAG, "key displayed is " + pk);

    PopTokenData data = new PopTokenData(new PublicKey(pk));

    Bitmap myBitmap =
        QRCode.from(gson.toJson(data))
            .withColor(ActivityUtils.getQRCodeColor(getActivity()), Color.TRANSPARENT)
            .bitmap();
    binding.rollCallPkQrCode.setImageBitmap(myBitmap);
    // Set the QR visible only if the rollcall is opened and the user isn't the organizer
    binding.rollCallPkQrCode.setVisibility(
        (!laoViewModel.isOrganizer() && rollCall.isOpen()) ? View.VISIBLE : View.INVISIBLE);
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
    map.put(EventState.CREATED, R.string.created_displayed_text);
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

  private void handleBackNav() {
    requireActivity()
        .getOnBackPressedDispatcher()
        .addCallback(
            getViewLifecycleOwner(),
            new OnBackPressedCallback(true) {
              @Override
              public void handleOnBackPressed() {
                Log.d(TAG, "Back pressed, going to event list");
                LaoActivity.setCurrentFragment(
                    getParentFragmentManager(), R.id.fragment_event_list, EventListFragment::new);
              }
            });
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
