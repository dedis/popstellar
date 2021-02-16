package com.github.dedis.student20_pop.detail;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;

import com.github.dedis.student20_pop.R;
import com.github.dedis.student20_pop.ViewModelFactory;
import com.github.dedis.student20_pop.home.HomeActivity;
import com.github.dedis.student20_pop.model.entities.LAOEntity;
import com.github.dedis.student20_pop.ui.IdentityFragment;
import com.github.dedis.student20_pop.ui.OrganizerFragment;
import com.github.dedis.student20_pop.utility.ActivityUtils;

import java.util.Objects;

public class LaoDetailActivity extends AppCompatActivity {

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
        setupPropertiesButton();

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

        // Subscribe to "open properties" event
        mViewModel.getOpenPropertiesEvent().observe(this, booleanEvent -> {
            Boolean event = booleanEvent.getContentIfNotHandled();
            if (event != null) {
                setupPropertiesFragment();
            }
        });
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

    public void setupPropertiesButton() {
        Button propertiesButton = (Button) findViewById(R.id.tab_properties);

        propertiesButton.setOnClickListener(v -> mViewModel.openProperties());
    }

    private void setupLaoFragment() {
        if(mViewModel.isOrganizer()) {
            setupOrganizerFragment(mViewModel.getCurrentLao());
        }
        else {
            setupAttendeeFragment(mViewModel.getCurrentLao());
        }
    }

    private void setupOrganizerFragment(LAOEntity laoEntity) {
        OrganizerFragment organizerFragment = (OrganizerFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_organizer);
        if (organizerFragment == null) {
            organizerFragment = OrganizerFragment.newInstance(laoEntity);
            ActivityUtils.replaceFragmentInActivity(
                    getSupportFragmentManager(), organizerFragment, R.id.fragment_container_lao_detail
            );
        }
    }

    private void setupAttendeeFragment(LAOEntity laoEntity) {
        OrganizerFragment organizerFragment = (OrganizerFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_attendee);
        if (organizerFragment == null) {
            organizerFragment = OrganizerFragment.newInstance(laoEntity);
            ActivityUtils.replaceFragmentInActivity(
                    getSupportFragmentManager(), organizerFragment, R.id.fragment_container_lao_detail
            );
        }
    }

    private void setupHomeActivity() {
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
    }

    private void setupIdentityFragment() {
        IdentityFragment identityFragment = (IdentityFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_identity);
        if (identityFragment == null) {
            identityFragment = IdentityFragment.newInstance();
            ActivityUtils.replaceFragmentInActivity(
                    getSupportFragmentManager(), identityFragment, R.id.fragment_container_lao_detail
            );
        }
    }

    private void setupPropertiesFragment() {
        //TODO: have identity separated from properties tabs
    }
}
