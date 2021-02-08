package com.github.dedis.student20_pop.home;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.github.dedis.student20_pop.Event;
import com.github.dedis.student20_pop.OrganizerActivity;
import com.github.dedis.student20_pop.R;
import com.github.dedis.student20_pop.ViewModelFactory;
import com.github.dedis.student20_pop.ui.HomeFragment;
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

    public void openLaoDetails(String laoId) {
        Intent intent = new Intent(this, OrganizerActivity.class);
        intent.putExtra("LAO_ID", laoId);
        startActivity(intent);
    }


}
