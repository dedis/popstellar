package com.github.dedis.popstellar.ui.detail.event.rollcall;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.github.dedis.popstellar.R;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.barcode.BarcodeDetector;

/**
 * This fragment wraps the QRCodeScanningFragment in order to show the user how many attendees he
 * has added so far and here the QRCode Fragment won't disappear after scanning a QR code, but will
 * stay until user tells he's done scanning attendees.
 *
 * <p>The attribute eventId represents the Roll-Call Event's id the user wants to add attendees to.
 */
public final class AddAttendeeFragment extends Fragment {

  public static final String TAG = AddAttendeeFragment.class.getSimpleName();
  private final String eventId;
  private final CameraSource camera;
  private final BarcodeDetector detector;

  public AddAttendeeFragment(String eventId, CameraSource camera, BarcodeDetector detector) {
    this.eventId = eventId;
    this.camera = camera;
    this.detector = detector;
  }

  public static AddAttendeeFragment newInstance(
      String eventId, CameraSource camera, BarcodeDetector detector) {
    return new AddAttendeeFragment(eventId, camera, detector);
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.add_attendee_fragment, container, false);

    Button confirm = view.findViewById(R.id.add_attendee_confirm);
    confirm.setOnClickListener(
        click -> {
          AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
          builder
              .setMessage(getString(R.string.scan_all_attendees_question))
              .setCancelable(false)
              .setPositiveButton(
                  getString(R.string.confirm),
                  (dialog, id) ->
                      getActivity()
                          .getSupportFragmentManager()
                          .popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE))
              .setNegativeButton(getString(R.string.cancel), (dialog, id) -> {});

          AlertDialog alert = builder.create();
          alert.show();
        });

    return view;
  }
}
