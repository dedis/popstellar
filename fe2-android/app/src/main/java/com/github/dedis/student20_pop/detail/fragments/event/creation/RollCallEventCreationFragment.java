package com.github.dedis.student20_pop.detail.fragments.event.creation;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.dedis.student20_pop.databinding.FragmentCreateRollCallEventBinding;
import com.github.dedis.student20_pop.detail.LaoDetailActivity;
import com.github.dedis.student20_pop.detail.LaoDetailViewModel;
import com.github.dedis.student20_pop.model.event.EventType;

/** Fragment that shows up when user wants to create a Roll-Call Event */
public final class RollCallEventCreationFragment extends AbstractEventCreationFragment {

    public static final String TAG = RollCallEventCreationFragment.class.getSimpleName();

    private FragmentCreateRollCallEventBinding mFragBinding;
    private LaoDetailViewModel mLaoDetailViewModel;
    private EditText rollCallTitleEditText;
    private Button confirmButton;
    private Button openButton;

    private final TextWatcher confirmTextWatcher =
            new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    // Nothing needed here
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String meetingTitle = rollCallTitleEditText.getText().toString().trim();
                    boolean areFieldsFilled =
                            !meetingTitle.isEmpty() && !getStartDate().isEmpty() && !getStartTime().isEmpty();
                    confirmButton.setEnabled(areFieldsFilled);
                    openButton.setEnabled(areFieldsFilled);
                }

                @Override
                public void afterTextChanged(Editable s) {
                    // Nothing needed here
                }
            };

    public static RollCallEventCreationFragment newInstance() {
        return new RollCallEventCreationFragment();
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        mFragBinding = FragmentCreateRollCallEventBinding.inflate(inflater, container, false);

        mLaoDetailViewModel = LaoDetailActivity.obtainViewModel(getActivity());

        setDateAndTimeView(mFragBinding.getRoot(), this, getFragmentManager());
        addStartDateAndTimeListener(confirmTextWatcher);

        rollCallTitleEditText = mFragBinding.rollCallTitleText;
        openButton = mFragBinding.rollCallOpen;
        confirmButton = mFragBinding.rollCallConfirm;

        mFragBinding.setLifecycleOwner(getActivity());

        return mFragBinding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setupConfirmButton();
        setupOpenButton();
        setupCancelButton();

        // Subscribe to "new LAO event creation" event
        mLaoDetailViewModel
                .getNewLaoEventCreationEvent()
                .observe(
                        this,
                        eventTypeEvent -> {
                            EventType eventType = eventTypeEvent.getContentIfNotHandled();
                            if (eventType == EventType.ROLL_CALL) {
                                createRollCall();
                            }
                        });

        // Subscribe to "open new roll call" event
        mLaoDetailViewModel
                .getOpenNewRollCallEvent()
                .observe(
                        this,
                        booleanEvent -> {
                            Boolean action = booleanEvent.getContentIfNotHandled();
                            if (action != null) {
                                createAndOpenRollCall();
                            }
                        });

        mLaoDetailViewModel
                .getCreatedRollCallEvent()
                .observe(
                        this,
                        booleanEvent -> {
                            Boolean action = booleanEvent.getContentIfNotHandled();
                            if (action != null) {
                                mLaoDetailViewModel.openLaoDetail();
                            }
                        });
    }

    private void setupConfirmButton() {
        confirmButton.setOnClickListener(
                v -> mLaoDetailViewModel.newLaoEventCreation(EventType.ROLL_CALL));
    }

    private void setupOpenButton() {
        openButton.setOnClickListener(v -> mLaoDetailViewModel.openNewRollCall(true));
    }

    private void setupCancelButton() {
        mFragBinding.rollCallCancel.setOnClickListener(v -> mLaoDetailViewModel.openLaoDetail());
    }

    private void createRollCall() {
        computeTimesInSeconds();

        String title = mFragBinding.rollCallTitleText.getText().toString();
        String description = mFragBinding.rollCallEventDescriptionText.getText().toString();
        mLaoDetailViewModel.createNewRollCall(title, description, startTimeInSeconds, endTimeInSeconds, false);
    }

    private void createAndOpenRollCall() {
        computeTimesInSeconds();

        String title = mFragBinding.rollCallTitleText.getText().toString();
        String description = mFragBinding.rollCallEventDescriptionText.getText().toString();
        mLaoDetailViewModel.createNewRollCall(title, description, startTimeInSeconds, endTimeInSeconds, true);
    }
}