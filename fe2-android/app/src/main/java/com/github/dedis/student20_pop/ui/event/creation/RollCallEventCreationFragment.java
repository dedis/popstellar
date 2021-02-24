package com.github.dedis.student20_pop.ui.event.creation;

import android.content.Context;
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
import androidx.fragment.app.FragmentManager;
import com.github.dedis.student20_pop.PoPApplication;
import com.github.dedis.student20_pop.R;
import com.github.dedis.student20_pop.model.event.RollCallEvent;
import com.github.dedis.student20_pop.utility.ui.listener.OnAddAttendeesListener;
import com.github.dedis.student20_pop.utility.ui.listener.OnEventCreatedListener;
import java.util.Objects;

/** Fragment that shows up when user wants to create a Roll-Call Event */
public final class RollCallEventCreationFragment extends AbstractEventCreationFragment {
  public static final String TAG = RollCallEventCreationFragment.class.getSimpleName();

  private EditText rollCallDescriptionEditText;
  private EditText rollCallTitleEditText;
  private RollCallEvent rollCallEvent;
  private OnEventCreatedListener eventCreatedListener;
  private OnAddAttendeesListener onAddAttendeesListener;
  private Button confirmButton;
  private Button openButton;

  private final TextWatcher confirmTextWatcher =
      new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
          String meetingTitle = rollCallTitleEditText.getText().toString().trim();
          boolean areFieldsFilled =
              !meetingTitle.isEmpty() && !getStartDate().isEmpty() && !getStartTime().isEmpty();
          confirmButton.setEnabled(areFieldsFilled);
          openButton.setEnabled(areFieldsFilled);
        }

        @Override
        public void afterTextChanged(Editable s) {}
      };

  public static RollCallEventCreationFragment newInstance() {
    return new RollCallEventCreationFragment();
  }

  @Override
  public void onAttach(@NonNull Context context) {
    super.onAttach(context);
    eventCreatedListener = (OnEventCreatedListener) context;
    onAddAttendeesListener = (OnAddAttendeesListener) context;
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    final FragmentManager fragmentManager =
        (Objects.requireNonNull(getActivity())).getSupportFragmentManager();
    View view = inflater.inflate(R.layout.fragment_create_roll_call_event, container, false);
    PoPApplication app = (PoPApplication) getActivity().getApplication();

    setDateAndTimeView(view, RollCallEventCreationFragment.this, fragmentManager);
    addDateAndTimeListener(confirmTextWatcher);

    rollCallTitleEditText = view.findViewById(R.id.roll_call_title_text);
    rollCallDescriptionEditText = view.findViewById(R.id.roll_call_event_description_text);

    openButton = view.findViewById(R.id.roll_call_open);

    confirmButton = view.findViewById(R.id.roll_call_confirm);
    confirmButton.setOnClickListener(
        v -> {
          computeTimesInSeconds();

          if (rollCallEvent == null) {
            //            rollCallEvent =
            //                new RollCallEvent(
            //                    rollCallTitleEditText.getText().toString(),
            //                    startTimeInSeconds,
            //                    endTimeInSeconds,
            //                    app.getCurrentLaoUnsafe().getId(),
            //                    new ObservableArrayList<>(),
            //                    NO_LOCATION,
            //                    rollCallDescriptionEditText.getText().toString());
          }
          //          eventCreatedListener.OnEventCreatedListener(rollCallEvent);
          fragmentManager.popBackStackImmediate();
        });

    //    openButton.setOnClickListener(
    //        v -> {
    //          computeTimesInSeconds();
    //
    //          rollCallEvent =
    //              new RollCallEvent(
    //                  rollCallTitleEditText.getText().toString(),
    //                  startTimeInSeconds,
    //                  endTimeInSeconds,
    //                  app.getCurrentLaoUnsafe().getId(),
    //                  new ObservableArrayList<>(),
    //                  NO_LOCATION,
    //                  rollCallDescriptionEditText.getText().toString());
    //          eventCreatedListener.OnEventCreatedListener(rollCallEvent);
    //          onAddAttendeesListener.onAddAttendeesListener(rollCallEvent.getId());
    //        });

    Button cancelButton = view.findViewById(R.id.roll_call_cancel);
    //    cancelButton.setOnClickListener(
    //        v -> {
    //          fragmentManager.popBackStackImmediate();
    //        });

    return view;
  }
}
