package com.github.dedis.popstellar.model.objects.security

import com.github.dedis.popstellar.model.Immutable
import com.github.dedis.popstellar.model.objects.security.privatekey.PlainPrivateKey

/** Represents a AuthToken key pair with its private and public keys */
@Immutable
class AuthToken : KeyPair {
  constructor(
      privateKey: ByteArray,
      publicKey: ByteArray
  ) : super(PlainPrivateKey(privateKey), PublicKey(publicKey))

  constructor(otherToken: PoPToken) : super(otherToken.privateKey, otherToken.publicKey)

  override val privateKey: PrivateKey
    get() = super.privateKey as PlainPrivateKey
}
