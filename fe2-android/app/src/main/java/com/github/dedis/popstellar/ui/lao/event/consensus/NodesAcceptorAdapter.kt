package com.github.dedis.popstellar.ui.lao.event.consensus

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LifecycleOwner
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.databinding.ConsensusNodeLayoutBinding
import com.github.dedis.popstellar.model.objects.ConsensusNode
import com.github.dedis.popstellar.model.objects.ElectInstance
import com.github.dedis.popstellar.model.objects.security.MessageID
import com.github.dedis.popstellar.ui.lao.LaoViewModel
import com.github.dedis.popstellar.utility.error.ErrorUtils.logAndShow

class NodesAcceptorAdapter(
    private val ownNode: ConsensusNode,
    private val instanceId: String,
    private val lifecycleOwner: LifecycleOwner,
    private val laoViewModel: LaoViewModel,
    private val consensusViewModel: ConsensusViewModel
) : BaseAdapter() {
  private var nodes: List<ConsensusNode> = ArrayList()

  fun setList(nodes: List<ConsensusNode>) {
    this.nodes = nodes
    notifyDataSetChanged()
  }

  override fun getCount(): Int {
    return nodes.size
  }

  override fun getItem(position: Int): ConsensusNode {
    return nodes[position]
  }

  override fun getItemId(position: Int): Long {
    return position.toLong()
  }

  override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
    val binding: ConsensusNodeLayoutBinding? =
        if (convertView == null) {
          val inflater = LayoutInflater.from(parent.context)
          ConsensusNodeLayoutBinding.inflate(inflater)
        } else {
          DataBindingUtil.getBinding(convertView)
        }

    checkNotNull(binding) { "Binding could not be find in the view" }

    val node = getItem(position)
    val lastElectInstance = node.getLastElectInstance(instanceId)
    val state = node.getState(instanceId)
    val alreadyAccepted =
        lastElectInstance
            .map(ElectInstance::messageId)
            .map { o: MessageID -> ownNode.getAcceptedMessageIds().contains(o) }
            .orElse(false)
    var text =
        when (state) {
          ElectInstance.State.FAILED -> "Start Failed\n"
          ElectInstance.State.STARTING -> "Approve Start by\n"
          ElectInstance.State.WAITING -> "Waiting\n"
          ElectInstance.State.ACCEPTED -> "Started by\n"
        }
    text += node.publicKey.encoded

    binding.nodeButton.text = text
    binding.nodeButton.isEnabled = state === ElectInstance.State.STARTING && !alreadyAccepted
    lastElectInstance.ifPresent { electInstance: ElectInstance ->
      binding.nodeButton.setOnClickListener {
        laoViewModel.addDisposable(
            consensusViewModel
                .sendConsensusElectAccept(electInstance, true)
                .subscribe(
                    {},
                    { error: Throwable ->
                      logAndShow(parent.context, TAG, error, R.string.error_consensus_accept)
                    }))
      }
    }
    binding.lifecycleOwner = lifecycleOwner
    binding.executePendingBindings()

    return binding.root
  }

  companion object {
    private val TAG = NodesAcceptorAdapter::class.java.simpleName
  }
}
