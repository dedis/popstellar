package com.github.dedis.popstellar.ui.digitalcash;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.utility.ActivityUtils;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.function.Supplier;

import dagger.hilt.android.AndroidEntryPoint;

/** Activity for the digital cash */
@AndroidEntryPoint
public class DigitalCashMain extends AppCompatActivity {
  private DigitalCashViewModel mViewModel;
  public static final String TAG = DigitalCashMain.class.getSimpleName();

  public static final String LAO_ID = "LAO_ID";
  public static final String LAO_NAME = "LAO_NAME";
  public static final String ROLL_CALL_ID = "ROLL_CALL_ID";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.digital_cash_main_activity);

    mViewModel = obtainViewModel(this);
    mViewModel.setLaoId((String) getIntent().getExtras().get(LAO_ID));
    mViewModel.setLaoName((String) getIntent().getExtras().get(LAO_NAME));
    mViewModel.setRollCallId((String) getIntent().getExtras().get(ROLL_CALL_ID));

    setupNavigationBar();
    // Subscribe to "open home"
    mViewModel
        .getOpenHomeEvent()
        .observe(
            this,
            booleanEvent -> {
              Log.d(TAG, "Open digital cash home Fragment");
              Boolean event = booleanEvent.getContentIfNotHandled();
              if (event != null) {
                setupFragment(R.id.fragment_digital_cash_home);
              }
            });

    // Subscribe to "open history"
    mViewModel
        .getOpenHistoryEvent()
        .observe(
            this,
            booleanEvent -> {
              Log.d(TAG, "Open digital cash history Fragment");
              Boolean event = booleanEvent.getContentIfNotHandled();
              if (event != null) {
                setupFragment(R.id.fragment_digital_cash_history);
              }
            });

    // Subscribe to "open send"
    mViewModel
        .getOpenSendEvent()
        .observe(
            this,
            booleanEvent -> {
              Log.d(TAG, "Open digital cash send Fragment");
              Boolean event = booleanEvent.getContentIfNotHandled();
              if (event != null) {
                setupFragment(R.id.fragment_digital_cash_send);
              }
            });

    // Subscribe to "open receive"
    mViewModel
        .getOpenReceiveEvent()
        .observe(
            this,
            booleanEvent -> {
              Log.d(TAG, "Open digital cash receive Fragment");
              Boolean event = booleanEvent.getContentIfNotHandled();
              if (event != null) {
                setupFragment(R.id.fragment_digital_cash_receive);
              }
            });

    // Subscribe to "open issue"
    mViewModel
        .getOpenIssueEvent()
        .observe(
            this,
            booleanEvent -> {
              Log.d(TAG, "Open digital cash issue Fragment");
              Boolean event = booleanEvent.getContentIfNotHandled();

              if (event != null) {
                Log.d(
                    TAG,
                    "the key from organizer is : "
                        + mViewModel.getCurrentLao().getOrganizer().getEncoded());
                Log.d(
                    TAG,
                    "the key from the person is "
                        + mViewModel.getKeyManager().getMainPublicKey().getEncoded());
                if (!mViewModel
                    .getCurrentLao()
                    .getOrganizer()
                    .getEncoded()
                    .equals(mViewModel.getKeyManager().getMainPublicKey().getEncoded())) {
                  Toast.makeText(this, "You have to be the Lao Organizer", Toast.LENGTH_SHORT)
                      .show();
                } else {

                  setupFragment(R.id.fragment_digital_cash_issue);
                }
              }
            });

    // Subscribe to "open receipt"
    mViewModel
        .getOpenReceiptEvent()
        .observe(
            this,
            booleanEvent -> {
              Log.d(TAG, "Open digital cash receipt Fragment");
              Boolean event = booleanEvent.getContentIfNotHandled();
              if (event != null) {
                setupFragment(R.id.fragment_digital_cash_receipt);
              }
            });

    mViewModel.openHome();
  }

  public static DigitalCashViewModel obtainViewModel(FragmentActivity activity) {
    return new ViewModelProvider(activity).get(DigitalCashViewModel.class);
  }

  @SuppressLint("NonConstantResourceId")
  public void setupNavigationBar() {
    BottomNavigationView bottomNavigationView = findViewById(R.id.digital_cash_nav_bar);
    bottomNavigationView.setOnItemSelectedListener(
        item -> {
          int id = item.getItemId();
          if (id == R.id.home_coin) {
            mViewModel.openHome();
          } else if (id == R.id.history_coin) {
            mViewModel.openHistory();
          } else if (id == R.id.send_coin) {
            mViewModel.openSend();
          } else if (id == R.id.receive_coin) {
            mViewModel.openReceive();
          } else if (id == R.id.issue_coin) {
            mViewModel.openIssue();
          }
          return true;
        });
  }

  public void setupFragment(int id) {
    if (id == R.id.fragment_digital_cash_home) {
      setCurrentFragment(R.id.fragment_digital_cash_home, DigitalCashHomeFragment::newInstance);
    } else if (id == R.id.fragment_digital_cash_history) {
      setCurrentFragment(
          R.id.fragment_digital_cash_history, DigitalCashHistoryFragment::newInstance);
    } else if (id == R.id.fragment_digital_cash_send) {
      setCurrentFragment(R.id.fragment_digital_cash_send, DigitalCashSendFragment::newInstance);
    } else if (id == R.id.fragment_digital_cash_receive) {
      setCurrentFragment(
          R.id.fragment_digital_cash_receive, DigitalCashReceiveFragment::newInstance);
    } else if (id == R.id.fragment_digital_cash_issue) {
      setCurrentFragment(R.id.fragment_digital_cash_issue, DigitalCashIssueFragment::newInstance);
    } else if (id == R.id.fragment_digital_cash_receipt) {
      setCurrentFragment(
          R.id.fragment_digital_cash_receipt, DigitalCashReceiptFragment::newInstance);
    }
  }

  /**
   * Set the current fragment in the container of the activity
   *
   * @param id of the fragment
   * @param fragmentSupplier provides the fragment if it is missing
   */
  private void setCurrentFragment(@IdRes int id, Supplier<Fragment> fragmentSupplier) {
    Fragment fragment = getSupportFragmentManager().findFragmentById(id);
    // If the fragment was not created yet, create it now
    if (fragment == null) fragment = fragmentSupplier.get();

    // Set the new fragment in the container
    ActivityUtils.replaceFragmentInActivity(
        getSupportFragmentManager(), fragment, R.id.fragment_container_digital_cash);
  }
}
