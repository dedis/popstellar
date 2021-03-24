    package com.github.dedis.student20_pop.detail.fragments;
    import android.os.Bundle;
    import android.text.Editable;
    import android.text.TextWatcher;
    import android.view.LayoutInflater;
    import android.view.View;
    import android.view.ViewGroup;
    import android.widget.Button;
    import android.widget.EditText;
    import android.widget.TextView;

    import androidx.annotation.NonNull;
    import androidx.annotation.Nullable;
    import androidx.fragment.app.FragmentActivity;

    import com.github.dedis.student20_pop.databinding.FragmentCreateRollCallEventBinding;
    import com.github.dedis.student20_pop.databinding.FragmentElectionDisplayBinding;
    import com.github.dedis.student20_pop.detail.LaoDetailActivity;
    import com.github.dedis.student20_pop.detail.LaoDetailViewModel;
    import com.github.dedis.student20_pop.home.HomeActivity;

    import java.time.Instant;

    public final class ElectionDisplayFragment extends AbstractEventCreationFragment {
        public static final String TAG =ElectionDisplayFragment.class.getSimpleName();
        private TextView laoNameTextView;
        private LaoDetailViewModel mLaoDetailViewModel;
        private FragmentElectionDisplayBinding mElectionDisplayFragBinding;
        private Button electionNameButton1;
        public static ElectionDisplayFragment newInstance(){ return new ElectionDisplayFragment(); }

        @Override
        public void onActivityCreated (@Nullable Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
        }
        @Nullable
        @Override
        public View onCreateView(
                @NonNull LayoutInflater inflater,
                @Nullable ViewGroup container,
                @Nullable Bundle savedInstanceState) {

            mElectionDisplayFragBinding =
                    FragmentElectionDisplayBinding.inflate(inflater,container, false);
            FragmentActivity activity = getActivity();
            if (activity instanceof LaoDetailActivity) {
                mLaoDetailViewModel = LaoDetailActivity.obtainViewModel(activity);
            } else {
                throw new IllegalArgumentException("Cannot obtain view model for " + TAG);
            }
            // Set the text widget in layout to current LAO name
            laoNameTextView = mElectionDisplayFragBinding.LAONameElectionDisplay;
            laoNameTextView.setText(mLaoDetailViewModel.getCurrentLaoName().getValue());
            //////////////////////////////////////////////////////

            setUpElectionNameButton();

            mElectionDisplayFragBinding.setLifecycleOwner(activity);
            return mElectionDisplayFragBinding.getRoot();
        }

        private void setUpElectionNameButton() {
           /* mElectionDisplayFragBinding.electionName1.setOnClickListener(  v -> {
                mLaoDetailViewModel.castVotes();
            }); */
        }

    }
