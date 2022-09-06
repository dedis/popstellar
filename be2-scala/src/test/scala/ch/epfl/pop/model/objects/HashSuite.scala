package ch.epfl.pop.model.objects

import org.scalatest.funsuite.{AnyFunSuite => FunSuite}
import org.scalatest.matchers.should.Matchers

class HashSuite extends FunSuite with Matchers {
  test("Hash 'fromString' works for a random string") {
    val hash: Hash = Hash.sha256Hash("PoP")
    val expected: Hash = Hash(Base64Data("EmE4jL2zWjfJY_BVHYQdX19TtZTQwlqDA--dh_4mo2s="))

    hash should equal(expected)
  }

  test("Hash 'fromStrings' works against expected fe1-web data") {
    val hash: Hash = Hash.fromStrings("abcd", "1234")
    val expected: Hash = Hash(Base64Data("61I7DQkiMtdHFM5VygjbFqrVmn4NAl0wSVxkj6Q5iDw="))

    hash should equal(expected)
  }

  test("Hash 'fromStrings' works with no string") {
    val hash: Hash = Hash.fromStrings()
    val expected: Hash = Hash(Base64Data("47DEQpj8HBSa-_TImW-5JCeuQeRkm5NMpJWZG3hSuFU="))

    hash should equal(expected)
  }

  test("Hash 'fromStrings' works with one string (1)") {
    val hash: Hash = Hash.fromStrings("PoP")
    val expected: Hash = Hash(Base64Data("fAoSEwSI4qx6brSVryjql0GdkwbMNidcIeJHa8TxGR4="))

    hash should equal(expected)
  }

  test("Hash 'fromStrings' works with one string (2)") {
    val hash: Hash = Hash.fromStrings("5tzPu6@22+1")
    val expected: Hash = Hash(Base64Data("j75l9c9eoHUIVnxD-jnablIzRnIDo61TZ-8WQHVQBu0="))

    hash should equal(expected)
  }

  test("Hash 'fromStrings' works with array of strings (1)") {
    val hash: Hash = Hash.fromStrings("salut", "toi")
    val expected: Hash = Hash(Base64Data("d_mpef1MwgCQoph0wjZDFDmhngFMXAAROLp6XNDii5c="))

    hash should equal(expected)
  }

  test("Hash 'fromStrings' works with array of strings (2)") {
    val hash: Hash = Hash.fromStrings("1", "2", "3", "4")
    val expected: Hash = Hash(Base64Data("yru1zNKR23-3CidGmsdNH53tuvyLM0iftp2aPib-KVU="))

    hash should equal(expected)
  }
}
