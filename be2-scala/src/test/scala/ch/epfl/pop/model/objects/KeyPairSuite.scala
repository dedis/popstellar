package ch.epfl.pop.model.objects

import org.scalatest.{FunSuite, Matchers}

class KeyPairSuite extends FunSuite with Matchers {
  test("Encrypt and decrypt a message return the original message") {
    val message = Base64Data.encode("Hello")
    val keypair = KeyPair()
    val encrypted = keypair.encrypt(message)
    keypair.decrypt(encrypted) should equal(message)
  }
}
