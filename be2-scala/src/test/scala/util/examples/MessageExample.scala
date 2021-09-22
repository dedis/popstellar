package util.examples

import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.objects.{Base64Data, Hash, PublicKey, Signature, WitnessSignaturePair}

object MessageExample {

  final val MESSAGE: Message = Message(
    Base64Data("eyJjcmVhdGlvbiI6MTYzMTg4NzQ5NiwiaWQiOiJ4aWdzV0ZlUG1veGxkd2txMUt1b0wzT1ZhODl4amdYalRPZEJnSldjR1drPSIsIm5hbWUiOiJoZ2dnZ2dnIiwib3JnYW5pemVyIjoidG9fa2xaTHRpSFY0NDZGdjk4T0xOZE5taS1FUDVPYVR0YkJrb3RUWUxpYz0iLCJ3aXRuZXNzZXMiOltdLCJvYmplY3QiOiJsYW8iLCJhY3Rpb24iOiJjcmVhdGUifQ=="),
    PublicKey(Base64Data("to_klZLtiHV446Fv98OLNdNmi-EP5OaTtbBkotTYLic=")),
    Signature(Base64Data("2VDJCWg11eNPUvZOnvq5YhqqIKLBcik45n-6o87aUKefmiywagivzD4o_YmjWHzYcb9qg-OgDBZbBNWSUgJICA==")),
    Hash(Base64Data("f1jTxH8TU2UGUBnikGU3wRTHjhOmIEQVmxZBK55QpsE=")),
    WitnessSignaturePair(PublicKey(Base64Data("wit1")), Signature(Base64Data("sig1"))) :: WitnessSignaturePair(PublicKey(Base64Data("wit2")), Signature(Base64Data("sig2"))) :: Nil
  )

  final val MESSAGE_FAULTY_ID: Message = Message(
    Base64Data("eyJjcmVhdGlvbiI6MTYzMTg4NzQ5NiwiaWQiOiJ4aWdzV0ZlUG1veGxkd2txMUt1b0wzT1ZhODl4amdYalRPZEJnSldjR1drPSIsIm5hbWUiOiJoZ2dnZ2dnIiwib3JnYW5pemVyIjoidG9fa2xaTHRpSFY0NDZGdjk4T0xOZE5taS1FUDVPYVR0YkJrb3RUWUxpYz0iLCJ3aXRuZXNzZXMiOltdLCJvYmplY3QiOiJsYW8iLCJhY3Rpb24iOiJjcmVhdGUifQ=="),
    PublicKey(Base64Data("to_klZLtiHV446Fv98OLNdNmi-EP5OaTtbBkotTYLic=")),
    Signature(Base64Data("2VDJCWg11eNPUvZOnvq5YhqqIKLBcik45n-6o87aUKefmiywagivzD4o_YmjWHzYcb9qg-OgDBZbBNWSUgJICA==")),
    Hash(Base64Data("FAULTY_ID")),
    WitnessSignaturePair(PublicKey(Base64Data("wit1")), Signature(Base64Data("sig1"))) :: WitnessSignaturePair(PublicKey(Base64Data("wit2")), Signature(Base64Data("sig2"))) :: Nil
  )
}
