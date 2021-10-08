package com.github.dedis.popstellar.ui.socialmedia;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.SocialMediaSendFragmentBinding;

public class SocialMediaSendFragment extends Fragment {

    private static final String TAG = SocialMediaSendFragment.class.getSimpleName();

    private SocialMediaSendFragmentBinding mSocialMediaSendFragBinding;
    private SocialMediaViewModel mSocialMediaViewModel;

    public static SocialMediaSendFragment newInstance() {
        return new SocialMediaSendFragment();
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        mSocialMediaSendFragBinding = SocialMediaSendFragmentBinding.inflate(inflater, container, false);

        mSocialMediaViewModel = SocialMediaActivity.obtainViewModel(getActivity());

        mSocialMediaSendFragBinding.setViewModel(mSocialMediaViewModel);
        mSocialMediaSendFragBinding.setLifecycleOwner(getActivity());

        return mSocialMediaSendFragBinding.getRoot();
    }
}