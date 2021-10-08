package com.github.dedis.popstellar.ui.socialmedia;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;
import android.view.MenuItem;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.ViewModelFactory;
import com.github.dedis.popstellar.utility.ActivityUtils;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class SocialMediaActivity extends AppCompatActivity {
    
    private SocialMediaViewModel mViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.social_media_activity);

        mViewModel = obtainViewModel(this);

        setupNavigationBar();
        setupSocialMediaHomeFragment();

        //Subscribe to "open home" event
        mViewModel.getOpenHomeEvent()
            .observe(
                this,
                booleanEvent -> {
                    Boolean event = booleanEvent.getContentIfNotHandled();
                    if (event != null) {
                        setupSocialMediaHomeFragment();
                    }
                }
            );

        //Subscribe to "open send" event
        mViewModel.getOpenSendEvent()
                .observe(
                        this,
                        booleanEvent -> {
                            Boolean event = booleanEvent.getContentIfNotHandled();
                            if (event != null) {
                                setupSocialMediaSendFragment();
                            }
                        }
                );

        //Subscribe to "open following" event
        mViewModel.getOpenFollowingEvent()
                .observe(
                        this,
                        booleanEvent -> {
                            Boolean event = booleanEvent.getContentIfNotHandled();
                            if (event != null) {
                                setupSocialMediaFollowingFragment();
                            }
                        }
                );

        //Subscribe to "open profile" event
        mViewModel.getOpenProfileEvent()
                .observe(
                        this,
                        booleanEvent -> {
                            Boolean event = booleanEvent.getContentIfNotHandled();
                            if (event != null) {
                                setupSocialMediaProfileFragment();
                            }
                        }
                );

    }

    public static SocialMediaViewModel obtainViewModel(FragmentActivity activity) {
        ViewModelFactory factory = ViewModelFactory.getInstance(activity.getApplication());
        SocialMediaViewModel viewModel = new ViewModelProvider(activity, factory).get(SocialMediaViewModel.class);
        return viewModel;
    }

    public void setupNavigationBar() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.social_media_nav_bar);
        bottomNavigationView.setOnNavigationItemSelectedListener(listener);
    }

    private void setupSocialMediaHomeFragment() {
        SocialMediaHomeFragment socialMediaHomeFragment =
            (SocialMediaHomeFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_social_media_home);
        if (socialMediaHomeFragment == null) {
            socialMediaHomeFragment = SocialMediaHomeFragment.newInstance();
            ActivityUtils.replaceFragmentInActivity(
                getSupportFragmentManager(), socialMediaHomeFragment, R.id.fragment_container_social_media);
        }
    }

    private void setupSocialMediaSendFragment() {
        SocialMediaSendFragment socialMediaSendFragment =
                (SocialMediaSendFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_social_media_send);
        if (socialMediaSendFragment == null) {
            socialMediaSendFragment = SocialMediaSendFragment.newInstance();
            ActivityUtils.replaceFragmentInActivity(
                    getSupportFragmentManager(), socialMediaSendFragment, R.id.fragment_container_social_media);
        }
    }

    private void setupSocialMediaFollowingFragment() {
        SocialMediaFollowingFragment socialMediaFollowingFragment =
                (SocialMediaFollowingFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_social_media_following);
        if (socialMediaFollowingFragment == null) {
            socialMediaFollowingFragment = SocialMediaFollowingFragment.newInstance();
            ActivityUtils.replaceFragmentInActivity(
                    getSupportFragmentManager(), socialMediaFollowingFragment, R.id.fragment_container_social_media);
        }
    }

    private void setupSocialMediaProfileFragment() {
        SocialMediaProfileFragment socialMediaProfileFragment =
                (SocialMediaProfileFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_social_media_profile);
        if (socialMediaProfileFragment == null) {
            socialMediaProfileFragment = SocialMediaProfileFragment.newInstance();
            ActivityUtils.replaceFragmentInActivity(
                    getSupportFragmentManager(), socialMediaProfileFragment, R.id.fragment_container_social_media);
        }
    }

    private BottomNavigationView.OnNavigationItemSelectedListener listener = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {

                case R.id.social_media_home:
                    mViewModel.openHome();
                    return true;

                case R.id.social_media_send:
                    mViewModel.openSend();
                    return true;

                case R.id.social_media_following:
                    mViewModel.openFollowing();
                    return true;

                case R.id.social_media_profile:
                    mViewModel.openProfile();
                    return true;
            }
            return false;
        }
    };
}