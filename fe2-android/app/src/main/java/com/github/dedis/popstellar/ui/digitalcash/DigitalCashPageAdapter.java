package com.github.dedis.popstellar.ui.digitalcash;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

public class DigitalCashPageAdapter extends FragmentStatePagerAdapter {
    public DigitalCashPageAdapter(FragmentManager fm ){
        super(fm);
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        Fragment fragment;
        Bundle args = new Bundle();
        switch (position){
            case 0:
                fragment = new DigitalCashHome();
                args.putInt(DigitalCashReceive.ARG, position+1);
                fragment.setArguments(args);
                return fragment;
            case 1:
                fragment = new DigitalCashReceive();
                args.putInt(DigitalCashReceive.ARG, position+1);
                fragment.setArguments(args);
                return fragment;
            default :
                fragment = new DigitalCashSend();
                args.putInt(DigitalCashSend.ARG, position+1);
                fragment.setArguments(args);
        }

        return fragment;
    }

    @Override
    public int getCount() {
        return 3;
    }

    @Override
    public CharSequence getPageTitle(int position){
        return "item" + (position+1);
    }
}
