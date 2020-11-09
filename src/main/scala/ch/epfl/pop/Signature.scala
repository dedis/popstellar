package ch.epfl.pop

import scorex.crypto.signatures
import scorex.crypto.signatures.Curve25519
import scorex.util.encode.Base16


object Signature {

  /**
   * Verify if a signature is correct.
   * @param msg the msg to verify
   * @param signature the signature corresponding to the message, in hex format
   * @param key the public key used for verification, in hex format
   * @return wether the signature is correct
   */
  def verify(msg: String, signature: String, key: String): Boolean = {
    val sigByte = Base16.decode(signature).map(s => signatures.Signature @@ s)
    val keyByte = Base16.decode(key).map(k => signatures.PublicKey @@ k)
    println(sigByte)
    println(keyByte)
    sigByte.fold(_ => false,
      s => keyByte.fold(_ => false,
        k => Curve25519.verify(s, msg.getBytes("UTF-8"), k)
      )
    )
  }
}
