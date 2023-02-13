package com.github.dedis.popstellar.ui.detail.event.consensus;

import android.view.*;
import android.widget.BaseAdapter;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.LifecycleOwner;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.ConsensusNodeLayoutBinding;
import com.github.dedis.popstellar.model.objects.ConsensusNode;
import com.github.dedis.popstellar.model.objects.ElectInstance;
import com.github.dedis.popstellar.model.objects.ElectInstance.State;
import com.github.dedis.popstellar.ui.detail.LaoDetailViewModel;
import com.github.dedis.popstellar.utility.error.ErrorUtils;

import java.util.*;

public class NodesAcceptorAdapter extends BaseAdapter {

  private static final String TAG = NodesAcceptorAdapter.class.getSimpleName();
  private List<ConsensusNode> nodes = new ArrayList<>();
  private final ConsensusNode ownNode;
  private final String instanceId;
  private final LaoDetailViewModel laoDetailViewModel;
  private final LifecycleOwner lifecycleOwner;

  public NodesAcceptorAdapter(
      ConsensusNode ownNode,
      String instanceId,
      LifecycleOwner lifecycleOwner,
      LaoDetailViewModel laoDetailViewModel) {
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
    Optional<ElectInstance> lastElectInstance = node.getLastElectInstance(instanceId);
    State state = node.getState(instanceId);
    boolean alreadyAccepted =
        lastElectInstance
            .map(ElectInstance::getMessageId)
            .map(ownNode.getAcceptedMessageIds()::contains)
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
    lastElectInstance.ifPresent(
        electInstance ->
            binding.nodeButton.setOnClickListener(
                clicked ->
                    laoDetailViewModel.addDisposable(
                        laoDetailViewModel
                            .sendConsensusElectAccept(electInstance, true)
                            .subscribe(
                                () -> {},
                                error ->
                                    ErrorUtils.logAndShow(
                                        parent.getContext(),
                                        TAG,
                                        error,
                                        R.string.error_consensus_accept)))));
    binding.setLifecycleOwner(lifecycleOwner);
    binding.executePendingBindings();

    return binding.getRoot();
  }
}
