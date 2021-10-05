package com.github.dedis.popstellar.ui.socialmedia;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.github.dedis.popstellar.databinding.SocialMediaFragmentBinding;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.ui.home.HomeActivity;
import com.github.dedis.popstellar.ui.home.HomeViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Fragment used to display Social media ui
 */
public final class SocialMediaFragment extends Fragment {

    public static final String TAG = SocialMediaFragment.class.getSimpleName();

    private SocialMediaFragmentBinding mSocialMediaFragBinding;
    private HomeViewModel mHomeViewModel;


    public static SocialMediaFragment newInstance() {
        return new SocialMediaFragment();
    }

    @Nullable
    @Override
    public View onCreateView(
        @NonNull LayoutInflater inflater,
        @Nullable ViewGroup container,
        @Nullable Bundle savedInstanceData) {
      mSocialMediaFragBinding = SocialMediaFragmentBinding.inflate(inflater, container, false);

      mHomeViewModel = HomeActivity.obtainViewModel(getActivity());

      mSocialMediaFragBinding.setViewModel(mHomeViewModel);
      mSocialMediaFragBinding.setLifecycleOwner(getActivity());

      return mSocialMediaFragBinding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setupSendButton();
    }

    private void setupSendButton() {
        mSocialMediaFragBinding.buttonSend.setOnClickListener(v -> mHomeViewModel.sendNewChirp());
    }
}
