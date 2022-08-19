package ch.epfl.pop.model.objects

import org.scalatest.funsuite.{AnyFunSuite => FunSuite}
import org.scalatest.matchers.should.Matchers

class KeyPairSuite extends FunSuite with Matchers {
  test("Encrypt and decrypt a message return the original message") {
    val message = Base64Data.encode("Hello")
    val keypair = KeyPair()
    val encrypted = keypair.encrypt(message)
    val decrypted = keypair.decrypt(encrypted)
    decrypted should equal(message)
  }
  test("Encrypt and decrypt a message too long should crash") {
    val message = Base64Data.encode("H".repeat(30))
    val keypair = KeyPair()
    assertThrows[IllegalArgumentException](keypair.encrypt(message))
  }
}
