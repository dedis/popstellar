package com.github.dedis.popstellar.ui.lao.event.rollcall;

import static com.github.dedis.popstellar.utility.Constants.*;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.content.res.AppCompatResources;
import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.RollCallFragmentBinding;
import com.github.dedis.popstellar.model.objects.RollCall;
import com.github.dedis.popstellar.model.objects.event.EventState;
import com.github.dedis.popstellar.model.objects.security.PoPToken;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.model.qrcode.PopTokenData;
import com.github.dedis.popstellar.repository.RollCallRepository;
import com.github.dedis.popstellar.ui.lao.LaoActivity;
import com.github.dedis.popstellar.ui.lao.event.AbstractEventFragment;
import com.github.dedis.popstellar.ui.lao.event.eventlist.EventListFragment;
import com.github.dedis.popstellar.ui.qrcode.QrScannerFragment;
import com.github.dedis.popstellar.ui.qrcode.ScanningAction;
import com.github.dedis.popstellar.utility.ActivityUtils;
import com.github.dedis.popstellar.utility.Constants;
import com.github.dedis.popstellar.utility.error.*;
import com.github.dedis.popstellar.utility.error.keys.KeyException;
import com.github.dedis.popstellar.utility.error.keys.NoRollCallException;
import dagger.hilt.android.AndroidEntryPoint;
import java.util.EnumMap;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import net.glxn.qrgen.android.QRCode;
import timber.log.Timber;

@AndroidEntryPoint
public class RollCallFragment extends AbstractEventFragment {

  public static final String TAG = RollCallFragment.class.getSimpleName();

  @Inject RollCallRepository rollCallRepo;

  private RollCallFragmentBinding binding;

  private RollCall rollCall;

  private RollCallViewModel rollCallViewModel;

  private final EnumMap<EventState, Integer> managementTextMap = buildManagementTextMap();
  private final EnumMap<EventState, Integer> managementIconMap = buildManagementIconMap();

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

    // Set the description dropdown
    binding.rollCallDescriptionCard.setOnClickListener(
        v ->
            ActivityUtils.handleExpandArrow(
                binding.rollCallDescriptionArrow, binding.rollCallDescriptionText));

    // Set the location dropdown
    binding.rollCallLocationCard.setOnClickListener(
        v ->
            ActivityUtils.handleExpandArrow(
                binding.rollCallLocationArrow, binding.rollCallLocationText));

    binding.rollCallManagementButton.setOnClickListener(
        v -> {
          EventState state = rollCall.getState();
          switch (state) {
            case CLOSED:
            case CREATED:
              laoViewModel.addDisposable(
                  rollCallViewModel
                      .openRollCall(rollCall.id)
                      .subscribe(
                          () ->
                              /* Here the fragment is reopened as we want to have continuity between
                               * the list of attendees and the list of scanned tokens for the organizer.
                               * By reopening the fragment, the roll call attendees will be immediately
                               * displayed also in the list of scanned tokens. */
                              LaoActivity.setCurrentFragment(
                                  getParentFragmentManager(),
                                  R.id.fragment_roll_call,
                                  () -> RollCallFragment.newInstance(rollCall.persistentId)),
                          error ->
                              ErrorUtils.logAndShow(
                                  requireContext(), TAG, error, R.string.error_open_rollcall)));
              break;
            case OPENED:
              // will add the scan to this fragment in the future
              laoViewModel.addDisposable(
                  rollCallViewModel
                      .closeRollCall(rollCall.id)
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
            .getRollCallObservable(rollCall.persistentId)
            .subscribe(
                rc -> {
                  Timber.tag(TAG).d("Received rc update: %s", rc);
                  rollCall = rc;
                  setUpStateDependantContent();
                },
                error ->
                    ErrorUtils.logAndShow(
                        requireContext(), TAG, error, R.string.unknown_roll_call_exception)));

    handleBackNav(TAG);
    return binding.getRoot();
  }

  @Override
  public void onResume() {
    super.onResume();
    setTab(R.string.roll_call_title);
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
    setupTime(
        rollCall,
        binding.rollCallStartTime,
        binding
            .rollCallEndTime); // Suggested time is updated in case of early/late close/open/reopen

    EventState rcState = rollCall.getState();
    boolean isOrganizer = laoViewModel.isOrganizer();

    binding.rollCallFragmentTitle.setText(rollCall.getName());

    // Set the description and location visible if the QR is not displayed
    // (i.e. I'm the organizer or the roll call is open)
    if (rollCall.isOpen() && !isOrganizer) {
      binding.rollCallMetadataContainer.setVisibility(View.GONE);
    } else {
      binding.rollCallMetadataContainer.setVisibility(View.VISIBLE);
      // Set the description invisible if it's empty
      if (rollCall.description.isEmpty()) {
        binding.rollCallDescriptionCard.setVisibility(View.GONE);
      }
    }

    binding.rollCallLocationText.setText(rollCall.location);
    binding.rollCallDescriptionText.setText(rollCall.description);

    // Set visibility of management button as Gone by default
    binding.rollCallManagementButton.setVisibility(View.GONE);

    // The management button is only visible to the organizer under the following conditions:
    if (isOrganizer) {
      // If the roll call is the last closed roll call or it's not closed (either opened or created)
      try {
        if (!rollCall.isClosed()
            || rollCallRepo.getLastClosedRollCall(laoViewModel.getLaoId()).equals(rollCall)) {
          binding.rollCallManagementButton.setVisibility(View.VISIBLE);
        }
      } catch (NoRollCallException ignored) {
      }
    }

    binding.rollCallManagementButton.setText(managementTextMap.getOrDefault(rcState, ID_NULL));

    Drawable imgManagement =
        AppCompatResources.getDrawable(
            requireContext(), managementIconMap.getOrDefault(rcState, ID_NULL));
    binding.rollCallManagementButton.setCompoundDrawablesWithIntrinsicBounds(
        imgManagement, null, null, null);

    setStatus(rcState, binding.rollCallStatusIcon, binding.rollCallStatus);

    // Show scanning button only if the current state is Opened
    if (rcState == EventState.OPENED && isOrganizer) {
      binding.rollCallScanningButton.setVisibility(View.VISIBLE);
    } else {
      binding.rollCallScanningButton.setVisibility(View.GONE);
    }

    setupListOfAttendees();
    retrieveAndDisplayPublicKey();
    handleRotation();
  }

  /**
   * This function sets the visibility logic of both the header and the list of attendees/scanned
   * tokens, depending on the roll call state and whether the user is the organizer. The adapter of
   * the ViewList is set accordingly, as the proper content is displayed.
   */
  private void setupListOfAttendees() {
    boolean isOrganizer = laoViewModel.isOrganizer();
    // Set the visibility of the list:
    // It is set to visible only if the roll call is closed
    // Or also if the user is the organizer and roll call is opened
    // Otherwise do not display the list
    int visibility =
        (rollCall.isClosed() || (isOrganizer && rollCall.isOpen())) ? View.VISIBLE : View.INVISIBLE;
    binding.rollCallAttendeesText.setVisibility(visibility);
    binding.listViewAttendees.setVisibility(visibility);

    List<String> attendeesList = null;
    if (isOrganizer && rollCall.isOpen()) {
      // Show the list of all time scanned attendees if the roll call is opened
      // and the user is the organizer
      attendeesList =
          rollCallViewModel.getAttendees().stream()
              .map(PublicKey::getEncoded)
              .collect(Collectors.toList());
      binding.rollCallAttendeesText.setText(
          String.format(
              getResources().getString(R.string.roll_call_scanned),
              rollCallViewModel.getAttendees().size()));
    } else if (rollCall.isClosed()) {
      // Show the list of attendees if the roll call has ended
      binding.rollCallAttendeesText.setText(
          String.format(
              getResources().getString(R.string.roll_call_attendees), rollCall.attendees.size()));
      attendeesList =
          rollCall.attendees.stream().map(PublicKey::getEncoded).collect(Collectors.toList());
    }

    if (attendeesList != null) {
      binding.listViewAttendees.setAdapter(
          new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, attendeesList));
    }
  }

  @SuppressLint("SourceLockedOrientationActivity")
  private void handleRotation() {
    Activity activity = getActivity();
    if (activity == null) {
      return;
    }
    if (rollCall.isOpen() && !laoViewModel.isOrganizer()) {
      // If the qr is visible, then the activity rotation should be locked,
      // as the QR could not fit in the screen in landscape
      activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    } else {
      activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
    }
  }

  private void retrieveAndDisplayPublicKey() {
    PoPToken popToken = getPopToken();
    if (popToken == null) {
      return;
    }

    String pk = popToken.publicKey.getEncoded();
    Timber.tag(TAG).d("key displayed is %s", pk);

    // Set the QR visible only if the rollcall is opened and the user isn't the organizer
    binding.rollCallPkQrCode.setVisibility((rollCall.isOpen()) ? View.VISIBLE : View.INVISIBLE);

    // Don't lose time generating the QR code if it's not visible
    if (laoViewModel.isOrganizer() || rollCall.isClosed()) {
      return;
    }

    PopTokenData data = new PopTokenData(new PublicKey(pk));
    Bitmap myBitmap =
        QRCode.from(gson.toJson(data))
            .withColor(ActivityUtils.getQRCodeColor(requireContext()), Color.TRANSPARENT)
            .bitmap();
    binding.rollCallPkQrCode.setImageBitmap(myBitmap);
  }

  private EnumMap<EventState, Integer> buildManagementTextMap() {
    EnumMap<EventState, Integer> map = new EnumMap<>(EventState.class);
    map.put(EventState.CREATED, R.string.open);
    map.put(EventState.OPENED, R.string.close);
    map.put(EventState.CLOSED, R.string.reopen_rollcall);
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
