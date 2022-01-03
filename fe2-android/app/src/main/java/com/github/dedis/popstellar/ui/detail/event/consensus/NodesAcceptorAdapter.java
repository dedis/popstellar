package com.github.dedis.popstellar.ui.detail.event.consensus;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.LifecycleOwner;

import com.github.dedis.popstellar.databinding.ConsensusNodeLayoutBinding;
import com.github.dedis.popstellar.model.objects.Consensus;
import com.github.dedis.popstellar.model.objects.ConsensusNode;
import com.github.dedis.popstellar.model.objects.ConsensusNode.State;
import com.github.dedis.popstellar.ui.detail.LaoDetailViewModel;

import java.util.List;
import java.util.Optional;

public class NodesAcceptorAdapter extends BaseAdapter {

  private List<ConsensusNode> nodes;
  private final ConsensusNode ownNode;
  private final String instanceId;
  private final LaoDetailViewModel laoDetailViewModel;
  private final LifecycleOwner lifecycleOwner;

  public NodesAcceptorAdapter(
      List<ConsensusNode> nodes,
      ConsensusNode ownNode,
      String instanceId,
      LifecycleOwner lifecycleOwner,
      LaoDetailViewModel laoDetailViewModel) {
    this.nodes = nodes;
    this.ownNode = ownNode;
    this.instanceId = instanceId;
    this.laoDetailViewModel = laoDetailViewModel;
    this.lifecycleOwner = lifecycleOwner;
  }

  public void setList(List<ConsensusNode> nodes) {
    this.nodes = nodes;
    notifyDataSetChanged();
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

    if (binding == null) {
      throw new IllegalStateException("Binding could not be find in the view");
    }

    ConsensusNode node = getItem(position);
    Optional<Consensus> lastConsensus = node.getLastConsensus(instanceId);
    State state = node.getState(instanceId);
    boolean alreadyAccepted =
        lastConsensus
            .map(consensus -> ownNode.getAcceptedMessageIds().contains(consensus.getMessageId()))
            .orElse(false);

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
      case ACCEPTED:
        text = "Started by\n";
    }
    text += node.getPublicKey().getEncoded();

    binding.nodeButton.setText(text);
    binding.nodeButton.setEnabled(state == State.STARTING && !alreadyAccepted);
    lastConsensus.ifPresent(
        consensus ->
            binding.nodeButton.setOnClickListener(
                clicked -> laoDetailViewModel.sendConsensusElectAccept(consensus, true)));

    binding.setLifecycleOwner(lifecycleOwner);
    binding.executePendingBindings();

    return binding.getRoot();
  }
}
