package com.github.dedis.student20_pop.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.github.dedis.student20_pop.Event;
import com.github.dedis.student20_pop.OrganizerActivity;
import com.github.dedis.student20_pop.R;
import com.github.dedis.student20_pop.ViewModelFactory;
import com.github.dedis.student20_pop.ui.HomeFragment;
import com.github.dedis.student20_pop.ui.LaunchFragment;
import com.github.dedis.student20_pop.ui.qrcode.CameraPermissionFragment;
import com.github.dedis.student20_pop.ui.qrcode.QRCodeScanningFragment;
import com.github.dedis.student20_pop.utility.ActivityUtils;

public class HomeActivity extends AppCompatActivity  {

    private HomeViewModel mViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupViewFragment();

        mViewModel = obtainViewModel(this);

        // Subscribe to "open lao" event
        mViewModel.getOpenLaoEvent().observe(this, new Observer<Event<String>>() {
            @Override
            public void onChanged(Event<String> stringEvent) {
                String laoId = stringEvent.getContentIfNotHandled();
                if (laoId != null) {
                    openLaoDetails(laoId);
                }
            }
        });

        // Subscribe to "openConnect" event
        mViewModel.getOpenConnectEvent().observe(this, new Observer<Event<String>>() {
            @Override
            public void onChanged(Event<String> stringEvent) {
                String action = stringEvent.getContentIfNotHandled();
                if (action != null) {
                    switch (action) {
                        case "SCAN":
                            setupScanFragment();
                            break;
                        case "REQUEST_CAMERA_PERMISSION":
                            setupCameraPermissionFragment();
                            break;
                    }
                }
            }
        });

        mViewModel.getOpenLaunchEvent().observe(this, new Observer<Event<Boolean>>() {
            @Override
            public void onChanged(Event<Boolean> booleanEvent) {
                Boolean event = booleanEvent.getContentIfNotHandled();
                if (event != null) {
                    setupLaunchFragment();
                }
            }
        });
    }

    public static HomeViewModel obtainViewModel(FragmentActivity activity) {
        ViewModelFactory factory = ViewModelFactory.getInstance(activity.getApplication());
        HomeViewModel viewModel = new ViewModelProvider(activity, factory).get(HomeViewModel.class);

        return viewModel;
    }

    private void setupViewFragment() {
        HomeFragment homeFragment = (HomeFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_container_main);
        if (homeFragment == null) {
            homeFragment = HomeFragment.newInstance();
            ActivityUtils.replaceFragmentInActivity(
                    getSupportFragmentManager(), homeFragment, R.id.fragment_container_main
            );
        }
    }

    private void setupScanFragment() {
        QRCodeScanningFragment scanningFragment = (QRCodeScanningFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_qrcode);
        if (scanningFragment == null) {
            scanningFragment = QRCodeScanningFragment.newInstance();
            ActivityUtils.replaceFragmentInActivity(
                    getSupportFragmentManager(), scanningFragment, R.id.fragment_container_main
            );
        }
    }

    private void setupCameraPermissionFragment() {
        CameraPermissionFragment cameraPermissionFragment = (CameraPermissionFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_camera_perm);
        if (cameraPermissionFragment == null) {
            cameraPermissionFragment = CameraPermissionFragment.newInstance();
            ActivityUtils.replaceFragmentInActivity(
                    getSupportFragmentManager(), cameraPermissionFragment, R.id.fragment_container_main
            );
        }
    }

    private void openLaoDetails(String laoId) {
        Intent intent = new Intent(this, OrganizerActivity.class);
        intent.putExtra("LAO_ID", laoId);
        startActivity(intent);
    }

    private void setupLaunchFragment() {
        LaunchFragment launchFragment = (LaunchFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_launch);
        if (launchFragment == null) {
            launchFragment = LaunchFragment.newInstance();
            ActivityUtils.replaceFragmentInActivity(
                    getSupportFragmentManager(), launchFragment, R.id.fragment_container_main
            );
        }
    }


}
