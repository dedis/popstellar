package com.github.dedis.student20_pop;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.github.dedis.student20_pop.model.Keys;
import com.github.dedis.student20_pop.model.event.Event;
import com.github.dedis.student20_pop.model.event.EventType;
import com.github.dedis.student20_pop.model.event.RollCallEvent;
import com.github.dedis.student20_pop.ui.ConnectingFragment;
import com.github.dedis.student20_pop.ui.IdentityFragment;
import com.github.dedis.student20_pop.ui.OrganizerFragment;
import com.github.dedis.student20_pop.ui.event.creation.MeetingEventCreationFragment;
import com.github.dedis.student20_pop.ui.event.creation.PollEventCreationFragment;
import com.github.dedis.student20_pop.ui.event.creation.RollCallEventCreationFragment;
import com.github.dedis.student20_pop.ui.qrcode.AddAttendeeFragment;
import com.github.dedis.student20_pop.ui.qrcode.CameraPermissionFragment;
import com.github.dedis.student20_pop.ui.qrcode.QRCodeScanningFragment;
import com.github.dedis.student20_pop.ui.qrcode.QRCodeScanningFragment.QRCodeScanningType;
import com.github.dedis.student20_pop.utility.qrcode.OnCameraAllowedListener;
import com.github.dedis.student20_pop.utility.qrcode.OnCameraNotAllowedListener;
import com.github.dedis.student20_pop.utility.qrcode.QRCodeListener;
import com.github.dedis.student20_pop.utility.ui.listener.OnAddAttendeesListener;
import com.github.dedis.student20_pop.utility.ui.listener.OnAddWitnessListener;
import com.github.dedis.student20_pop.utility.ui.listener.OnEventCreatedListener;
import com.github.dedis.student20_pop.utility.ui.listener.OnEventTypeSelectedListener;

import java.util.Optional;

import static com.github.dedis.student20_pop.PoPApplication.AddWitnessResult;
import static com.github.dedis.student20_pop.PoPApplication.AddWitnessResult.ADD_WITNESS_ALREADY_EXISTS;
import static com.github.dedis.student20_pop.PoPApplication.AddWitnessResult.ADD_WITNESS_SUCCESSFUL;
import static com.github.dedis.student20_pop.PoPApplication.getAppContext;
import static com.github.dedis.student20_pop.model.event.RollCallEvent.AddAttendeeResult;
import static com.github.dedis.student20_pop.model.event.RollCallEvent.AddAttendeeResult.ADD_ATTENDEE_ALREADY_EXISTS;
import static com.github.dedis.student20_pop.model.event.RollCallEvent.AddAttendeeResult.ADD_ATTENDEE_SUCCESSFUL;
import static com.github.dedis.student20_pop.model.event.RollCallEvent.AddAttendeeResult.ADD_ATTENDEE_UNSUCCESSFUL;
import static com.github.dedis.student20_pop.ui.qrcode.QRCodeScanningFragment.QRCodeScanningType.ADD_ROLL_CALL_ATTENDEE;
import static com.github.dedis.student20_pop.ui.qrcode.QRCodeScanningFragment.QRCodeScanningType.ADD_WITNESS;

/**
 * Activity used to display the different UIs for organizers
 **/
public class OrganizerActivity extends FragmentActivity implements OnEventTypeSelectedListener, OnEventCreatedListener, OnAddWitnessListener, OnAddAttendeesListener,
        OnCameraNotAllowedListener, QRCodeListener, OnCameraAllowedListener {

    public static final String TAG = OrganizerActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_organizer);
        if (findViewById(R.id.fragment_container_organizer) != null) {
            if (savedInstanceState != null) {
                return;
            }

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container_organizer, new OrganizerFragment()).commit();
        }
    }

    @Override
    public void onBackPressed() {
        getSupportFragmentManager().popBackStackImmediate();
    }

    /**
     * Manage the fragment change after clicking a specific view.
     *
     * @param view the clicked view
     */
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.tab_home:
                //Future: different Home UI for organizer (without connect UI?)
                Intent mainActivityIntent = new Intent(this, MainActivity.class);
                startActivity(mainActivityIntent);
                break;
            case R.id.tab_identity:
                showFragment(new IdentityFragment(), IdentityFragment.TAG);
                break;

            default:
                break;
        }
    }

    private void showFragment(Fragment fragment, String TAG) {
        if (!fragment.isVisible()) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container_organizer, fragment, TAG)
                    .addToBackStack(TAG)
                    .commit();
        }
    }

    /**
     * Launches the fragment corresponding to the event creation the organizer has chosen
     *
     * @param eventType
     */
    @Override
    public void OnEventTypeSelectedListener(EventType eventType) {
        switch (eventType) {
            case MEETING:
                showFragment(new MeetingEventCreationFragment(), MeetingEventCreationFragment.TAG);
                break;
            case ROLL_CALL:
                showFragment(new RollCallEventCreationFragment(), RollCallEventCreationFragment.TAG);
                break;
            case POLL:
                showFragment(new PollEventCreationFragment(), PollEventCreationFragment.TAG);
                break;
            default:
                Log.d("Default Event Type :", "Default Behaviour TBD");
                break;
        }
    }

    @Override
    public void onAddWitnessListener() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            showFragment(new QRCodeScanningFragment(ADD_WITNESS, null), QRCodeScanningFragment.TAG);
        } else {
            showFragment(new CameraPermissionFragment(ADD_WITNESS, null), CameraPermissionFragment.TAG);
        }
    }

    @Override
    public void onCameraNotAllowedListener(QRCodeScanningType qrCodeScanningType, String eventId) {
        showFragment(new CameraPermissionFragment(qrCodeScanningType, eventId), CameraPermissionFragment.TAG);
    }

    @Override
    public void onQRCodeDetected(String data, QRCodeScanningType qrCodeScanningType, String eventId) {
        Log.i(TAG, "Received qrcode url : " + data);

        int keyLength = new Keys().getPublicKey().length();
        String personId = data.substring(0, keyLength);
        PoPApplication app = (PoPApplication) getApplication();

        switch (qrCodeScanningType) {
            case ADD_ROLL_CALL_ATTENDEE:
                Optional<Event> matchingEvent = app.getCurrentLao()
                        .flatMap(lao ->
                                lao.getEvents()
                                .parallelStream()
                                .filter(event -> event.getId().equals(eventId))
                                .distinct()
                                .findAny());

                this.runOnUiThread(
                        () -> {
                            AddAttendeeResult attendeeHasBeenAdded;
                            if (matchingEvent.isPresent()) {
                                attendeeHasBeenAdded = ((RollCallEvent) matchingEvent.get()).addAttendee(personId);
                            } else {
                                attendeeHasBeenAdded = ADD_ATTENDEE_UNSUCCESSFUL;
                            }

                            if (attendeeHasBeenAdded == ADD_ATTENDEE_SUCCESSFUL) {
                                Toast.makeText(this, getString(R.string.add_attendee_successful), Toast.LENGTH_LONG).show();
                            } else if (attendeeHasBeenAdded == ADD_ATTENDEE_ALREADY_EXISTS) {
                                Toast.makeText(getAppContext(), getString(R.string.add_attendee_already_exists), Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(this, getString(R.string.add_attendee_unsuccessful), Toast.LENGTH_LONG).show();
                            }
                        });

                break;
            case ADD_WITNESS:
                //TODO
                AddWitnessResult witnessHasBeenAdded = app.addWitness(personId);

                this.runOnUiThread(
                        () -> {
                            if (witnessHasBeenAdded == ADD_WITNESS_SUCCESSFUL) {
                                Toast.makeText(this, getString(R.string.add_witness_successful), Toast.LENGTH_SHORT).show();
                                getSupportFragmentManager().popBackStackImmediate();
                            } else if (witnessHasBeenAdded == ADD_WITNESS_ALREADY_EXISTS) {
                                Toast.makeText(getAppContext(), getString(R.string.add_witness_already_exists), Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, getString(R.string.add_witness_unsuccessful), Toast.LENGTH_SHORT).show();
                            }
                        });

                break;
            case CONNECT_LAO:
                // TODO extract url and lao id from data
                showFragment(ConnectingFragment.newInstance(data, "lao_id"), ConnectingFragment.TAG);
                break;
            default:
                break;
        }
    }

    @Override
    public void onCameraAllowedListener(QRCodeScanningType qrCodeScanningType, String eventId) {
        showFragment(new QRCodeScanningFragment(qrCodeScanningType, eventId), QRCodeScanningFragment.TAG);
    }

    @Override
    public void OnEventCreatedListener(Event event) {
        ((PoPApplication) getApplication()).addEvent(event);
    }

    @Override
    public void onAddAttendeesListener(String eventId) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            showFragment(new AddAttendeeFragment(eventId), AddAttendeeFragment.TAG);
        } else {
            showFragment(new CameraPermissionFragment(ADD_ROLL_CALL_ATTENDEE, eventId), CameraPermissionFragment.TAG);
        }
    }
}
