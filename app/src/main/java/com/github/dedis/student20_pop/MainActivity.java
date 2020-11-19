package com.github.dedis.student20_pop;

import android.Manifest;
import android.os.Bundle;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.github.dedis.student20_pop.model.Lao;
import com.github.dedis.student20_pop.model.Person;
import com.github.dedis.student20_pop.ui.CameraPermissionFragment;
import com.github.dedis.student20_pop.ui.AttendeeFragment;
import com.github.dedis.student20_pop.ui.ConnectFragment;
import com.github.dedis.student20_pop.ui.HomeFragment;
import com.github.dedis.student20_pop.ui.LaunchFragment;
import com.github.dedis.student20_pop.utility.security.PrivateInfoStorage;

import java.util.Collections;
import java.util.Date;

/**
 * Activity used to display the different UIs
 **/
public final class MainActivity extends FragmentActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private boolean testingAttendee = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        if (findViewById(R.id.fragment_container) != null) {
            if (savedInstanceState != null) {
                return;
            }

            if (testingAttendee){
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.fragment_container, new AttendeeFragment()).commit();
            }
            else {
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.fragment_container, new HomeFragment()).commit();
            }
        }
    }

    /**
     * Manage the fragment change after clicking a specific view.
     *
     * @param view the clicked view
     */
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.tab_home:
                showFragment(new HomeFragment(), HomeFragment.TAG);
                break;
            case R.id.tab_connect:
                if(ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
                    showFragment(new ConnectFragment(), ConnectFragment.TAG);
                else
                    showFragment(new CameraPermissionFragment(), CameraPermissionFragment.TAG);
                break;
            case R.id.tab_launch:
                showFragment(new LaunchFragment(), LaunchFragment.TAG);
                break;
            case R.id.button_launch:
                String name = ((EditText) findViewById(R.id.entry_box_launch)).getText().toString();
                // For later: send LAO and organizer information
                Person organizer = new Person("name");
                // Creating the LAO and adding it to the organizer's LAO
                Lao lao = new Lao(name, new Date(), organizer.getId());
                organizer.setLaos(Collections.singletonList(lao.getId()));
                // Store the private key of the organizer
                if(PrivateInfoStorage.storeData(this, organizer.getId(), organizer.getAuthentication()))
                    Log.d(TAG, "Stored private key of organizer");
                showFragment(new HomeFragment(), LaunchFragment.TAG);
                Toast.makeText(this,
                        getResources().getString(R.string.message_launch_successful, name),
                        Toast.LENGTH_LONG)
                        .show();
                break;
            case R.id.button_cancel_launch:
                ((EditText) findViewById(R.id.entry_box_launch)).getText().clear();
                showFragment(new HomeFragment(), LaunchFragment.TAG);
                break;
            default:
        }
    }

    private void showFragment(Fragment fragment, String TAG) {
        if (!fragment.isVisible()) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment, TAG)
                    .addToBackStack(TAG)
                    .commit();
        }
    }
}