package ch.epfl.pop.model.objects

import java.nio.charset.StandardCharsets

import com.google.crypto.tink.subtle.Ed25519Sign
import org.scalatest.Inspectors.forEvery
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

/*Helper object for testing*/
case class TestObj(ed_signer: Ed25519Sign, keyPair: Ed25519Sign.KeyPair)

class SignatureSuite extends FunSuite with Matchers with BeforeAndAfterAll {
  // Fixture test
  final val tester = {
    val kpair = Ed25519Sign.KeyPair.newKeyPair()
    val privateKey = kpair.getPrivateKey
    val ed_signer = new Ed25519Sign(privateKey)
    TestObj(ed_signer, kpair)
  }
  // Data used for testing signature
  final val dataTest = Seq("PoP-scala", "HelloWorld", "Not true is false", "😀", "OMEGA \u03A9", "\u03A8", "Non empty can be fully non empty", "Not false is true")

  final val verify_pk = PublicKey(Base64Data.encode(tester.keyPair.getPublicKey))

  /*Provides correct signature for a msg*/
  private def getTrueSignatureTest(msg: String): Signature = {

    val data = msg.getBytes(StandardCharsets.UTF_8)
    val signed_data = tester.ed_signer.sign(data)
    val signature = Signature(Base64Data.encode(signed_data))
    signature
  }

  /*Provides falsified signature for a msg*/
  private def getFalseSignatureTest(msg: String): Signature = {
    getTrueSignatureTest("X" + msg + "Y")
  }

  test("Basic true signature") {
    forEvery(dataTest) {
      (msg: String) =>
        {
          val signature = getTrueSignatureTest(msg)
          // Assertion
          val msg_encoded = Base64Data.encode(msg)
          signature.verify(verify_pk, msg_encoded) should be(true)
        }
    }
  }
  test("Basic true empty message signature (1)") {
    // Empty msg
    val msg = ""
    val signature = getTrueSignatureTest(msg)
    // Assertion
    val msg_encoded = Base64Data.encode(msg)
    signature.verify(verify_pk, msg_encoded) should be(true)
  }

  test("Basic true one letter message signature (1)") {
    /*Single letter*/
    val msg = "A"
    val signature = getTrueSignatureTest(msg)
    // Assertion
    val msg_encoded = Base64Data.encode(msg)
    signature.verify(verify_pk, msg_encoded) should be(true)
  }

  test("Basic false signature") {

    /** Fake signature * */
    forEvery(dataTest) {
      (msg: String) =>
        {
          val signature = getFalseSignatureTest(msg)
          // Assertion
          val msg_encoded = Base64Data.encode(msg)
          signature.verify(verify_pk, msg_encoded) should be(false)
        }
    }
  }

  test("Basic false empty message signature") {
    // Empty msg
    val msg = ""
    val signature = getFalseSignatureTest(msg)
    // Assertion
    val msg_encoded = Base64Data.encode(msg)
    signature.verify(verify_pk, msg_encoded) should be(false)
  }

  test("Basic false one letter message signature") {
    // Single letter
    val msg = "A"
    val signature = getFalseSignatureTest(msg)
    // Assertion
    val msg_encoded = Base64Data.encode(msg)
    signature.verify(verify_pk, msg_encoded) should be(false)
  }
}
