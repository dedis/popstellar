package com.github.dedis.student20_pop.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.ObservableList;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.github.dedis.student20_pop.PoPApplication;
import com.github.dedis.student20_pop.R;
import com.github.dedis.student20_pop.model.event.Event;
import com.github.dedis.student20_pop.model.event.RollCallEvent;

import java.util.Objects;
import java.util.Optional;

import static com.github.dedis.student20_pop.ui.QRCodeScanningFragment.QRCodeScanningType.ADD_ROLL_CALL;

public class AddAttendeeFragment extends Fragment {

    public static final String TAG = AddAttendeeFragment.class.getSimpleName();
    private final String eventId;

    public AddAttendeeFragment(String eventId) {
        super();
        this.eventId = eventId;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_attendee, container, false);

        Fragment newFragment = new QRCodeScanningFragment(ADD_ROLL_CALL, eventId);
        Objects.requireNonNull(getActivity())
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.add_attendee_qr_code_fragment, newFragment, QRCodeScanningFragment.TAG).addToBackStack(null)
                .commit();

        PoPApplication app = (PoPApplication) getActivity().getApplication();
        Optional<Event> matchingEvent = app
                .getEvents(app.getCurrentLao())
                .parallelStream()
                .filter(event -> event.getId().equals(eventId))
                .distinct()
                .findAny();

        RollCallEvent rollCallEvent;

        if (matchingEvent.isPresent()) {
            rollCallEvent = (RollCallEvent) matchingEvent.get();
            rollCallEvent.getAttendees()
                    .addOnListChangedCallback(
                            new ObservableList.OnListChangedCallback<ObservableList<String>>() {
                                @Override
                                public void onChanged(ObservableList<String> sender) {
                                    ((TextView) view.findViewById(R.id.add_attendee_number_text))
                                            .setText(getString(R.string.add_attendees_number, sender.size()));
                                }

                                @Override
                                public void onItemRangeChanged(ObservableList<String> sender, int positionStart, int itemCount) {
                                    ((TextView) view.findViewById(R.id.add_attendee_number_text))
                                            .setText(getString(R.string.add_attendees_number, sender.size()));
                                }

                                @Override
                                public void onItemRangeInserted(ObservableList<String> sender, int positionStart, int itemCount) {
                                    ((TextView) view.findViewById(R.id.add_attendee_number_text))
                                            .setText(getString(R.string.add_attendees_number, sender.size()));
                                }

                                @Override
                                public void onItemRangeMoved(ObservableList<String> sender, int fromPosition, int toPosition, int itemCount) {
                                    ((TextView) view.findViewById(R.id.add_attendee_number_text))
                                            .setText(getString(R.string.add_attendees_number, sender.size()));
                                }

                                @Override
                                public void onItemRangeRemoved(ObservableList<String> sender, int positionStart, int itemCount) {
                                    ((TextView) view.findViewById(R.id.add_attendee_number_text))
                                            .setText(getString(R.string.add_attendees_number, sender.size()));
                                }
                            });
        }


        Button confirm = view.findViewById(R.id.add_attendee_confirm);
        confirm.setOnClickListener(click -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(getString(R.string.scan_all_attendees_question))
                    .setCancelable(false)
                    .setPositiveButton(getString(R.string.confirm), (dialog, id) -> {
                        getActivity().getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);;
                    })
                    .setNegativeButton(getString(R.string.cancel), (dialog, id) -> {
                    });

            AlertDialog alert = builder.create();
            alert.show();
        });

        return view;
    }
}
