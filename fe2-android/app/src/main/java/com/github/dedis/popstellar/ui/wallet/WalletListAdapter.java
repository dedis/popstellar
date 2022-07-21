package com.github.dedis.popstellar.ui.wallet;

import android.view.*;
import android.widget.BaseAdapter;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.LifecycleOwner;

import com.github.dedis.popstellar.databinding.RollCallEventLayoutBinding;
import com.github.dedis.popstellar.model.objects.RollCall;
import com.github.dedis.popstellar.ui.detail.LaoDetailViewModel;

import java.text.SimpleDateFormat;
import java.util.*;

public class WalletListAdapter extends BaseAdapter {

  private List<RollCall> rollCalls;
  private final SimpleDateFormat DATE_FORMAT =
      new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ENGLISH);
  private final LifecycleOwner lifecycleOwner;
  private final LaoDetailViewModel viewModel;

  public WalletListAdapter(
      List<RollCall> rollCalls, LaoDetailViewModel viewModel, LifecycleOwner activity) {
    this.viewModel = viewModel;
    setList(rollCalls);
    lifecycleOwner = activity;
  }

  public void replaceList(List<RollCall> rollCalls) {
    setList(rollCalls);
  }

  private void setList(List<RollCall> rollCalls) {
    this.rollCalls = rollCalls;
    notifyDataSetChanged();
  }

  @Override
  public int getCount() {
    return rollCalls != null ? rollCalls.size() : 0;
  }

  @Override
  public Object getItem(int position) {
    return rollCalls.get(position);
  }

  @Override
  public long getItemId(int position) {
    return position;
  }

  @Override
  public View getView(int position, View view, ViewGroup viewGroup) {
    RollCallEventLayoutBinding binding;
    if (view == null) {
      // inflate
      LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());

      binding = RollCallEventLayoutBinding.inflate(inflater, viewGroup, false);
    } else {
      binding = DataBindingUtil.getBinding(view);
    }

    if (binding == null) throw new IllegalStateException("Binding could not be find in the view");

    RollCall rollCall = rollCalls.get(position);
    binding.rollcallDate.setText(
        "Ended: " + DATE_FORMAT.format(new Date(1000 * rollCall.getEnd())));
    binding.rollcallTitle.setText("Roll Call: " + rollCall.getName());
    binding.rollcallLocation.setText("Location: " + rollCall.getLocation());

    binding.rollcallOpenButton.setVisibility(View.GONE);
    binding.rollcallReopenButton.setVisibility(View.GONE);
    binding.rollcallScheduledButton.setVisibility(View.GONE);
    binding.rollcallEnterButton.setVisibility(View.GONE);
    binding.rollcallClosedButton.setVisibility(View.GONE);

    binding.rollcallAttendeesListButton.setVisibility(View.VISIBLE);
    binding.rollcallAttendeesListButton.setOnClickListener(
        clicked -> viewModel.openAttendeesList(rollCall.getId()));

    Boolean isOrganizer = viewModel.isOrganizer().getValue();
    if (isOrganizer != null && !isOrganizer) {
      binding.rollcallTokenButton.setVisibility(View.VISIBLE);
      binding.rollcallTokenButton.setOnClickListener(
          clicked -> viewModel.openRollCallToken(rollCall.getId()));
    }

    binding.setLifecycleOwner(lifecycleOwner);
    binding.executePendingBindings();

    return binding.getRoot();
  }
}
