package com.github.dedis.popstellar.ui.detail.witness;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.LifecycleOwner;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.WitnessesListViewLayoutBinding;
import com.github.dedis.popstellar.ui.detail.LaoDetailViewModel;

import java.util.List;

/** Adapter to show witnesses of an Event */
public class WitnessListViewAdapter extends BaseAdapter {

  private final LaoDetailViewModel viewModel;

  private List<String> witnesses;

  private final LifecycleOwner lifecycleOwner;

  public WitnessListViewAdapter(
      List<String> witness, LaoDetailViewModel viewModel, LifecycleOwner activity) {
    this.viewModel = viewModel;
    setList(witness);
    lifecycleOwner = activity;
  }

  public void replaceList(List<String> witnesses) {
    setList(witnesses);
  }

  private void setList(List<String> witnesses) {
    this.witnesses = witnesses;
    notifyDataSetChanged();
  }

  /**
   * How many items are in the data set represented by this Adapter.
   *
   * @return Count of items.
   */
  @Override
  public int getCount() {
    return witnesses != null ? witnesses.size() : 0;
  }

  /**
   * Get the data item associated with the specified position in the data set.
   *
   * @param position Position of the item whose data we want within the adapter's data set.
   * @return The data at the specified position.
   */
  @Override
  public Object getItem(int position) {
    return witnesses.get(position);
  }

  /**
   * Get the row id associated with the specified position in the list.
   *
   * @param position The position of the item within the adapter's data set whose row id we want.
   * @return The id of the item at the specified position.
   */
  @Override
  public long getItemId(int position) {
    return position;
  }

  /**
   * Get a View that displays the data at the specified position in the data set. You can either
   * create a View manually or inflate it from an XML layout file. When the View is inflated, the
   * parent View (GridView, ListView...) will apply default layout parameters unless you use {@link
   * LayoutInflater#inflate(int, ViewGroup, boolean)} to specify a root view and to prevent
   * attachment to the root.
   *
   * @param position The position of the item within the adapter's data set of the item whose view
   *     we want.
   * @param convertView The old view to reuse, if possible. Note: You should check that this view is
   *     non-null and of an appropriate type before using. If it is not possible to convert this
   *     view to display the correct data, this method can create a new view. Heterogeneous lists
   *     can specify their number of view types, so that this View is always of the right type (see
   *     {@link #getViewTypeCount()} and {@link #getItemViewType(int)}).
   * @param parent The parent that this view will eventually be attached to
   * @return A View corresponding to the data at the specified position.
   */
  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    WitnessesListViewLayoutBinding binding;
    if (convertView == null) {
      LayoutInflater inflater = LayoutInflater.from(parent.getContext());
      binding = WitnessesListViewLayoutBinding.inflate(inflater, parent, false);
    } else {
      binding = DataBindingUtil.getBinding(convertView);
    }

    binding.setLifecycleOwner(lifecycleOwner);
    binding.setViewModel(viewModel);
    binding.setPosition(position + 1);
    binding.setWitness(witnesses.get(position));
    Context context = parent.getContext();
    WitnessDeleteListener deleteButtonListener =
        witness -> {
          AlertDialog.Builder adb = new AlertDialog.Builder(context);
          // TODO: Wait on this until we hear a success/failure from the server.
          adb.setTitle(context.getString(R.string.delete_witness_dialog_title));
          adb.setMessage(
              context.getString(R.string.delete_witness_dialog_message, +(position + 1)));
          adb.setNegativeButton(context.getString(R.string.button_cancel), null);
          adb.setPositiveButton(
              context.getString(R.string.button_confirm),
              (dialog, which) -> {
                viewModel.removeWitness(witness);
              });
          adb.show();
        };

    binding.setListener(deleteButtonListener);
    binding.executePendingBindings();

    return binding.getRoot();
  }
}
