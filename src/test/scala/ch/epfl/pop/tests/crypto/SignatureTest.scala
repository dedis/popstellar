package ch.epfl.pop.tests.crypto

import java.nio.charset.StandardCharsets

import ch.epfl.pop.crypto.Signature
import org.scalatest.FunSuite
import scorex.crypto.signatures.Curve25519

class SignatureTest extends FunSuite {

  test("Verify correct signature") {
    val seed = "PoP".getBytes
    val msg = "{\"ts\": 1604911874, \"type\":\"rollcall\"}"
    val (sk, pk) = Curve25519.createKeyPair(seed)
    val sig = Curve25519.sign(sk, msg.getBytes(StandardCharsets.UTF_8))

    assert(Signature.verify(msg, sig, pk))
  }

  test("Signature verification fails on incorrect signature") {
    val seed = "PoP".getBytes
    val msg = "{\"ts\": 1604911874, \"type\":\"rollcall\"}"
    val (_, pk) = Curve25519.createKeyPair(seed)
    val sig = "incorrect signature".getBytes()

    assert(!Signature.verify(msg, sig, pk))
  }

}
