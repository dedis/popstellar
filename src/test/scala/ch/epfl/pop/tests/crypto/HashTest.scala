package ch.epfl.pop.tests.crypto

import java.math.BigInteger
import java.util.Arrays
import ch.epfl.pop.crypto.Hash
import org.scalatest.FunSuite
import scorex.crypto.signatures.{Curve25519, PublicKey}

import java.nio.charset.StandardCharsets

class HashTest extends FunSuite{

  test("Verify that the message id computed is correct") {
    val msg = "{\"type\":\"rollcall\"}"
    val signature = "signature" // Base64 representation is c2lnbmF0dXJl
    //SHA-256 hash of ["{\"type\":\"rollcall\"}","c2lnbmF0dXJl"]
    val correctHash = new BigInteger("d8a4fc4cd72c82df967c932c3182c976772f2f517e84c0659e73d2ca2776bcdb", 16).toByteArray.tail
    assert(correctHash.length == 32)
    assert(Arrays.equals(Hash.computeMessageId(msg, signature.getBytes(StandardCharsets.UTF_8)), correctHash))
  }

}
