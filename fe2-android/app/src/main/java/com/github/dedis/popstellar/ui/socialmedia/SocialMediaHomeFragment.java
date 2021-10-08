package com.github.dedis.popstellar.ui.socialmedia;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.SocialMediaHomeFragmentBinding;

public class SocialMediaHomeFragment extends Fragment {

    private static final String TAG = SocialMediaHomeFragment.class.getSimpleName();

    private SocialMediaHomeFragmentBinding mSocialMediaHomeFragBinding;
    private SocialMediaViewModel mSocialMediaViewModel;

    public static SocialMediaHomeFragment newInstance() {
        return new SocialMediaHomeFragment();
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        mSocialMediaHomeFragBinding = SocialMediaHomeFragmentBinding.inflate(inflater, container, false);

        mSocialMediaViewModel = SocialMediaActivity.obtainViewModel(getActivity());

        mSocialMediaHomeFragBinding.setViewModel(mSocialMediaViewModel);
        mSocialMediaHomeFragBinding.setLifecycleOwner(getActivity());

        return mSocialMediaHomeFragBinding.getRoot();
    }
}