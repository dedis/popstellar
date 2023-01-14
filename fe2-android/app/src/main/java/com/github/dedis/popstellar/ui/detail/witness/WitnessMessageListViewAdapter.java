package com.github.dedis.popstellar.ui.detail.witness;

import android.app.AlertDialog;
import android.content.Context;
import android.view.*;
import android.widget.BaseAdapter;

import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.FragmentActivity;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.WitnessMessageLayoutBinding;
import com.github.dedis.popstellar.model.objects.WitnessMessage;
import com.github.dedis.popstellar.ui.detail.LaoDetailViewModel;
import com.github.dedis.popstellar.utility.error.ErrorUtils;

import java.util.List;

/** Adapter to show the messages that have to be signed by the witnesses */
public class WitnessMessageListViewAdapter extends BaseAdapter {

  private static final String TAG = WitnessMessageListViewAdapter.class.getSimpleName();
  private final LaoDetailViewModel viewModel;

  private List<WitnessMessage> messages;

  private final FragmentActivity activity;

  public WitnessMessageListViewAdapter(
      List<WitnessMessage> messages, LaoDetailViewModel viewModel, FragmentActivity activity) {
    this.viewModel = viewModel;
    this.activity = activity;
    setList(messages);
  }

  public void replaceList(List<WitnessMessage> messages) {
    setList(messages);
  }

  private void setList(List<WitnessMessage> messages) {
    this.messages = messages;
    notifyDataSetChanged();
  }

  /**
   * How many items are in the data set represented by this Adapter.
   *
   * @return Count of items.
   */
  @Override
  public int getCount() {
    return messages != null ? messages.size() : 0;
  }

  @Override
  public Object getItem(int position) {
    return messages.get(position);
  }

  @Override
  public long getItemId(int position) {
    return position;
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    WitnessMessageLayoutBinding binding;
    if (convertView == null) {
      // inflate
      LayoutInflater inflater = LayoutInflater.from(parent.getContext());

      binding = WitnessMessageLayoutBinding.inflate(inflater, parent, false);
    } else {
      binding = DataBindingUtil.getBinding(convertView);
    }

    if (binding == null) throw new IllegalStateException("Binding could not be find in the view");

    Context context = parent.getContext();
    View.OnClickListener listener =
        v -> {
          AlertDialog.Builder adb = new AlertDialog.Builder(context);

          if (Boolean.TRUE.equals(viewModel.isWitness().getValue())) {
            adb.setTitle("Sign Message");
            adb.setMessage(
                " Are you sure you want to sign message with ID : "
                    + messages.get(position).getMessageId());
            adb.setNegativeButton("Cancel", null);
            adb.setPositiveButton(
                "Confirm",
                (dialog, which) ->
                    viewModel.addDisposable(
                        viewModel
                            .signMessage(messages.get(position))
                            .subscribe(
                                () -> {},
                                error ->
                                    ErrorUtils.logAndShow(
                                        activity, TAG, error, R.string.error_sign_message))));
          } else {
            adb.setTitle("You are not a witness");
            adb.setMessage("You need to be a witness in order to sign this message");
            adb.setCancelable(false);
            adb.setPositiveButton("Ok", (dialog, which) -> {});
          }
          adb.show();
        };
    binding.signMessageButton.setOnClickListener(listener);

    binding.setMessage(messages.get(position));
    binding.setViewmodel(viewModel);
    binding.setLifecycleOwner(activity);

    binding.executePendingBindings();

    return binding.getRoot();
  }
}
