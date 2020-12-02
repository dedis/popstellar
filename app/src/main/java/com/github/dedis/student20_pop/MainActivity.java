package com.github.dedis.student20_pop;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.github.dedis.student20_pop.model.Lao;
import com.github.dedis.student20_pop.model.Person;
import com.github.dedis.student20_pop.ui.CameraPermissionFragment;
import com.github.dedis.student20_pop.ui.ConnectFragment;
import com.github.dedis.student20_pop.ui.HomeFragment;
import com.github.dedis.student20_pop.ui.LaunchFragment;
import com.github.dedis.student20_pop.utility.network.PoPClientEndpoint;
import com.github.dedis.student20_pop.utility.security.PrivateInfoStorage;

import java.net.URI;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.CompletableFuture;

import javax.websocket.DeploymentException;

/**
 * Activity used to display the different UIs
 **/
public final class MainActivity extends FragmentActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        if (findViewById(R.id.fragment_container_main) != null) {
            if (savedInstanceState != null) {
                return;
            }

            getSupportFragmentManager().beginTransaction()
                        .add(R.id.fragment_container_main, new HomeFragment()).commit();
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
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
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

                CompletableFuture.supplyAsync(() -> {
                    try {
                        return PoPClientEndpoint.connectToServer(URI.create("ws://10.0.2.2:2020/"), organizer);
                    } catch (DeploymentException e) {
                        e.printStackTrace();
                        return null;
                    }
                }).thenCompose(p -> {
                    if(p != null)
                        return p.createLoa(lao.getName(), lao.getTime(), lao.getTime(), organizer.getId());
                    else
                        return null;
                }).whenComplete((errCode, t) -> {
                        if(t != null)
                            Log.e(TAG, "Error while creating the lao", t);
                        else {
                            // Start the Organizer Activity (user is considered an organizer)
                            Intent intent = new Intent(this, OrganizerActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);
                        }
                    });

                // Store the private key of the organizer
                if (PrivateInfoStorage.storeData(this, organizer.getId(), organizer.getAuthentication()))
                    Log.d(TAG, "Stored private key of organizer");
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
                    .replace(R.id.fragment_container_main, fragment, TAG)
                    .addToBackStack(TAG)
                    .commit();
        }
    }
}
