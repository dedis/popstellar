package ch.epfl.pop.model.objects

import org.scalatest.funsuite.{AnyFunSuite => FunSuite}
import org.scalatest.matchers.should.Matchers
import util.examples.LaoDataExample

class AddressSuite extends FunSuite with Matchers {
  test("Address.of works for a public key") {
    val addr = Address.of(LaoDataExample.PUBLICKEY)
    val expected = Address(Base64Data("-Ayu-GvqkKkJlnZk1RWa5zD1zZQ="))

    addr.decode().length shouldBe 20
    addr should equal(expected)
  }
}
