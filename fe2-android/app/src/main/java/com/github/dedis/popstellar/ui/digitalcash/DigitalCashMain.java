package com.github.dedis.popstellar.ui.digitalcash;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.Menu;

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

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.digital_cash_main_activity);

    mViewModel = obtainViewModel(this);

    setupFragment(R.id.fragment_digital_cash_home);

    // Subscribe to "open home"
    mViewModel
        .getOpenHomeEvent()
        .observe(
            this,
            booleanEvent -> {
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
              Boolean event = booleanEvent.getContentIfNotHandled();
              if (event != null) {
                setupFragment(R.id.fragment_digital_cash_issue);
              }
            });

    // Subscribe to "open receipt"
    mViewModel
        .getOpenIssueEvent()
        .observe(
            this,
            booleanEvent -> {
              Boolean event = booleanEvent.getContentIfNotHandled();
              if (event != null) {
                setupFragment(R.id.fragment_digital_cash_receipt);
              }
            });
  }

  public static DigitalCashViewModel obtainViewModel(FragmentActivity activity) {
    return new ViewModelProvider(activity).get(DigitalCashViewModel.class);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.digital_cash_menu, menu);
    return true;
  }

  @SuppressLint("NonConstantResourceId")
  public void setupNavigationBar() {
    BottomNavigationView bottomNavigationView = findViewById(R.id.social_media_nav_bar);
    bottomNavigationView.setOnItemSelectedListener(
        item -> {
          switch (item.getItemId()) {
            case R.id.home_coin:
              mViewModel.openHome();
              break;
            case R.id.history_coin:
              mViewModel.openHistory();
              break;
            case R.id.send_coin_m:
              mViewModel.openSend();
              break;
            case R.id.receive_coin_m:
              mViewModel.openReceive();
              break;
            case R.id.issue_coin:
              mViewModel.openIssue();
              break;
            default:
          }
          return true;
        });
  }

  public void setupFragment(int id) {
    switch (id) {
      case R.id.fragment_digital_cash_home:
        setCurrentFragment(R.id.fragment_digital_cash_home, DigitalCashHomeFragment::newInstance);
        break;
      case R.id.fragment_digital_cash_history:
        setCurrentFragment(
            R.id.fragment_digital_cash_history, DigitalCashHistoryFragment::newInstance);
        break;
      case R.id.fragment_digital_cash_send:
        setCurrentFragment(R.id.fragment_digital_cash_send, DigitalCashSendFragment::newInstance);
        break;
      case R.id.fragment_digital_cash_receive:
        setCurrentFragment(
            R.id.fragment_digital_cash_receive, DigitalCashReceiveFragment::newInstance);
        break;
      case R.id.fragment_digital_cash_issue:
        setCurrentFragment(R.id.fragment_digital_cash_issue, DigitalCashIssueFragment::newInstance);
        break;
      case R.id.fragment_digital_cash_receipt:
        setCurrentFragment(R.id.fragment_digital_cash_receipt, DigitalCashReceipt::newInstance);
        break;
      default:
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
        getSupportFragmentManager(), fragment, R.id.fragment_container_social_media);
  }
}
