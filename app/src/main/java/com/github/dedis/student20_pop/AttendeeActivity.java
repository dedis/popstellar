package com.github.dedis.student20_pop;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.github.dedis.student20_pop.ui.AttendeeFragment;
import com.github.dedis.student20_pop.ui.HomeFragment;
import com.github.dedis.student20_pop.utility.ui.OnEventTypeSelectedListener;

/**
 * Activity used to display the different UIs for attendees
 **/
public class AttendeeActivity extends FragmentActivity implements OnEventTypeSelectedListener {

    public static final String TAG = AttendeeActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_attendee);

        //TODO: retrieve lao/PopContext
        Intent intent = getIntent();

        if (findViewById(R.id.fragment_container_attendee) != null) {
            if (savedInstanceState != null) {
                return;
            }

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container_attendee, new AttendeeFragment()).commit();
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
                //Future: different Home UI for organizer (without connect UI?)
                showFragment(new HomeFragment(), HomeFragment.TAG);
                break;
            default:
                break;
        }
    }

    private void showFragment(Fragment fragment, String TAG) {
        if (!fragment.isVisible()) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container_attendee, fragment, TAG)
                    .addToBackStack(TAG)
                    .commit();
        }
    }

    @Override
    public void OnEventTypeSelectedListener(EventType eventType) {
        switch (eventType) {
            case MEETING:
                //TODO
                Log.d("Meeting Event Type ", "Launch here Meeting Event Creation Fragment");
                break;
            case ROLL_CALL:
                //TODO
                Log.d("Roll-Call Event Type ", "Launch here Roll-Call Event Creation Fragment");
                break;
            case POLL:
                //TODO
                Log.d("Poll Event Type ", "Launch here Poll Event Creation Fragment");
                break;
            default:
                Log.d("Default Event Type :", "Default Behaviour TBD");
                break;
        }
    }
}
