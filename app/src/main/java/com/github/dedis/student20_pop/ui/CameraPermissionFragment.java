package com.github.dedis.student20_pop.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.github.dedis.student20_pop.R;

/**
 * Fragment handling permission granting for the camera
 */
public final class CameraPermissionFragment extends Fragment implements View.OnClickListener {

    public static final String TAG = ConnectFragment.class.getSimpleName();

    private static final int HANDLE_CAMERA_PERM = 2;
    private int container;

    public CameraPermissionFragment(){
        this(R.id.fragment_container_main);
    }

    public CameraPermissionFragment(int container){
        super();
        this.container = container;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_camera_perm, container, false);
        //set button click listener
        view.findViewById(R.id.allow_camera_button).setOnClickListener(this);

        // Check for the camera permission, if is is granted, switch to ConnectFragment
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
            switchToConnectFragment();

        return view;
    }

    private void switchToConnectFragment() {
        requireFragmentManager()
                .beginTransaction()
                .replace(container, new ConnectFragment(), ConnectFragment.TAG)
                .commit();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == HANDLE_CAMERA_PERM &&
                grantResults.length != 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // we have permission, so we switch to ConnectFragment
            switchToConnectFragment();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // If the permission was granted while the app was paused, switch to ConnectFragment

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
            switchToConnectFragment();
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.allow_camera_button) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, HANDLE_CAMERA_PERM);
        }
    }
}
