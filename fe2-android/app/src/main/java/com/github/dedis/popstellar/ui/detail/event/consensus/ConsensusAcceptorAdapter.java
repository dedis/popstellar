package com.github.dedis.popstellar.ui.detail.event.consensus;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.LifecycleOwner;
import com.github.dedis.popstellar.databinding.ConsensusAcceptorStatusBinding;
import java.util.List;
import java.util.Map;

public class ConsensusAcceptorAdapter extends BaseAdapter {

  private List<String> acceptors;
  private Map<String, Boolean> acceptorsResponse;
  private LifecycleOwner lifecycleOwner;

  public ConsensusAcceptorAdapter(
      List<String> acceptors,
      Map<String, Boolean> acceptorsResponse,
      LifecycleOwner lifecycleOwner) {
    this.acceptors = acceptors;
    this.acceptorsResponse = acceptorsResponse;
    this.lifecycleOwner = lifecycleOwner;
  }

  @Override
  public int getCount() {
    return acceptors.size();
  }

  @Override
  public Object getItem(int position) {
    return acceptors.get(position);
  }

  @Override
  public long getItemId(int position) {
    return position;
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    ConsensusAcceptorStatusBinding binding;
    if (convertView == null) {
      LayoutInflater inflater = LayoutInflater.from(parent.getContext());
      binding = ConsensusAcceptorStatusBinding.inflate(inflater);
    } else {
      binding = DataBindingUtil.getBinding(convertView);
    }

    String publicKey = acceptors.get(position);
    String response = getResponse(publicKey);
    binding.publicKey.setText(publicKey);
    binding.response.setText(response);

    binding.setLifecycleOwner(lifecycleOwner);
    binding.executePendingBindings();

    return binding.getRoot();
  }

  private String getResponse(String publicKey) {
    Boolean accepted = acceptorsResponse.get(publicKey);
    if (accepted == null) {
      return "Waiting";
    } else if (accepted) {
      return "Accepted";
    } else {
      return "Rejected";
    }
  }
}
