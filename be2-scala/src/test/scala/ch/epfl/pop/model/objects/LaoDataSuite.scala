package ch.epfl.pop.model.objects

import org.scalatest.{FunSuite, Matchers}
import ch.epfl.pop.model.objects.{Base64Data, Hash, PublicKey}
import ch.epfl.pop.model.network.method.message.Message

import util.examples.MessageExample


class LaoDataSuite extends FunSuite with Matchers {
    test("Apply works with empty/full list for LaoData"){
        val laoData: LaoData = LaoData(PublicKey(Base64Data("a")), List.empty, List.empty)

        laoData.owner should equal (PublicKey(Base64Data("a")))
        laoData.attendees should equal(List.empty)
        laoData.witnesses should equal(List.empty)

        val laoData2: LaoData = LaoData(PublicKey(Base64Data("a")), List(PublicKey(Base64Data("b"))), List.empty)

        laoData2.owner should equal (PublicKey(Base64Data("a")))
        laoData2.attendees should equal(List(PublicKey(Base64Data("b"))))
        laoData2.witnesses should equal(List.empty)
    }

    test("Json conversions work for LaoData") {
        val laoData: LaoData = LaoData(PublicKey(Base64Data("a")), List.empty, List.empty)

        val laoData2: LaoData = LaoData.buildFromJson(laoData.toJsonString)

        laoData2 should equal (laoData)
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

        emptyData should equal(LaoData(null, List.empty, List.empty))
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

        createData should equal(LaoData(PublicKey(Base64Data("key")), List(PublicKey(Base64Data("key"))), List.empty))

        val rollCallData: LaoData = createData.updateWith(messageCloseRollCall)

        rollCallData should equal (LaoData(PublicKey(Base64Data("key")), List(PublicKey(Base64Data("keyAttendee"))), List.empty))

    }

}
