package com.github.dedis.popstellar.model.objects.view

import com.github.dedis.popstellar.model.objects.Channel
import com.github.dedis.popstellar.model.objects.ConsensusNode
import com.github.dedis.popstellar.model.objects.ElectInstance
import com.github.dedis.popstellar.model.objects.Lao
import com.github.dedis.popstellar.model.objects.security.MessageID
import com.github.dedis.popstellar.model.objects.security.PublicKey
import java.util.Optional

class LaoView(lao: Lao?) {
    private val lao: Lao

    /**
     * This class offers useful getters for Lao state to handlers and prevents changing its state. It
     * is provided as an intermediate step towards functional handling of Objects. To change the state
     * of an Lao, one can use createLaoCopy() which returns a copy of the wrapped Lao, and update the
     * repository with said updated LAO.
     *
     * @param lao the Lao to be wrapped
     */
    init {
        requireNotNull(lao)
        this.lao = Lao(lao)
    }

    fun createLaoCopy(): Lao {
        return Lao(lao)
    }

    val lastModified: Long
        get() = lao.lastModified!!
    val name: String?
        get() = lao.getName()
    val id: String?
        get() = lao.getId()

    fun isOrganizer(publicKey: PublicKey): Boolean {
        return lao.getOrganizer() == publicKey
    }

    val channel: Channel
        get() = lao.channel
    val organizer: PublicKey?
        get() = lao.getOrganizer()

    fun getElectInstance(messageId: MessageID?): Optional<ElectInstance> {
        // TODO uncomment that when consensus does not rely on call by reference
        //    Optional<ElectInstance> optional = lao.getElectInstance(messageId);
        //    return optional.map(ElectInstance::new); // If empty returns empty optional, if not
        // returns optional with copy of retrieved ElectInstance
        return lao.getElectInstance(messageId)
    }

    val nodes: List<ConsensusNode>
        get() = lao.nodes

    fun getNode(key: PublicKey): ConsensusNode? {
        return lao.getNode(key)
    }

    val creation: Long
        get() = lao.creation!!

    override fun toString(): String {
        return lao.toString()
    }
}