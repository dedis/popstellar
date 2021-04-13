    package com.github.dedis.student20_pop.detail.fragments;
    import android.os.Bundle;
    import android.view.LayoutInflater;
    import android.view.View;
    import android.view.ViewGroup;
    import android.widget.TextView;

    import androidx.annotation.NonNull;
    import androidx.annotation.Nullable;
    import androidx.fragment.app.FragmentActivity;

    import com.github.dedis.student20_pop.databinding.FragmentElectionDisplayBinding;
    import com.github.dedis.student20_pop.detail.LaoDetailActivity;
    import com.github.dedis.student20_pop.detail.LaoDetailViewModel;

    public final class ElectionDisplayFragment extends AbstractEventCreationFragment {
        public static final String TAG =ElectionDisplayFragment.class.getSimpleName();
        public static ElectionDisplayFragment newInstance(){ return new ElectionDisplayFragment(); }

        @Nullable
        @Override
        public View onCreateView(
                @NonNull LayoutInflater inflater,
                @Nullable ViewGroup container,
                @Nullable Bundle savedInstanceState) {

            FragmentElectionDisplayBinding mElectionDisplayFragBinding =
                    FragmentElectionDisplayBinding.inflate(inflater,container, false);
            FragmentActivity activity = getActivity();
            LaoDetailViewModel mLaoDetailViewModel;
            if (activity instanceof LaoDetailActivity) {
                mLaoDetailViewModel = LaoDetailActivity.obtainViewModel(activity);
            } else {
                throw new IllegalArgumentException("Cannot obtain view model for " + TAG);
            }
            // Set the text widget in layout to current LAO name
            TextView laoNameTextView = mElectionDisplayFragBinding.LAONameElectionDisplay;
            laoNameTextView.setText(mLaoDetailViewModel.getCurrentLaoName().getValue());
            //////////////////////////////////////////////////////

            mElectionDisplayFragBinding.setLifecycleOwner(activity);
            return mElectionDisplayFragBinding.getRoot();
        }


    }
