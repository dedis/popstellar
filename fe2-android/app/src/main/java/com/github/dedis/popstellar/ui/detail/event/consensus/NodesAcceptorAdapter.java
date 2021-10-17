package com.github.dedis.popstellar.ui.detail.event.consensus;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LifecycleOwner;
import com.github.dedis.popstellar.databinding.ConsensusNodeLayoutBinding;
import com.github.dedis.popstellar.model.objects.Consensus;
import com.github.dedis.popstellar.model.objects.ConsensusNode;
import com.github.dedis.popstellar.model.objects.ConsensusNode.State;
import com.github.dedis.popstellar.ui.detail.LaoDetailActivity;
import com.github.dedis.popstellar.ui.detail.LaoDetailViewModel;
import java.util.List;

public class NodesAcceptorAdapter extends BaseAdapter {

  private static final String TAG = NodesAcceptorAdapter.class.getSimpleName();

  private List<ConsensusNode> nodes;
  private final LaoDetailViewModel laoDetailViewModel;
  private final LifecycleOwner lifecycleOwner;

  public NodesAcceptorAdapter(List<ConsensusNode> nodes, FragmentActivity fragmentActivity) {
    this.nodes = nodes;
    this.laoDetailViewModel = LaoDetailActivity.obtainViewModel(fragmentActivity);
    this.lifecycleOwner = fragmentActivity;
  }

  public void updateList(List<ConsensusNode> nodes) {
    if (nodes == null) {
      throw new IllegalArgumentException("nodes list can't be null");
    }
    this.nodes = nodes;
    notifyDataSetChanged();
  }

  public void updateItem(String publicKey, State newState, Consensus consensus) {
    for (ConsensusNode node : nodes) {
      if (node.getPublicKey().equals(publicKey)) {
        node.setState(newState);
        node.setConsensus(consensus);
        notifyDataSetChanged();
        return;
      }
    }
    Log.w(TAG, "Couldn't find a node with public key : " + publicKey);
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
        clicked -> laoDetailViewModel.sendConsensusElectAccept(node.getConsensus(), true));

    binding.setLifecycleOwner(lifecycleOwner);
    binding.executePendingBindings();

    return binding.getRoot();
  }
}
