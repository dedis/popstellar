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

import com.github.dedis.student20_pop.model.Event;
import com.github.dedis.student20_pop.model.Keys;
import com.github.dedis.student20_pop.ui.CameraPermissionFragment;
import com.github.dedis.student20_pop.ui.ConnectingFragment;
import com.github.dedis.student20_pop.ui.IdentityFragment;
import com.github.dedis.student20_pop.ui.OrganizerFragment;
import com.github.dedis.student20_pop.ui.QRCodeScanningFragment;
import com.github.dedis.student20_pop.ui.QRCodeScanningFragment.QRCodeScanningType;
import com.github.dedis.student20_pop.ui.event.MeetingEventCreationFragment;
import com.github.dedis.student20_pop.utility.qrcode.OnCameraAllowedListener;
import com.github.dedis.student20_pop.utility.qrcode.OnCameraNotAllowedListener;
import com.github.dedis.student20_pop.utility.qrcode.QRCodeListener;
import com.github.dedis.student20_pop.utility.ui.organizer.OnAddWitnessListener;
import com.github.dedis.student20_pop.utility.ui.organizer.OnEventCreatedListener;
import com.github.dedis.student20_pop.utility.ui.organizer.OnEventTypeSelectedListener;

import static com.github.dedis.student20_pop.PoPApplication.AddWitnessResult;
import static com.github.dedis.student20_pop.PoPApplication.AddWitnessResult.ADD_WITNESS_ALREADY_EXISTS;
import static com.github.dedis.student20_pop.PoPApplication.AddWitnessResult.ADD_WITNESS_SUCCESSFUL;
import static com.github.dedis.student20_pop.PoPApplication.getAppContext;
import static com.github.dedis.student20_pop.ui.QRCodeScanningFragment.QRCodeScanningType.ADD_WITNESS;

/**
 * Activity used to display the different UIs for organizers
 **/
public class OrganizerActivity extends FragmentActivity implements OnEventTypeSelectedListener, OnEventCreatedListener, OnAddWitnessListener,
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
        // Prevent going back to MainActivity
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
    public void OnEventTypeSelectedListener(Event.EventType eventType) {
        switch (eventType) {
            case MEETING:
                showFragment(new MeetingEventCreationFragment(), MeetingEventCreationFragment.TAG);
                break;
            case ROLL_CALL:
                //TODO
                Log.d("Roll-Call Event Type ", "Launch here Roll-Call Event Creation Fragment");
                break;
            case POLL:
                //TODO
                Log.d("Poll Event Type ", "Launch here Poll Event Creation Fragment");
                break;
            default:
                Log.d("Default Event Type :", "Default Behaviour TBD");
                break;
        }
    }

    @Override
    public void onAddWitnessListener() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            showFragment(new QRCodeScanningFragment(ADD_WITNESS), QRCodeScanningFragment.TAG);
        } else {
            showFragment(new CameraPermissionFragment(ADD_WITNESS), CameraPermissionFragment.TAG);
        }
    }

    @Override
    public void onCameraNotAllowedListener(QRCodeScanningType qrCodeScanningType) {
        showFragment(new CameraPermissionFragment(qrCodeScanningType), CameraPermissionFragment.TAG);
    }

    @Override
    public void onQRCodeDetected(String data, QRCodeScanningType qrCodeScanningType) {
        Log.i(TAG, "Received qrcode url : " + data);
        switch (qrCodeScanningType) {
            case ADD_ROLL_CALL:
                //TODO
                break;
            case ADD_WITNESS:
                //TODO
                int keyLength = new Keys().getPublicKey().length();
                String witnessId = data.substring(0, keyLength);

                PoPApplication app = (PoPApplication) getApplication();
                AddWitnessResult hasBeenAdded = app.addWitness(witnessId);

                this.runOnUiThread(
                        () -> {
                            if (hasBeenAdded == ADD_WITNESS_SUCCESSFUL) {
                                Toast.makeText(this, getString(R.string.add_witness_successful), Toast.LENGTH_SHORT).show();
                                getSupportFragmentManager().popBackStackImmediate();
                            } else if (hasBeenAdded == ADD_WITNESS_ALREADY_EXISTS) {
                                Toast.makeText(getAppContext(), getString(R.string.add_witness_already_exists), Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, getString(R.string.add_witness_unsuccessful), Toast.LENGTH_SHORT).show();
                            }
                        });

                break;
            case CONNECT_LAO:
                showFragment(ConnectingFragment.newInstance(data), ConnectingFragment.TAG);
                break;
            default:
                break;
        }
    }

    @Override
    public void onCameraAllowedListener(QRCodeScanningType qrCodeScanningType) {
        showFragment(new QRCodeScanningFragment(qrCodeScanningType), QRCodeScanningFragment.TAG);
    }

    public void OnEventCreatedListener(Event event) {
        ((PoPApplication) getApplication()).addEvent(event);
    }
}
