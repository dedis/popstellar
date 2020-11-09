package crypto

import ch.epfl.pop.Signature
import org.scalatest.FunSuite
import scorex.crypto.signatures.Curve25519
import scorex.util.encode.Base16

class SignatureTest extends FunSuite {

  test("Verify correct signature") {
    val seed = "PoP".getBytes
    val msg = "{\"ts\": 1604911874, \"type\":\"rollcall\"}"
    val (sk, pk) = Curve25519.createKeyPair(seed)
    val sig = Curve25519.sign(sk, msg.getBytes("UTF-8"))
    val sigHex = Base16.encode(sig)
    val keyHex = Base16.encode(pk)

    assert(Signature.verify(msg, sigHex, keyHex))
  }

  test("Signature verification fails on incorrect signature") {
    val seed = "PoP".getBytes
    val msg = "{\"ts\": 1604911874, \"type\":\"rollcall\"}"
    val (_, pk) = Curve25519.createKeyPair(seed)
    val sigHex = Base16.encode("incorrect signature".getBytes())
    val keyHex = Base16.encode(pk)

    assert(!Signature.verify(msg, sigHex, keyHex))
  }

}
