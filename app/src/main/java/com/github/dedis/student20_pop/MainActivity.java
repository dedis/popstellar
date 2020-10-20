package com.github.dedis.student20_pop;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.Toast;

import com.github.dedis.student20_pop.attendeeUI.AttendeeFragment;
import com.github.dedis.student20_pop.attendeeUI.ExpandableListViewAdapter;
import com.github.dedis.student20_pop.model.Lao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


/**
 * Activity used to display the different UIs
**/
public final class MainActivity extends FragmentActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    ExpandableListViewAdapter listViewAdapter;
    ExpandableListView expandableListView;
    List<String> eventTypes;
    HashMap<String, List<String>> eventList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*
        expandableListView = findViewById(R.id.exp_list_view);

        showList();

        listViewAdapter = new ExpandableListViewAdapter(this, eventTypes, eventList);
        expandableListView.setAdapter(listViewAdapter);
        expandableListView.expandGroup(1);
        expandableListView.expandGroup(2);
        */

        if (findViewById(R.id.fragment_container) != null) {
            if (savedInstanceState != null) {
                return;
            }

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, new HomeFragment()).commit();
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
                showFragment(new HomeFragment(), HomeFragment.TAG);
                break;
            case R.id.tab_connect:
                //showFragment(new ConnectFragment(), ConnectFragment.TAG);
                showFragment(new AttendeeFragment(), AttendeeFragment.TAG);
                break;
            case R.id.tab_launch:
                showFragment(new LaunchFragment(), LaunchFragment.TAG);
                break;
            /*case R.id.button_launch:
                String name = ((EditText) findViewById(R.id.entry_box_launch)).getText().toString();
                // Creating the LAO but not sending the information for now
                Lao lao = new Lao(name);
                showFragment(new HomeFragment(), LaunchFragment.TAG);
                Toast.makeText(this,
                        getResources().getString(R.string.message_launch_successful, name),
                        Toast.LENGTH_LONG)
                        .show();
                break;
            case R.id.button_cancel_launch:
                ((EditText) findViewById(R.id.entry_box_launch)).getText().clear();
                showFragment(new HomeFragment(), LaunchFragment.TAG);
                break;
             */
            default:
        }
    }

    private void showFragment(Fragment fragment, String TAG) {
        if (!fragment.isVisible()) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment, TAG)
                    .addToBackStack(TAG)
                    .commit();
        }
    }


    /**
    private void showList(){
        eventTypes = new ArrayList<String>();
        eventList = new HashMap<String, List<String>>();

        eventTypes.add("Properties");
        eventTypes.add("Past Events");
        eventTypes.add("Present Events");
        eventTypes.add("Future Events");

        List<String> properties = new ArrayList<>();
        properties.add("first property");
        properties.add("another property");
        properties.add("another property");

        List<String> pastEvents = new ArrayList<>();
        pastEvents.add("past event 1");
        pastEvents.add("past event 2");

        List<String> presentEvents = new ArrayList<>();

        List<String> futureEvents = new ArrayList<>();
        futureEvents.add("future event 1");
        futureEvents.add("future event 2");

        eventList.put(eventTypes.get(0), properties);
        eventList.put(eventTypes.get(1), pastEvents);
        eventList.put(eventTypes.get(2), presentEvents);
        eventList.put(eventTypes.get(3), futureEvents);
    }
     **/



}
