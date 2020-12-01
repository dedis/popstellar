package crypto

import java.math.BigInteger
import java.util.Arrays

import ch.epfl.pop.crypto.Hash
import org.scalatest.FunSuite
import scorex.crypto.signatures.{Curve25519, PublicKey}

class HashTest extends FunSuite{

  test("Verify that the message id computed is correct") {
    val msg = "{\"type\":\"rollcall\"}"
    val signature = "signature"
    val correctHash = new BigInteger("01c55bfd46fbc1577de65d86ff117440280b460f2c2e26845326cc10588c3d44", 16).toByteArray
    assert(Arrays.equals(Hash.computeMessageId(msg, signature.getBytes), correctHash))
  }

}
