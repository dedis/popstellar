package com.github.dedis.popstellar.ui.digitalcash;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager.widget.ViewPager;

import android.app.ActionBar;
import android.os.Bundle;

import com.github.dedis.popstellar.R;

public class DigitalCashMain extends FragmentActivity {
    DigitalCashPageAdapter digitalCashPageAdapter;
    ViewPager viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_digital_cash_main);

        ViewModel digiviewmodel = new ViewModelProvider(this).get(DigitalCashViewModel.class);

        digitalCashPageAdapter = new DigitalCashPageAdapter(getSupportFragmentManager());
        viewPager = (ViewPager) findViewById(R.id.pager);
        viewPager.setAdapter(digitalCashPageAdapter);
    }

}