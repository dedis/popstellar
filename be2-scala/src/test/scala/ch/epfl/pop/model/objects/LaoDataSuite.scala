package ch.epfl.pop.model.objects

import org.scalatest.{FunSuite, Matchers}
import ch.epfl.pop.model.objects.{Base64Data, Hash, PrivateKey, PublicKey}
import ch.epfl.pop.model.network.method.message.Message

import util.examples.MessageExample
import com.google.crypto.tink.subtle.Ed25519Sign


class LaoDataSuite extends FunSuite with Matchers {
    final val KEYPAIR: Ed25519Sign.KeyPair = Ed25519Sign.KeyPair.newKeyPair
    final val PUBLICKEY: PublicKey = PublicKey(Base64Data.encode(KEYPAIR.getPublicKey))
    final val PRIVATEKEY: PrivateKey = PrivateKey(Base64Data.encode(KEYPAIR.getPrivateKey))

    test("Apply works with empty/full list for LaoData"){
        val laoData: LaoData = LaoData(PublicKey(Base64Data("a")), List.empty, PRIVATEKEY, PUBLICKEY, List.empty)

        laoData.owner should equal (PublicKey(Base64Data("a")))
        laoData.attendees should equal(List.empty)
        laoData.privateKey should equal (PRIVATEKEY)
        laoData.publicKey should equal (PUBLICKEY)
        laoData.witnesses should equal(List.empty)

        val laoData2: LaoData = LaoData(PublicKey(Base64Data("a")), List(PublicKey(Base64Data("b"))), PRIVATEKEY, PUBLICKEY, List.empty)

        laoData2.owner should equal (PublicKey(Base64Data("a")))
        laoData2.attendees should equal(List(PublicKey(Base64Data("b"))))
        laoData2.privateKey should equal (PRIVATEKEY)
        laoData2.publicKey should equal (PUBLICKEY)
        laoData2.witnesses should equal(List.empty)
    }

    test("Json conversions work for LaoData") {
        val laoData: LaoData = LaoData(PublicKey(Base64Data("a")), List.empty, PRIVATEKEY, PUBLICKEY, List.empty)

        val laoData2: LaoData = LaoData.buildFromJson(laoData.toJsonString)

        //the checks are done separately, as otherwise equals seems to compare the toString equalities, which is not the case for Arrays
        laoData2.owner should equal (laoData.owner)
        laoData2.attendees should equal(laoData.attendees)
        laoData2.privateKey should equal (laoData.privateKey)
        laoData2.publicKey should equal (laoData.publicKey)
        laoData2.witnesses should equal(laoData.witnesses)
    }

    test("LaoData is affected by CloseRollCall and CreateLao messages") {
        val messageCloseRollCall: Message = MessageExample.MESSAGE_CLOSEROLLCALL
        val messageCreateLao: Message = MessageExample.MESSAGE_CREATELAO

        LaoData.isAffectedBy(messageCloseRollCall) should equal (true)
        LaoData.isAffectedBy(messageCreateLao) should equal (true)
    }

    test("LaoData is not affected by some other type of message"){
        val messageWithoutMessageData: Message = MessageExample.MESSAGE
        val messageAddChirp: Message = MessageExample.MESSAGE_ADDCHIRP

        LaoData.isAffectedBy(messageWithoutMessageData) should equal (false)
        LaoData.isAffectedBy(messageAddChirp) should equal (false)
    }

    test("emptyLaoData generates what it should"){
        val emptyData: LaoData = LaoData.emptyLaoData

        emptyData.owner should equal (null)
        emptyData.attendees should equal(List.empty)
        // we can't test keypair here
        emptyData.witnesses should equal(List.empty)
    }

    test("getName works"){
        LaoData.getName should equal("LaoData")
    }

    test("LaoData is updated as it should"){
        val messageCloseRollCall: Message = MessageExample.MESSAGE_CLOSEROLLCALL
        val messageCreateLao: Message = MessageExample.MESSAGE_CREATELAO
        val messageAddChirp: Message = MessageExample.MESSAGE_ADDCHIRP
        val messageWithoutMessageData: Message = MessageExample.MESSAGE

        val emptyData: LaoData = LaoData.emptyLaoData

        val chirpData: LaoData = emptyData.updateWith(messageAddChirp)

        chirpData should equal(emptyData)

        val withoutMessageDataData: LaoData = chirpData.updateWith(messageWithoutMessageData)

        withoutMessageDataData should equal (chirpData)

        val createData: LaoData = withoutMessageDataData.updateWith(messageCreateLao)

        createData.owner should equal (PublicKey(Base64Data("key")))
        createData.attendees should equal(List(PublicKey(Base64Data("key"))))
        createData.witnesses should equal(List.empty)

        val rollCallData: LaoData = createData.updateWith(messageCloseRollCall)

        rollCallData.owner should equal (PublicKey(Base64Data("key")))
        rollCallData.attendees should equal(List(PublicKey(Base64Data("keyAttendee"))))
        rollCallData.witnesses should equal(List.empty)

    }

}
