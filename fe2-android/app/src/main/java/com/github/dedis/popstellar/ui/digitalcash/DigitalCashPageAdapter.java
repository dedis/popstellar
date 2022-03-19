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
        Fragment send_fragment = new DigitalCashSend();
        Bundle args = new Bundle();
        args.putInt(DigitalCashSend.ARG, position+1);
        send_fragment.setArguments(args);
        return send_fragment;
    }

    @Override
    public int getCount() {
        return 2;
    }

    @Override
    public CharSequence getPageTitle(int position){
        return "item" + (position+1);
    }
}
