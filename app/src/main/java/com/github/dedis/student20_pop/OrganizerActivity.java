package com.github.dedis.student20_pop;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.github.dedis.student20_pop.model.Keys;
import com.github.dedis.student20_pop.model.Lao;
import com.github.dedis.student20_pop.ui.CameraPermissionFragment;
import com.github.dedis.student20_pop.ui.ConnectingFragment;
import com.github.dedis.student20_pop.ui.HomeFragment;
import com.github.dedis.student20_pop.ui.IdentityFragment;
import com.github.dedis.student20_pop.ui.OrganizerFragment;
import com.github.dedis.student20_pop.ui.QRCodeScanningFragment;
import com.github.dedis.student20_pop.ui.QRCodeScanningFragment.QRCodeScanningType;
import com.github.dedis.student20_pop.utility.qrcode.OnCameraAllowedListener;
import com.github.dedis.student20_pop.utility.qrcode.OnCameraNotAllowedListener;
import com.github.dedis.student20_pop.utility.qrcode.QRCodeListener;
import com.github.dedis.student20_pop.utility.ui.OnAddWitnessListener;
import com.github.dedis.student20_pop.utility.ui.OnEventTypeSelectedListener;

import java.util.Date;

import static com.github.dedis.student20_pop.ui.QRCodeScanningFragment.QRCodeScanningType.ADD_WITNESS;

/**
 * Activity used to display the different UIs for organizers
 **/
public class OrganizerActivity extends FragmentActivity implements OnEventTypeSelectedListener, OnAddWitnessListener,
        OnCameraNotAllowedListener, QRCodeListener, OnCameraAllowedListener {

    public static final String TAG = OrganizerActivity.class.getSimpleName();
    public static final String PRIVATE_KEY_TAG = "PRIVATE_KEY";
    public static final String LAO_ID_TAG = "LAO_ID";

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
                showFragment(new HomeFragment(), HomeFragment.TAG);
                break;
            case R.id.tab_identity:
                Bundle bundle = new Bundle();

                final PoPApplication app = ((PoPApplication) getApplication());
                bundle.putString(PRIVATE_KEY_TAG, app.getPerson().getAuthentication());

                //TODO : Retrieve this LAO from the Intent
                Lao lao = new Lao("LAO I just joined", new Date(), new Keys().getPublicKey());
                bundle.putString(LAO_ID_TAG, lao.getId());

                // set Fragmentclass Arguments
                IdentityFragment identityFragment = new IdentityFragment();
                identityFragment.setArguments(bundle);
                showFragment(identityFragment, IdentityFragment.TAG);
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

    @Override
    public void OnEventTypeSelectedListener(EventType eventType) {
        switch (eventType) {
            case MEETING:
                //TODO
                Log.d("Meeting Event Type ", "Launch here Meeting Event Creation Fragment");
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
        // TODO : Get witness id from the QR code, add witness to witness list and send info to backend
    }

    @Override
    public void onCameraNotAllowedListener(QRCodeScanningType qrCodeScanningType) {
        showFragment(new CameraPermissionFragment(qrCodeScanningType), CameraPermissionFragment.TAG);
    }

    @Override
    public void onQRCodeDetected(String url, QRCodeScanningType qrCodeScanningType) {
        Log.i(TAG, "Received qrcode url : " + url);
        switch (qrCodeScanningType) {
            case ADD_ROLL_CALL:
                //TODO
                break;
            case ADD_WITNESS:
                //TODO
                Log.d("DEBUG", "URL Received : " + url);
                break;
            case CONNECT_LAO:
                showFragment(ConnectingFragment.newInstance(url), ConnectingFragment.TAG);
                break;
            default:
                break;
        }
    }

    @Override
    public void onCameraAllowedListener(QRCodeScanningType qrCodeScanningType) {
        showFragment(new QRCodeScanningFragment(qrCodeScanningType), QRCodeScanningFragment.TAG);
    }
}
