package ch.epfl.pop.tests

import ch.epfl.pop.Validate
import ch.epfl.pop.crypto.Hash
import ch.epfl.pop.json.JsonMessages.{CreateLaoMessageClient, UpdateLaoMessageClient}
import ch.epfl.pop.json.JsonUtils.MessageContentDataBuilder
import ch.epfl.pop.json.{Actions, ByteArray, Hash, MessageContent, Methods, Objects}
import ch.epfl.pop.tests.MessageCreationUtils._
import org.scalatest.FunSuite
import scorex.crypto.signatures.{Curve25519, PrivateKey, PublicKey}

import java.nio.ByteBuffer
import java.security.MessageDigest
class ValidateTest extends FunSuite {

  /* --------------- VALIDATE MESSAGE CONTENT --------------- */

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
    val (_, pk) = Curve25519.createKeyPair(seed)
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

  /* --------------- VALIDATE CREATE LAO --------------- */

  private val laoName = "My LAO"
  private val laoCreation = 229388L
  private val (sk, pk): (PrivateKey, PublicKey) = Curve25519.createKeyPair
  private val laoOrganizer = supertagged.untag(pk)
  private val md = MessageDigest.getInstance("SHA-256")
  md.update(laoOrganizer)
  md.update(ByteBuffer.allocate(8).putLong(laoCreation))
  md.update(laoName.getBytes())
  private val laoId = md.digest()
  private val root = "/root"
  private def createLao(id: Hash = laoId, name: String = laoName, creation: Long = laoCreation,
                        organizer: ByteArray = laoOrganizer, sender: PublicKey = pk): CreateLaoMessageClient = {
    val data = new MessageContentDataBuilder()
      .setHeader(Objects.Lao, Actions.Create)
      .setId(id)
      .setName(name)
      .setCreation(creation)
      .setLastModified(creation)
      .setOrganizer(organizer)
      .setWitnesses(Nil)
      .build()
    val params = getMessageParams(data, sender, sk, root)
    val create = CreateLaoMessageClient(params, 0, Methods.Publish)
    create
  }

  test("Create LAO validation works with correct parameters") {

    val create = createLao()
    assert(Validate.validate(create).isEmpty)
  }

  test("Create LAO validation fails with incorrect id") {
    val create = createLao(id = Array[Byte]())
    assert(Validate.validate(create).isDefined)
  }

  test("Create LAO validation fails with incorrect creation timestamp") {
    val create = createLao(creation = -1)
    assert(Validate.validate(create).isDefined)
  }

  test("Create LAO validation fails with incorrect organizer") {
    val create = createLao(sender = null)
    assert(Validate.validate(create).isDefined)
  }

  /* --------------- VALIDATE UPDATE LAO --------------- */
  private val updateLaoLastModified = laoCreation + 1
  private def updateLao(modified: Long = updateLaoLastModified, sender: PublicKey = pk): UpdateLaoMessageClient = {
    val data = new MessageContentDataBuilder()
      .setHeader(Objects.Lao, Actions.UpdateProperties)
      .setId(laoId)
      .setName(laoName)
      .setCreation(laoCreation)
      .setLastModified(modified)
      .setWitnesses(Nil)
      .build()
    val params = getMessageParams(data, sender, sk, root)
    val update = UpdateLaoMessageClient(params, 0, Methods.Publish)
    update
  }

  test("Update LAO validation works with valid parameters") {
    assert(Validate.validate(updateLao()).isEmpty)
  }

  test("Update LAO valition fails with invalid modification timestamp") {
    assert(Validate.validate(updateLao(modified = -1)).isDefined)
  }

  /* --------------- VALIDATE BROADCAST LAO --------------- */




}
