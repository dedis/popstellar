package com.github.dedis.popstellar.model.network.method.message

import com.github.dedis.popstellar.model.Immutable
import com.github.dedis.popstellar.model.objects.security.PublicKey
import com.github.dedis.popstellar.model.objects.security.Signature

@Immutable
class PublicKeySignaturePair(@JvmField val witness: PublicKey, @JvmField val signature: Signature) {

    override fun toString(): String {
        return ("PublicKeySignaturePair{"
                + "witness="
                + witness.encoded
                + ", signature="
                + signature.encoded
                + '}')
    }
}