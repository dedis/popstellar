package com.github.dedis.popstellar.ui.detail.event.rollcall;

import android.graphics.Bitmap;
import android.view.*;
import android.widget.BaseAdapter;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.LifecycleOwner;

import com.github.dedis.popstellar.databinding.AttendeeLayoutBinding;
import com.github.dedis.popstellar.model.objects.security.PublicKey;

import net.glxn.qrgen.android.QRCode;

import java.util.List;

public class AttendeesListAdapter extends BaseAdapter {

  private List<PublicKey> attendees;
  private final LifecycleOwner lifecycleOwner;

  public AttendeesListAdapter(List<PublicKey> attendees, LifecycleOwner activity) {
    setList(attendees);
    lifecycleOwner = activity;
  }

  private void setList(List<PublicKey> attendees) {
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
    }

    if (binding == null) throw new IllegalStateException("Binding could not be find in the view");

    String attendee = attendees.get(position).getEncoded();

    binding.publicKey.setText("Public key:\n" + attendee);

    Bitmap myBitmap = QRCode.from(attendee).bitmap();
    binding.pkQrCode.setImageBitmap(myBitmap);

    binding.setLifecycleOwner(lifecycleOwner);
    binding.executePendingBindings();

    return binding.getRoot();
  }
}
