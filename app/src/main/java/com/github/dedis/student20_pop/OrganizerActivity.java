package com.github.dedis.student20_pop;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.github.dedis.student20_pop.ui.HomeFragment;
import com.github.dedis.student20_pop.ui.OrganizerFragment;
import com.github.dedis.student20_pop.utility.ui.OnEventTypeSelectedListener;

public class OrganizerActivity extends FragmentActivity implements OnEventTypeSelectedListener  {

    private static final String TAG = OrganizerActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_organizer);
        if (findViewById(R.id.fragment_container_organizer) != null) {
            if (savedInstanceState != null) {
                return;
            }

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container_organizer, new OrganizerFragment()).commit();
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
                //Future: different Home UI for organizer (without connect)
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
                    .replace(R.id.fragment_container_organizer, fragment, TAG)
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
