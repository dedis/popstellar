package com.github.dedis.popstellar.ui.detail.event.rollcall;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.LifecycleOwner;

import com.github.dedis.popstellar.databinding.AttendeeLayoutBinding;

import net.glxn.qrgen.android.QRCode;

import java.util.List;

public class AttendeesListAdapter extends BaseAdapter {

  private List<String> attendees;
  private final LifecycleOwner lifecycleOwner;

  public AttendeesListAdapter(List<String> attendees, LifecycleOwner activity) {
    setList(attendees);
    lifecycleOwner = activity;
  }

  private void setList(List<String> attendees) {
    this.attendees = attendees;
    notifyDataSetChanged();
  }

  @Override
  public int getCount() {
    return attendees != null ? attendees.size() : 0;
  }

  @Override
  public Object getItem(int position) {
    return attendees.get(position);
  }

  @Override
  public long getItemId(int position) {
    return position;
  }

  @Override
  public View getView(int position, View view, ViewGroup viewGroup) {
    AttendeeLayoutBinding binding;
    if (view == null) {
      // inflate
      LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());

      binding = AttendeeLayoutBinding.inflate(inflater, viewGroup, false);
    } else {
      binding = DataBindingUtil.getBinding(view);

      if (binding == null) {
        throw new IllegalStateException("Cannot find binding");
      }
    }

    String attendee = attendees.get(position);

    binding.publicKey.setText("Public key:\n" + attendee);

    Bitmap myBitmap = QRCode.from(attendee).bitmap();
    binding.pkQrCode.setImageBitmap(myBitmap);

    binding.setLifecycleOwner(lifecycleOwner);
    binding.executePendingBindings();

    return binding.getRoot();
  }
}
