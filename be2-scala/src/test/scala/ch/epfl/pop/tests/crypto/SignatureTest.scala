package ch.epfl.pop.tests.crypto

import ch.epfl.pop.crypto.Signature
import ch.epfl.pop.tests.MessageCreationUtils.{b64EncodeToString, generateKeyPair, sign}
import org.scalatest.FunSuite
import java.nio.charset.StandardCharsets.UTF_8

class SignatureTest extends FunSuite {

  test("Verify correct signature") {
    val msg = "{\"ts\": 1604911874, \"type\":\"rollcall\"}"
    val kp = generateKeyPair()
    val sig = sign(kp, msg.getBytes(UTF_8))

    val msgEncoded = b64EncodeToString(msg.getBytes(UTF_8))
    assert(Signature.verify(msgEncoded, sig, kp.getPublicKey))
  }

  test("Signature verification fails on incorrect signature") {
    val msg = "{\"ts\": 1604911874, \"type\":\"rollcall\"}"
    val pk = generateKeyPair().getPublicKey
    val sig = "incorrect signature".getBytes()

    val msgEncoded = b64EncodeToString(msg.getBytes(UTF_8))
    assert(!Signature.verify(msgEncoded, sig, pk))
  }
}
