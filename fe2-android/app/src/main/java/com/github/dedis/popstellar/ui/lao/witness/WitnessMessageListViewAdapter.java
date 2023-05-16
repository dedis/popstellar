package com.github.dedis.popstellar.ui.lao.witness;

import android.app.AlertDialog;
import android.content.Context;
import android.view.*;
import android.widget.BaseAdapter;
import android.widget.Button;

import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.FragmentActivity;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.WitnessMessageLayoutBinding;
import com.github.dedis.popstellar.model.objects.WitnessMessage;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.ui.lao.LaoActivity;
import com.github.dedis.popstellar.ui.lao.LaoViewModel;
import com.github.dedis.popstellar.utility.ActivityUtils;
import com.github.dedis.popstellar.utility.error.ErrorUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import timber.log.Timber;

/** Adapter to show the messages that have to be signed by the witnesses */
public class WitnessMessageListViewAdapter extends BaseAdapter {

  private static final String TAG = WitnessMessageListViewAdapter.class.getSimpleName();
  private final LaoViewModel laoViewModel;
  private final WitnessingViewModel witnessingViewModel;

  private List<WitnessMessage> messages;

  private final FragmentActivity activity;
  private boolean isWitness;
  private static final String NO_SIGNATURES = "No signatures yet";

  public WitnessMessageListViewAdapter(List<WitnessMessage> messages, FragmentActivity activity) {
    laoViewModel = LaoActivity.obtainViewModel(activity);
    witnessingViewModel = LaoActivity.obtainWitnessingViewModel(activity, laoViewModel.getLaoId());
    isWitness = Boolean.TRUE.equals(laoViewModel.isWitness().getValue());
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

    if (binding == null) {
      throw new IllegalStateException("Binding could not be found in the view");
    }

    WitnessMessage witnessMessage = messages.get(position);

    // Set message title and description
    binding.messageTitle.setText(witnessMessage.getTitle());
    binding.messageDescriptionText.setText(witnessMessage.getDescription());

    // Set witness signatures text
    String formattedSignatures = formatPublicKeys(witnessMessage.getWitnesses());
    binding.witnessesText.setText(formattedSignatures);

    // Set message description dropdown
    binding.messageDescriptionCard.setOnClickListener(
        v ->
            ActivityUtils.handleExpandArrow(
                binding.messageDescriptionArrow, binding.messageDescriptionText));

    // Set signatures dropdown
    binding.signaturesCard.setOnClickListener(
        v -> ActivityUtils.handleExpandArrow(binding.signaturesArrow, binding.witnessesText));

    if (isWitness) {
      Context context = parent.getContext();
      View.OnClickListener listener =
          setUpSignButtonClickListener(context, witnessMessage, binding.signMessageButton);
      binding.signMessageButton.setOnClickListener(listener);
    } else {
      // Don't show the sign button if the user is not a witness
      binding.signMessageButton.setVisibility(View.GONE);
    }

    binding.setLifecycleOwner(activity);

    return binding.getRoot();
  }

  private View.OnClickListener setUpSignButtonClickListener(
      Context context, WitnessMessage message, Button button) {
    return v -> {
      AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);

      dialogBuilder.setTitle(R.string.sign_message);
      dialogBuilder.setMessage(
          String.format(
              context.getString(R.string.confirm_to_sign), message.getMessageId().getEncoded()));

      dialogBuilder.setNegativeButton(R.string.cancel, null);

      dialogBuilder.setPositiveButton(
          R.string.confirm,
          (dialog, which) ->
              laoViewModel.addDisposable(
                  witnessingViewModel
                      .signMessage(message)
                      .subscribe(
                          () -> {
                            Timber.tag(TAG).d("Witness message successfully signed");
                            button.setEnabled(false);
                          },
                          error ->
                              ErrorUtils.logAndShow(
                                  activity, TAG, error, R.string.error_sign_message))));

      dialogBuilder.show();
    };
  }

  private static String formatPublicKeys(Set<PublicKey> witnesses) {
    if (witnesses.isEmpty()) {
      return NO_SIGNATURES;
    } else {
      return witnesses.stream().map(PublicKey::getEncoded).collect(Collectors.joining("\n"));
    }
  }
}
