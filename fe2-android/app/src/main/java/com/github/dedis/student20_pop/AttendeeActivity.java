package com.github.dedis.student20_pop;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import com.github.dedis.student20_pop.ui.AttendeeFragment;
import com.github.dedis.student20_pop.ui.IdentityFragment;

/** Activity used to display the different UIs for attendees */
@Deprecated
public class AttendeeActivity extends FragmentActivity {

  public static final String TAG = AttendeeActivity.class.getSimpleName();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_attendee);

    Intent intent = getIntent();

    if (findViewById(R.id.fragment_container_attendee) != null) {
      if (savedInstanceState != null) {
        return;
      }

      getSupportFragmentManager()
          .beginTransaction()
          .add(R.id.fragment_container_attendee, new AttendeeFragment())
          .commit();
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
        // Future: different Home UI for organizer (without connect UI?)
        Intent mainActivityIntent = new Intent(this, MainActivity.class);
        startActivity(mainActivityIntent);
        break;
      case R.id.tab_identity:
        showFragment(new IdentityFragment(), IdentityFragment.TAG);
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
}
