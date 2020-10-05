package com.github.dedis.student20_pop;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import android.os.Bundle;
import android.view.View;

import com.github.dedis.student20_pop.ui.ConnectFragment;
import com.github.dedis.student20_pop.ui.HomeFragment;
import com.github.dedis.student20_pop.ui.LaunchFragment;

/**
 * Activity used to display the different UIs
**/
public final class MainActivity extends FragmentActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (findViewById(R.id.fragment_container) != null) {
            if (savedInstanceState != null) {
                return;
            }

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, new HomeFragment()).commit();
        }
    }

    public void onClick(View view) {
        switch(view.getId()) {
            case R.id.tab_home:
                showFragment(new HomeFragment(), HomeFragment.TAG);
                break;
            case R.id.tab_connect:
                showFragment(new ConnectFragment(), ConnectFragment.TAG);
                break;
            case R.id.tab_launch:
                showFragment(new LaunchFragment(), LaunchFragment.TAG);
                break;
            default:
        }
    }

    private void showFragment(Fragment fragment, String TAG) {
        if (!fragment.isVisible()) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment, TAG)
                    .addToBackStack(TAG).commit();
        }
    }
}
