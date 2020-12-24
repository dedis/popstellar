package ch.epfl.pop.tests.json

import java.util.Base64

import ch.epfl.pop.json.JsonMessages.JsonMessage
import ch.epfl.pop.json.JsonUtils.JsonMessageParserError
import ch.epfl.pop.json.{Actions, ChannelName, JsonMessageParser, Methods, Objects}

import scala.util.Random
import org.scalatest.{FunSuite, Matchers}


object JsonParserTestsUtils extends FunSuite with Matchers {

  val ERROR_MESSAGE: String = "error_for_error_code_above"


  def encodeBase64String(s: String): String = Base64.getEncoder.encode(s.getBytes()).map(_.toChar).mkString


  val MessageContentExample: String = """{
                                        |            "data": "eyJvYmplY3QiOiJsYW8iLCJhY3Rpb24iOiJ1cGRhdGVfcHJvcGVydGllcyIsImlkIjoiWVdGaCIsIm5hbWUiOiJNYSBMYW8iLCJjcmVhdGlvbiI6NDU0NSwibGFzdF9tb2RpZmllZCI6NDU0NSwib3JnYW5pemVyIjoiWW1JIiwid2l0bmVzc2VzIjpbIk1UST0iLCAiTVRNPSJdfQ==",
                                        |            "sender": "NTMwZEU4",
                                        |            "signature": "NTEwMA==",
                                        |            "message_id": "MWQw",
                                        |            "witness_signatures": [
                                        |              { "signature": "Y2ViMQ==", "witness": "Y2ViX3Byb3BfMQ==" },
                                        |              { "signature": "Y2ViMg==", "witness": "Y2ViX3Byb3BfMg==" },
                                        |              { "signature": "Y2ViMw==", "witness": "Y2ViX3Byb3BfMw==" }
                                        |            ]
                                        |        }""".stripMargin.filterNot((c: Char) => c.isWhitespace)


  val _dataLao: String = s"""{
                            |    "object": "${Objects.Lao.toString}",
                            |    "action": "F_ACTION",
                            |    FF_MODIFICATION
                            |    "id": "OTk5",
                            |    "name": "name",
                            |    "creation": 222,
                            |    "last_modified": 222,
                            |    "organizer": "OTA5",
                            |    "witnesses": ["MTEx"]
                            |}""".stripMargin.filterNot((c: Char) => c.isWhitespace)

  val _dataMeeting: String = s"""{
                                |    "object": "${Objects.Meeting.toString}",
                                |    "action": "F_ACTION",
                                |    FF_MODIFICATION
                                |    "id": "ODg4",
                                |    "name": "nameMeeting",
                                |    "creation": 333,
                                |    "last_modified": 333,
                                |    "location": "Paris",
                                |    "start": 400,
                                |    "end": 500,
                                |    "extra": "extra_stuff"
                                |}""".stripMargin.filterNot((c: Char) => c.isWhitespace)


  val _dataRollCall: String = s"""{
                            |    "object": "${Objects.RollCall.toString}",
                            |    "action": "F_ACTION",
                            |    FF_MODIFICATION
                            |    "id": "OTk5"
                            |}""".stripMargin.filterNot((c: Char) => c.isWhitespace)


  val dataUpdateLao: String = s"""{
                                 |    "object": "${Objects.Lao.toString}",
                                 |    "action": "${Actions.UpdateProperties.toString}",
                                 |    "name": "name6",
                                 |    "id": "ODg4",
                                 |    "last_modified": 2226,
                                 |    "witnesses": ["MTEx", "MTExNgo="]
                                 |}""".stripMargin.filterNot((c: Char) => c.isWhitespace)

  val dataWitnessMessage: String = s"""{
                                      |    "object": "${Objects.Message.toString}",
                                      |    "action": "${Actions.Witness.toString}",
                                      |    "message_id": "YmVlZgo=",
                                      |    "signature": "OTA5MAo="
                                      |}""".stripMargin.filterNot((c: Char) => c.isWhitespace)


  val dataCreateLao: String = _dataLao
    .replaceFirst("F_ACTION", Actions.Create.toString)
    .replaceFirst("FF_MODIFICATION", "")
    .replaceFirst("\"last_modified\":[0-9]*,", "")
  val dataBroadcastLao: String = _dataLao
    .replaceFirst("F_ACTION", Actions.State.toString)
    .replaceFirst(
      "FF_MODIFICATION",
      "\"modification_id\":\"NDU2\",\"modification_signatures\":[{\"witness\":\"Y2xlZjE=\",\"signature\":\"amUgc2lnbmU=\"},{\"witness\":\"Y2xlZjI=\",\"signature\":\"amUgc2lnbmUgYXVzc2k=\"}],"
    )
  val dataCreateMeeting: String = _dataMeeting
    .replaceFirst("F_ACTION", Actions.Create.toString)
    .replaceFirst("FF_MODIFICATION", "")
    .replaceFirst("\"last_modified\":[0-9]*,", "")
  val dataBroadcastMeeting: String = _dataMeeting
    .replaceFirst("F_ACTION", Actions.State.toString)
    .replaceFirst(
      "FF_MODIFICATION",
      "\"modification_id\":\"NDU2\",\"modification_signatures\":[{\"witness\":\"Y2xlZjE=\",\"signature\":\"amUgc2lnbmU=\"},{\"witness\":\"Y2xlZjI=\",\"signature\":\"amUgc2lnbmUgYXVzc2k=\"}],"
    )
  val dataCreateRollCall: String = _dataRollCall
    .replaceFirst("F_ACTION", Actions.Create.toString)
    .replaceFirst("FF_MODIFICATION", "\"name\":\"MonRollCall\",\"creation\":1234,\"start\":1500,\"scheduled\":1450,\"location\":\"INF\",\"roll_call_description\":\"description\",")
  val dataOpenRollCall: String = _dataRollCall
    .replaceFirst("F_ACTION", Actions.Open.toString)
    .replaceFirst("FF_MODIFICATION", "\"start\":2000,")
  val dataReopenRollCall: String = _dataRollCall
    .replaceFirst("F_ACTION", Actions.Reopen.toString)
    .replaceFirst("FF_MODIFICATION", "\"start\":3000,")
  val dataCloseRollCall: String = _dataRollCall
    .replaceFirst("F_ACTION", Actions.Close.toString)
    .replaceFirst("FF_MODIFICATION", "\"start\":4000,\"end\":5000,\"attendees\":[\"Y2xlZjE=\",\"Y2xlZjI=\"],")


  def embeddedMessage(
                       data: String,
                       method: Methods.Methods = Methods.Publish,
                       channel: ChannelName = "/root/lao_id",
                       id: Int = 0
                     ): String = {
    s"""{
       |    "jsonrpc": "2.0",
       |    "method": "${method.toString}",
       |    "params": {
       |        "channel": "$channel",
       |        "message": {
       |            "data": "${Base64.getEncoder.encode(data.getBytes).map(_.toChar).mkString}",
       |            "sender": "NTMwZEU4",
       |            "signature": "NTEwMA==",
       |            "message_id": "MWQw",
       |            "witness_signatures": [
       |              { "signature": "Y2ViMQ==", "witness": "Y2ViX3Byb3BfMQ==" },
       |              { "signature": "Y2ViMg==", "witness": "Y2ViX3Byb3BfMg==" },
       |              { "signature": "Y2ViMw==", "witness": "Y2ViX3Byb3BfMw==" }
       |            ]
       |        }
       |    },
       |    "id": $id
       |  }
       |""".stripMargin.filterNot((c: Char) => c.isWhitespace)
  }

  def embeddedServerAnswer(result: Option[Int], error: Option[String] = Some(ERROR_MESSAGE), code: Int = 0, id: Int = 0): String = {
    (result, error) match {
      case (Some(r), None) =>
        val additional: String = s""""result": $r"""
        s"""{
           |    "id": $id,
           |    "jsonrpc": "2.0",
           |    $additional
           |  }
           |""".stripMargin.filterNot((c: Char) => c.isWhitespace)
      case (None, Some(e)) =>
        val additional: String = s""""error": {"code": $code, "description": "$e"}"""
        s"""{
           |    $additional,
           |    "id": $id,
           |    "jsonrpc": "2.0"
           |  }
           |""".stripMargin.filterNot((c: Char) => c.isWhitespace)
      case _ => throw new IllegalArgumentException("Impossible argument combination in embeddedServerAnswer")
    }
  }

  def checkBogusInputs(source: String): Unit = {

    val RANDOM_GENERATOR = new Random(2020)

    def altereString(str: String): String = {
      if (str.isEmpty) "alteredString"
      else str.tail
    }

    def randomInt: Int = RANDOM_GENERATOR.nextInt()

    val arrEmpty: String = """[]"""
    val arr: String = """["MTu", "T0x"]"""

    def patternString(kw: String): String = s""""$kw":"[^,]*""""
    def patternAfterString(kw: String, newValue: String): String = s""""$kw":$newValue"""

    def performBogusTest(s: String): Unit = {
      JsonMessageParser.parseMessage(s) match {
        case Left(_) => fail()
        case Right(e) => e shouldBe a [JsonMessageParserError]
      }
    }

    def checkBatchTestsString(kw: String): Unit = {
      val pattern: String = s""""$kw":"[^,]*""""
      def patternAfter(newValue: String): String = s""""$kw":$newValue"""

      if (source.contains(kw)) {
        performBogusTest(source.replaceFirst(pattern, patternAfter("\"3.0\"")))
        performBogusTest(source.replaceFirst(pattern, patternAfter("\"string\"")))
        performBogusTest(source.replaceFirst(pattern, patternAfter("2.0")))
        performBogusTest(source.replaceFirst(pattern, patternAfter("")))
        performBogusTest(source.replaceFirst(pattern, patternAfter("s|{@sopOIJ34≠")))
        performBogusTest(source.replaceFirst(pattern, patternAfter(arrEmpty)))
        performBogusTest(source.replaceFirst(pattern, patternAfter(arr)))
        performBogusTest(source.replaceFirst(pattern, patternAfter(randomInt.toString)))
      }
    }

    def checkBatchTestsInt(kw: String, canBeNull: Boolean = false): Unit = {
      val pattern: String = s""""$kw":[0-9]*"""
      def patternAfter(newValue: String): String = s""""$kw":$newValue"""

      if (source.contains(kw)) {
        performBogusTest(source.replaceFirst(pattern, patternAfter("\"3.0\"")))
        performBogusTest(source.replaceFirst(pattern, patternAfter("\"3\"")))
        performBogusTest(source.replaceFirst(pattern, patternAfter("\"string\"")))
        if (!canBeNull) performBogusTest(source.replaceFirst(pattern, patternAfter("")))
        performBogusTest(source.replaceFirst(pattern, patternAfter("s|{@sopOIJ34≠")))
      }
    }

    def checkBatchWithArrays(source: String, kw: String, pattern: String): Unit = {
      performBogusTest(source.replaceFirst(pattern, patternAfterString(kw, randomInt.toString)))
      performBogusTest(source.replaceFirst(pattern, patternAfterString(kw, arrEmpty)))
      performBogusTest(source.replaceFirst(pattern, patternAfterString(kw, arr)))
    }

    def checkBatchWithNonBase64(source: String, kw: String): Unit = {
      performBogusTest(source.replaceFirst(patternString(kw), patternAfterString(kw, "not-A-Base64-String")))
    }

    if (source.contains("jsonrpc")) {
      val kw: String = "jsonrpc"
      checkBatchTestsString(kw)
    }
    if (source.contains("\"method\":")) checkBatchTestsString("method")
    if (source.contains("\"id\":") && source.contains("\"error\":")) checkBatchTestsInt("id", canBeNull = true)
    else if (source.contains("\"id\":")) checkBatchTestsInt("id")

    if (source.contains("\"params\":")) {
      if (source.contains("\"channel\":")) {
        val kw: String = "channel"
        val pattern: String = """"KW":"[^,]*"""".replaceFirst("KW", kw)

        checkBatchWithArrays(source, kw, pattern)
      }

      if (source.contains("\"message\":")) {
        if (source.contains("\"data\":")) {
          checkBatchWithNonBase64(source, "data")

          // could also check inside
        }
        if (source.contains("\"sender\":")) checkBatchWithNonBase64(source, "sender")
        if (source.contains("\"signature\":")) checkBatchWithNonBase64(source, "signature")
        if (source.contains("\"message_id\":")) checkBatchWithNonBase64(source, "message_id")
        if (source.contains("\"witness_signatures\":")) {
          // could check that all values are base64 strings
        }
      }
    }
    if (source.contains("\"result\":")) checkBatchTestsInt("result")
    if (source.contains("\"error\":")) { /* no bogus test possible : checks done later */ }
  }
}
