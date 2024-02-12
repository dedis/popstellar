package com.github.dedis.popstellar.model.objects.security

import com.github.dedis.popstellar.model.Immutable
import com.github.dedis.popstellar.model.objects.security.privatekey.PlainPrivateKey

/** Represents a PoPToken key pair with its private and public keys */
@Immutable
class PoPToken(privateKey: ByteArray, publicKey: ByteArray) :
    KeyPair(PlainPrivateKey(privateKey), PublicKey(publicKey)) {
  override val privateKey: PlainPrivateKey
    get() = super.privateKey as PlainPrivateKey
}
