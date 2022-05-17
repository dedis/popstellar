package com.github.dedis.popstellar.ui.digitalcash;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.Menu;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;

import com.github.dedis.popstellar.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

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

    setupNavigationBar();
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
    BottomNavigationView bottomNavigationView = findViewById(R.id.digital_cash_nav_bar);
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
}
