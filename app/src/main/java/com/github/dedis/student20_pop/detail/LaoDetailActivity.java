package com.github.dedis.student20_pop.detail;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;

import com.github.dedis.student20_pop.R;
import com.github.dedis.student20_pop.ViewModelFactory;
import com.github.dedis.student20_pop.home.HomeViewModel;
import com.github.dedis.student20_pop.model.Lao;
import com.github.dedis.student20_pop.ui.OrganizerFragment;
import com.github.dedis.student20_pop.utility.ActivityUtils;

import java.net.URI;
import java.util.Objects;

public class LaoDetailActivity extends AppCompatActivity {

    private LaoDetailViewModel mViewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lao_detail);

        String laoId = (String) Objects.requireNonNull(getIntent().getExtras()).get("LAO_ID");

        setupLaoFragment(laoId);

        mViewModel = obtainViewModel(this);


    }

    public static LaoDetailViewModel obtainViewModel(FragmentActivity activity) {
        ViewModelFactory factory = ViewModelFactory.getInstance(activity.getApplication());
        LaoDetailViewModel viewModel = new ViewModelProvider(activity, factory).get(LaoDetailViewModel.class);

        return viewModel;
    }

    private void setupLaoFragment(String laoId) {
        //TODO: look for LAO in db
        //TODO: figure if attendee or organizer, how to figure out user id??
        Lao lao = new Lao("name", "id", URI.create("host"));
        boolean organizer = true;
        if(organizer) setupOrganizerFragment(lao);
        else setupAttendeeFragment(lao);
    }

    private void setupOrganizerFragment(Lao lao) {
        OrganizerFragment organizerFragment = (OrganizerFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_organizer);
        if (organizerFragment == null) {
            organizerFragment = OrganizerFragment.newInstance(lao);
            ActivityUtils.replaceFragmentInActivity(
                    getSupportFragmentManager(), organizerFragment, R.id.fragment_container_lao_detail
            );
        }
    }

    private void setupAttendeeFragment(Lao lao) {
        OrganizerFragment organizerFragment = (OrganizerFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_attendee);
        if (organizerFragment == null) {
            organizerFragment = OrganizerFragment.newInstance(lao);
            ActivityUtils.replaceFragmentInActivity(
                    getSupportFragmentManager(), organizerFragment, R.id.fragment_container_lao_detail
            );
        }
    }
}
