package com.github.dedis.popstellar.ui.detail.event.consensus;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.LifecycleOwner;
import com.github.dedis.popstellar.databinding.ConsensusNodeLayoutBinding;
import com.github.dedis.popstellar.model.objects.ConsensusNode;
import com.github.dedis.popstellar.model.objects.ConsensusNode.State;
import com.github.dedis.popstellar.ui.detail.LaoDetailViewModel;
import java.util.List;

public class NodesAcceptorAdapter extends BaseAdapter {

  private final List<ConsensusNode> nodes;
  private final String ownPublicKey;
  private final LaoDetailViewModel laoDetailViewModel;
  private final LifecycleOwner lifecycleOwner;

  public NodesAcceptorAdapter(
      List<ConsensusNode> nodes,
      String ownPublicKey,
      LifecycleOwner lifecycleOwner,
      LaoDetailViewModel laoDetailViewModel) {
    this.nodes = nodes;
    this.ownPublicKey = ownPublicKey;
    this.laoDetailViewModel = laoDetailViewModel;
    this.lifecycleOwner = lifecycleOwner;
  }

  @Override
  public int getCount() {
    return nodes.size();
  }

  @Override
  public ConsensusNode getItem(int position) {
    return nodes.get(position);
  }

  @Override
  public long getItemId(int position) {
    return position;
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    ConsensusNodeLayoutBinding binding;
    if (convertView == null) {
      LayoutInflater inflater = LayoutInflater.from(parent.getContext());
      binding = ConsensusNodeLayoutBinding.inflate(inflater);
    } else {
      binding = DataBindingUtil.getBinding(convertView);
    }

    ConsensusNode node = getItem(position);
    State state = node.getState();
    String text = "";
    switch (state) {
      case FAILED:
        text = "Start Failed\n";
        break;
      case STARTING:
        text = "Approve Start by\n";
        break;
      case WAITING:
        text = "Waiting\n";
        break;
    }
    text += node.getPublicKey();

    binding.nodeButton.setText(text);
    binding.nodeButton.setEnabled(state == State.STARTING);
    binding.nodeButton.setOnClickListener(
        clicked -> {
          if (!node.getPublicKey().equals(ownPublicKey)) {
            laoDetailViewModel.sendConsensusElectAccept(node.getConsensus(), true);
          }
        });

    binding.setLifecycleOwner(lifecycleOwner);
    binding.executePendingBindings();

    return binding.getRoot();
  }
}
