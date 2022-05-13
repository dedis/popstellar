package com.github.dedis.popstellar.ui.digitalcash;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;

import androidx.annotation.IdRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.ui.detail.LaoDetailActivity;
import com.github.dedis.popstellar.utility.ActivityUtils;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import dagger.hilt.android.AndroidEntryPoint;

/** Activity for the social media */
@AndroidEntryPoint
public class DigitalCashMain extends AppCompatActivity {

  public static final String TAG = DigitalCashMain.class.getSimpleName();

  public static final String OPENED_FROM = "OPENED_FROM";
  public static final String LAO_ID = "LAO_ID";
  public static final String LAO_NAME = "LAO_NAME";
  private DigitalCashViewModel mViewModel;


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.digital_cash_main_activity);
    mViewModel = obtainViewModel(this);

    // When we launch the social media from a lao, it directly sets its id and name
    if (getIntent().getExtras().get(OPENED_FROM).equals(LaoDetailActivity.class.getSimpleName())) {
      mViewModel.setLaoId((String) getIntent().getExtras().get(LAO_ID));
      mViewModel.setLaoName((String) getIntent().getExtras().get(LAO_NAME));
    }
    setupDigitalCashHomeFragment();
    setupNavigationBar();

    // Subscribe to "lao name" string
    mViewModel
        .getLaoName()
        .observe(
            this,
            newLaoName -> {
              if (newLaoName != null) {
                Objects.requireNonNull(getSupportActionBar())
                    .setTitle(String.format("popstellar - %s", newLaoName));
              }
            });

    mViewModel.setLaoName((String) getIntent().getExtras().get(LAO_NAME));

    // Subscribe to "open home"
    mViewModel
        .getOpenHomeEvent()
        .observe(
            this,
            booleanEvent -> {
              Log.d(TAG, "@string/digital_cash_home_fragment");
              Boolean event = booleanEvent.getContentIfNotHandled();
              if (event != null) {
                setupDigitalCashHomeFragment();
              }
            });

    // Subscribe to "open history"
    mViewModel
        .getOpenHistoryEvent()
        .observe(
            this,
            booleanEvent -> {
              Log.d(TAG, "@string/digital_cash_history_fragment");
              Boolean event = booleanEvent.getContentIfNotHandled();
              if (event != null) {
                setupDigitalCashHistoryFragment();
              }
            });

    // Subscribe to "open send"
    mViewModel
        .getOpenSendEvent()
        .observe(
            this,
            booleanEvent -> {
              Log.d(TAG, "@string/digital_cash_send_fragment");
              Boolean event = booleanEvent.getContentIfNotHandled();
              if (event != null) {
                setupDigitalCashSendFragment();
              }
            });

    // Subscribe to "open receive"
    mViewModel
        .getOpenReceiveEvent()
        .observe(
            this,
            booleanEvent -> {
              Log.d(TAG, "@string/digital_cash_receive_fragment");
              Boolean event = booleanEvent.getContentIfNotHandled();
              if (event != null) {
                setupDigitalCashReceiveFragment();
              }
            });

    // Subscribe to "open issue"
    mViewModel
        .getOpenIssueEvent()
        .observe(
            this,
            booleanEvent -> {
              Log.d(TAG, "@string/digital_cash_issue_fragment");
              Boolean event = booleanEvent.getContentIfNotHandled();
              if (event != null) {
                setupDigitalCashIssueFragment();
              }
            });
  }

  public static DigitalCashViewModel obtainViewModel(FragmentActivity activity) {
    return new ViewModelProvider(activity).get(DigitalCashViewModel.class);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.digital_cash_menu, menu);

    // Get the submenu and clear its unique item. The item was needed to create the submenu
    SubMenu laosList = menu.findItem(R.id.laos_list).getSubMenu();
    // Adding all currently opened lao name to the submenu
    mViewModel
        .getLAOs()
        .observe(
            this,
            list -> {
              if (list != null) {
                laosList.clear();
                for (int i = 0; i < list.size(); ++i) {
                  // Creating a unique id using the index of the lao within the list
                  laosList.add(Menu.NONE, i, Menu.CATEGORY_CONTAINER, list.get(i).getName());
                }
              }
            });

    return true;
  }

  @SuppressLint("NonConstantResourceId")
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Retrieve the index of the lao within the list
    int i = item.getItemId();
    List<Lao> laos = mViewModel.getLAOs().getValue();
    if (laos != null && i >= 0 && i < laos.size()) {
      Lao lao = laos.get(i);
      mViewModel.setLaoId(lao.getId());
      mViewModel.setLaoName(lao.getName());
      return true;
    }
    return super.onOptionsItemSelected(item);
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

  public void setupDigitalCashHomeFragment() {
    setCurrentFragment(R.id.fragment_digital_cash_home, DigitalCashHomeFragment::newInstance);
  }

  public void setupDigitalCashSendFragment() {
    setCurrentFragment(R.id.fragment_digital_cash_send, DigitalCashSendFragment::newInstance);
  }

  public void setupDigitalCashHistoryFragment() {
    setCurrentFragment(R.id.fragment_digital_cash_history, DigitalCashHistoryFragment::newInstance);
  }

  public void setupDigitalCashReceiveFragment() {
    setCurrentFragment(R.id.fragment_digital_cash_receive, DigitalCashReceiveFragment::newInstance);
  }

  public void setupDigitalCashIssueFragment() {
    setCurrentFragment(R.id.fragment_digital_cash_issue, DigitalCashIssueFragment::newInstance);
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
