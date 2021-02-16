package com.github.dedis.student20_pop.home;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.github.dedis.student20_pop.Event;
import com.github.dedis.student20_pop.Injection;
import com.github.dedis.student20_pop.OrganizerActivity;
import com.github.dedis.student20_pop.R;
import com.github.dedis.student20_pop.ViewModelFactory;
import com.github.dedis.student20_pop.ui.ConnectingFragment;
import com.github.dedis.student20_pop.ui.HomeFragment;
import com.github.dedis.student20_pop.ui.LaunchFragment;
import com.github.dedis.student20_pop.ui.qrcode.CameraPermissionFragment;
import com.github.dedis.student20_pop.ui.qrcode.QRCodeScanningFragment;
import com.github.dedis.student20_pop.utility.ActivityUtils;
import com.google.android.gms.vision.barcode.BarcodeDetector;


public class HomeActivity extends AppCompatActivity {

    private HomeViewModel mViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupHomeFragment();

        mViewModel = obtainViewModel(this);

        setupHomeButton();
        setupLaunchButton();
        setupConnectButton();

        // Subscribe to "open lao" event
        mViewModel.getOpenLaoEvent().observe(this, stringEvent -> {
            String laoId = stringEvent.getContentIfNotHandled();
            if (laoId != null) {
                openLaoDetails(laoId);
            }
        });

        // Subscribe to "open home" event
        mViewModel.getOpenHomeEvent().observe(this, booleanEvent -> {
            Boolean event = booleanEvent.getContentIfNotHandled();
            if (event != null) {
                setupHomeFragment();
            }
        });

        // Subscribe to "open connecting" event
        mViewModel.getOpenConnectingEvent().observe(this, booleanEvent -> {
            Boolean event = booleanEvent.getContentIfNotHandled();
            if (event != null) {
                setupConnectingFragment("url", "lao");
            }
        });

        // Subscribe to "open connect" event
        mViewModel.getOpenConnectEvent().observe(this, stringEvent -> {
            String action = stringEvent.getContentIfNotHandled();
            if (action != null) {
                switch (action) {
                    case HomeViewModel.SCAN:
                        setupScanFragment();
                        break;
                    case HomeViewModel.REQUEST_CAMERA_PERMISSION:
                        setupCameraPermissionFragment();
                        break;
                }
            }
        });

        // Subscribe to "open launch" event
        mViewModel.getOpenLaunchEvent().observe(this, booleanEvent -> {
            Boolean event = booleanEvent.getContentIfNotHandled();
            if (event != null) {
                setupLaunchFragment();
            }
        });
    }

    public static HomeViewModel obtainViewModel(FragmentActivity activity) {
        ViewModelFactory factory = ViewModelFactory.getInstance(activity.getApplication());
        HomeViewModel viewModel = new ViewModelProvider(activity, factory).get(HomeViewModel.class);

        return viewModel;
    }

    public void setupHomeButton() {
        Button homeButton = (Button) findViewById(R.id.tab_home);

        homeButton.setOnClickListener(v -> mViewModel.openHome());
    }

    public void setupConnectButton() {
        Button connectButton = (Button) findViewById(R.id.tab_connect);

        connectButton.setOnClickListener(v -> mViewModel.openConnect());
    }

    public void setupLaunchButton() {
        Button launchButton = (Button) findViewById(R.id.tab_launch);

        launchButton.setOnClickListener(v -> mViewModel.openLaunch());
    }

    private void setupHomeFragment() {
        HomeFragment homeFragment = (HomeFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_home);
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
            Context context = getApplicationContext();
            BarcodeDetector qrCodeDetector = Injection.provideQRCodeDetector(context);
            int width = getResources().getInteger(R.integer.camera_preview_width);
            int height = getResources().getInteger(R.integer.camera_preview_height);
            scanningFragment = QRCodeScanningFragment.newInstance(
                    Injection.provideCameraSource(context, qrCodeDetector, width, height),
                    qrCodeDetector
            );
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

    private void setupLaunchFragment() {
        LaunchFragment launchFragment = (LaunchFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_launch);
        if (launchFragment == null) {
            launchFragment = LaunchFragment.newInstance();
            ActivityUtils.replaceFragmentInActivity(
                    getSupportFragmentManager(), launchFragment, R.id.fragment_container_main
            );
        }
    }

    private void setupConnectingFragment(String url, String laoId) {
        ConnectingFragment connectingFragment = (ConnectingFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_connecting);
        if (connectingFragment == null) {
            connectingFragment = ConnectingFragment.newInstance();
            ActivityUtils.replaceFragmentInActivity(
                    getSupportFragmentManager(), connectingFragment, R.id.fragment_container_main
            );
        }
    }

    private void openLaoDetails(String laoId) {
        Intent intent = new Intent(this, OrganizerActivity.class);
        intent.putExtra("LAO_ID", laoId);
        startActivity(intent);
    }
}
