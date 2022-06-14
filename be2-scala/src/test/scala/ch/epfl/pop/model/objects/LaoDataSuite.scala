package ch.epfl.pop.model.objects

import ch.epfl.pop.model.network.method.message.Message
import com.google.crypto.tink.subtle.Ed25519Sign
import org.scalatest.{FunSuite, Matchers}
import util.examples.MessageExample


class LaoDataSuite extends FunSuite with Matchers {
  final val KEYPAIR: Ed25519Sign.KeyPair = Ed25519Sign.KeyPair.newKeyPair
  final val PUBLICKEY: PublicKey = PublicKey(Base64Data.encode(KEYPAIR.getPublicKey))
  final val PRIVATEKEY: PrivateKey = PrivateKey(Base64Data.encode(KEYPAIR.getPrivateKey))

  test("Apply works with empty/full list for LaoData") {
    val laoData: LaoData = LaoData(PublicKey(Base64Data("cGs=")), List.empty, PRIVATEKEY, PUBLICKEY, List.empty)

    laoData.owner should equal(PublicKey(Base64Data("cGs=")))
    laoData.attendees should equal(List.empty)
    laoData.privateKey should equal(PRIVATEKEY)
    laoData.publicKey should equal(PUBLICKEY)
    laoData.witnesses should equal(List.empty)

    val laoData2: LaoData = LaoData(PublicKey(Base64Data("cGstYQ==")), List(PublicKey(Base64Data("cGstYg=="))), PRIVATEKEY, PUBLICKEY, List.empty)

    laoData2.owner should equal(PublicKey(Base64Data("cGstYQ==")))
    laoData2.attendees should equal(List(PublicKey(Base64Data("cGstYg=="))))
    laoData2.privateKey should equal(PRIVATEKEY)
    laoData2.publicKey should equal(PUBLICKEY)
    laoData2.witnesses should equal(List.empty)
  }

  test("Json conversions work for LaoData") {
    val laoData: LaoData = LaoData(PublicKey(Base64Data("cGs=")), List.empty, PRIVATEKEY, PUBLICKEY, List.empty)

    val laoData2: LaoData = LaoData.buildFromJson(laoData.toJsonString)

    //the checks are done separately, as otherwise equals seems to compare the toString equalities, which is not the case for Arrays
    laoData2.owner should equal(laoData.owner)
    laoData2.attendees should equal(laoData.attendees)
    laoData2.privateKey should equal(laoData.privateKey)
    laoData2.publicKey should equal(laoData.publicKey)
    laoData2.witnesses should equal(laoData.witnesses)
  }

  test("emptyLaoData generates what it should") {
    val emptyData: LaoData = LaoData()

    emptyData.owner should equal(null)
    emptyData.attendees should equal(List.empty)
    // we can't test keypair here
    emptyData.witnesses should equal(List.empty)
  }

  test("getName works") {
    LaoData.getName should equal("LaoData")
  }

  test("LaoData is updated as it should") {
    val messageCloseRollCall: Message = MessageExample.MESSAGE_CLOSEROLLCALL
    val messageCreateLao: Message = MessageExample.MESSAGE_CREATELAO_SIMPLIFIED
    val messageAddChirp: Message = MessageExample.MESSAGE_ADDCHIRP
    val messageWithoutMessageData: Message = MessageExample.MESSAGE
    val address: Option[String] = Option("ws://popdemo.dedis.ch")

    val emptyData: LaoData = LaoData()

    val chirpData: LaoData = emptyData.updateWith(messageAddChirp, address)

    chirpData should equal(emptyData)

    val withoutMessageDataData: LaoData = chirpData.updateWith(messageWithoutMessageData, address)

    withoutMessageDataData should equal(chirpData)

    val createData: LaoData = withoutMessageDataData.updateWith(messageCreateLao, address)

    createData.owner should equal(PublicKey(Base64Data("a2V5")))
    createData.attendees should equal(List(PublicKey(Base64Data("a2V5"))))
    createData.witnesses should equal(List.empty)
    createData.address should equal(address.get)

    val rollCallData: LaoData = createData.updateWith(messageCloseRollCall, address)

    rollCallData.owner should equal(PublicKey(Base64Data("a2V5")))
    rollCallData.attendees should equal(List(PublicKey(Base64Data("a2V5")), PublicKey(Base64Data("a2V5QXR0ZW5kZWU="))))
    rollCallData.witnesses should equal(List.empty)
    rollCallData.address should equal(address.get)

    val rollCallDataNoChange: LaoData = createData.updateWith(messageCloseRollCall, None)

    rollCallDataNoChange.address should equal(address.get)
  }

}
