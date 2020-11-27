package json

import java.util.Base64

import ch.epfl.pop.json.{Actions, ChannelName, Methods, Objects}

object JsonParserTestsUtils {

  val ERROR_MESSAGE: String = "error_for_error_code_above"


  def encodeBase64String(s: String): String = Base64.getEncoder.encode(s.getBytes()).map(_.toChar).mkString


  val MessageContentExample: String = """{
                                        |            "data": "eyJvYmplY3QiOiJsYW8iLCJhY3Rpb24iOiJ1cGRhdGVfcHJvcGVydGllcyIsImlkIjoiWVdGaCIsIm5hbWUiOiJNYSBMYW8iLCJjcmVhdGlvbiI6NDU0NSwibGFzdF9tb2RpZmllZCI6NDU0NSwib3JnYW5pemVyIjoiWW1JIiwid2l0bmVzc2VzIjpbIk1UST0iLCAiTVRNPSJdfQ==",
                                        |            "sender": "NTMwZEU4",
                                        |            "signature": "NTEwMA==",
                                        |            "message_id": "MWQw",
                                        |            "witness_signatures": ["Y2ViMQ==", "Y2ViMg==", "Y2ViMw=="]
                                        |        }""".stripMargin.filterNot((c: Char) => c.isWhitespace)


  val _dataLao: String = s"""{
                            |    "object": "${Objects.Lao.toString}",
                            |    "action": "F_ACTION",
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
                                |    "id": "ODg4",
                                |    "name": "nameMeeting",
                                |    "creation": 333,
                                |    "last_modified": 333,
                                |    "location": "Paris",
                                |    "start": 400,
                                |    "end": 500,
                                |    "extra": "extra_stuff"
                                |}""".stripMargin.filterNot((c: Char) => c.isWhitespace)


  val dataUpdateLao: String = s"""{
                                 |    "object": "${Objects.Lao.toString}",
                                 |    "action": "${Actions.UpdateProperties.toString}",
                                 |    "name": "name6",
                                 |    "last_modified": 2226,
                                 |    "witnesses": ["MTEx", "MTExNgo="]
                                 |}""".stripMargin.filterNot((c: Char) => c.isWhitespace)

  val dataWitnessMessage: String = s"""{
                                      |    "object": "${Objects.Message.toString}",
                                      |    "action": "${Actions.Witness.toString}",
                                      |    "message_id": "YmVlZgo=",
                                      |    "signature": "OTA5MAo="
                                      |}""".stripMargin.filterNot((c: Char) => c.isWhitespace)


  val dataCreateLao: String = _dataLao.replaceAll("F_ACTION", Actions.Create.toString)
  val dataBroadcastLao: String = _dataLao.replaceAll("F_ACTION", Actions.State.toString)
  val dataCreateMeeting: String = _dataMeeting.replaceAll("F_ACTION", Actions.Create.toString)
  val dataBroadcastMeeting: String = _dataMeeting.replaceAll("F_ACTION", Actions.State.toString)


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
       |            "witness_signatures": ["Y2ViMQ==", "Y2ViMg==", "Y2ViMw=="]
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
}
