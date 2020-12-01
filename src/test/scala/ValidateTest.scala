import ch.epfl.pop.Validate
import ch.epfl.pop.crypto.Hash
import ch.epfl.pop.json.MessageContent
import org.scalatest.FunSuite
import scorex.crypto.signatures.Curve25519
class ValidateTest extends FunSuite {

  test("Validate message content when correct") {
    val seed = "PoP".getBytes
    val (sk, pk) = Curve25519.createKeyPair(seed)
    val encodedData = "This is my message"
    val signature = Curve25519.sign(sk, encodedData.getBytes)
    val id = Hash.computeMessageId(encodedData, signature)
    val witnessSignatures = Nil
    val content: MessageContent = MessageContent(encodedData, null, pk, signature, id, witnessSignatures)
    assert(Validate.validate(content).isEmpty)
  }

  test("Validate message content fails with incorrect signature") {
    val seed = "PoP".getBytes
    val (sk, pk) = Curve25519.createKeyPair(seed)
    val encodedData = "This is my message"
    val signature = "incorrect signature".getBytes
    val id = Hash.computeMessageId(encodedData, signature)
    val witnessSignatures = Nil
    val content: MessageContent = MessageContent(encodedData, null, pk, signature, id, witnessSignatures)
    assert(Validate.validate(content).nonEmpty)
  }

  test("Validate message content fails with incorrect id") {
    val seed = "PoP".getBytes
    val (sk, pk) = Curve25519.createKeyPair(seed)
    val encodedData = "This is my message"
    val signature = Curve25519.sign(sk, encodedData.getBytes)
    val id = Hash.computeMessageId(encodedData, "incorrect".getBytes)
    val witnessSignatures = Nil
    val content: MessageContent = MessageContent(encodedData, null, pk, signature, id, witnessSignatures)
    assert(Validate.validate(content).nonEmpty)
  }





}
