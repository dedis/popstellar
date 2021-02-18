package com.github.dedis.student20_pop.detail;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;

import com.github.dedis.student20_pop.R;
import com.github.dedis.student20_pop.ViewModelFactory;
import com.github.dedis.student20_pop.home.HomeActivity;
import com.github.dedis.student20_pop.model.entities.LAOEntity;
import com.github.dedis.student20_pop.model.event.EventType;
import com.github.dedis.student20_pop.ui.IdentityFragment;
import com.github.dedis.student20_pop.ui.LaoDetailFragment;
import com.github.dedis.student20_pop.ui.OrganizerFragment;
import com.github.dedis.student20_pop.ui.event.creation.MeetingEventCreationFragment;
import com.github.dedis.student20_pop.ui.event.creation.PollEventCreationFragment;
import com.github.dedis.student20_pop.ui.event.creation.RollCallEventCreationFragment;
import com.github.dedis.student20_pop.ui.qrcode.CameraPermissionFragment;
import com.github.dedis.student20_pop.ui.qrcode.QRCodeScanningFragment;
import com.github.dedis.student20_pop.utility.ActivityUtils;
import com.github.dedis.student20_pop.utility.ui.listener.OnAddWitnessListener;
import com.github.dedis.student20_pop.utility.ui.listener.OnEventTypeSelectedListener;

import java.util.Objects;

import static com.github.dedis.student20_pop.ui.qrcode.QRCodeScanningFragment.QRCodeScanningType.ADD_WITNESS;

public class LaoDetailActivity extends AppCompatActivity implements OnEventTypeSelectedListener, OnAddWitnessListener {

    private LaoDetailViewModel mViewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lao_detail);

        mViewModel = obtainViewModel(this);

        mViewModel.setCurrentLao((String) Objects.requireNonNull(getIntent().getExtras()).get("LAO_ID"));

        setupLaoFragment();

        setupHomeButton();
        setupIdentityButton();

        // Subscribe to "open home" event
        mViewModel.getOpenHomeEvent().observe(this, booleanEvent -> {
            Boolean event = booleanEvent.getContentIfNotHandled();
            if (event != null) {
                setupHomeActivity();
            }
        });

        // Subscribe to "open identity" event
        mViewModel.getOpenIdentityEvent().observe(this, booleanEvent -> {
            Boolean event = booleanEvent.getContentIfNotHandled();
            if (event != null) {
                setupIdentityFragment();
            }
        });
    }

    @Override
    public void OnEventTypeSelectedListener(EventType eventType) {
        switch (eventType) {
            case MEETING:
                setupCreateMeetingFragment();
            case ROLL_CALL:
                setupCreateRollCallFragment();
            case POLL:
                setupCreatePollFragment();
            default:
        }
    }

    @Override
    public void onAddWitnessListener() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            // TODO: show QR Code scanning for adding witnesses
        } else {
            // TODO: show Camera permission for adding witnesses
        }
    }

    public static LaoDetailViewModel obtainViewModel(FragmentActivity activity) {
        ViewModelFactory factory = ViewModelFactory.getInstance(activity.getApplication());
        LaoDetailViewModel viewModel = new ViewModelProvider(activity, factory).get(LaoDetailViewModel.class);

        return viewModel;
    }

    public void setupHomeButton() {
        Button homeButton = (Button) findViewById(R.id.tab_home);

        homeButton.setOnClickListener(v -> mViewModel.openHome());
    }

    public void setupIdentityButton() {
        Button identityButton = (Button) findViewById(R.id.tab_identity);

        identityButton.setOnClickListener(v -> mViewModel.openIdentity());
    }

    private void setupLaoFragment() {
        LaoDetailFragment laoDetailFragment = (LaoDetailFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragment_lao_detail);
        if (laoDetailFragment == null) {
            laoDetailFragment = LaoDetailFragment.newInstance(this);
            ActivityUtils.replaceFragmentInActivity(
                    getSupportFragmentManager(), laoDetailFragment, R.id.fragment_container_lao_detail
            );
        }
    }

    private void setupHomeActivity() {
        Intent intent = new Intent(this, HomeActivity.class);
        setResult(HomeActivity.LAO_DETAIL_REQUEST_CODE, intent);
        finish();
    }

    private void setupIdentityFragment() {
        IdentityFragment identityFragment = (IdentityFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragment_identity);
        if (identityFragment == null) {
            identityFragment = IdentityFragment.newInstance();
            ActivityUtils.replaceFragmentInActivity(
                    getSupportFragmentManager(), identityFragment, R.id.fragment_container_lao_detail
            );
        }
    }

    private void setupCreateMeetingFragment() {
        MeetingEventCreationFragment meetingCreationFragment = (MeetingEventCreationFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragment_meeting_event_creation);
        if (meetingCreationFragment == null) {
            meetingCreationFragment = MeetingEventCreationFragment.newInstance();
            ActivityUtils.replaceFragmentInActivity(
                    getSupportFragmentManager(), meetingCreationFragment, R.id.fragment_container_lao_detail
            );
        }
    }

    private void setupCreateRollCallFragment() {
        RollCallEventCreationFragment rollCallCreationFragment = (RollCallEventCreationFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragment_create_roll_call_event);
        if (rollCallCreationFragment == null) {
            rollCallCreationFragment = RollCallEventCreationFragment.newInstance();
            ActivityUtils.replaceFragmentInActivity(
                    getSupportFragmentManager(), rollCallCreationFragment, R.id.fragment_container_lao_detail
            );
        }
    }

    private void setupCreatePollFragment() {
        PollEventCreationFragment pollCreationFragment = (PollEventCreationFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragment_organizer_poll);
        if (pollCreationFragment == null) {
            pollCreationFragment = PollEventCreationFragment.newInstance();
            ActivityUtils.replaceFragmentInActivity(
                    getSupportFragmentManager(), pollCreationFragment, R.id.fragment_container_lao_detail
            );
        }
    }
}
