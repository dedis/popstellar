package ch.epfl.pop.model.objects

import org.scalatest.{FunSuite, Matchers}

class Uint53Suite extends FunSuite with Matchers {
  import Uint53._

  test("MinValue is zero") {
    MinValue should equal(0)
  }

  test("MaxValue computed another way matches") {
    MaxValue should equal((1L<<53) - 1)
  }

  test("inRange examples") {
    inRange(-1) shouldBe false
    inRange(0) shouldBe true
    inRange(MaxValue) shouldBe true
    inRange(MaxValue+1) shouldBe false
  }

  test("safeSum examples case") {
    safeSum(Seq()) shouldBe Right(0)
    safeSum(Seq(MaxValue)) shouldBe Right(MaxValue)
    safeSum(Seq(MaxValue, 1)) shouldBe a[Left[_,_]]
    safeSum(Seq(MaxValue/2, MaxValue/2)) shouldBe Right(MaxValue - 1)
    safeSum(Seq(MaxValue/2, MaxValue/2, 1)) shouldBe Right(MaxValue)
  }

  test("safeSum tricky case 1") {
    val seq = Iterable.fill(2048)(1L<<52)
    assume(seq.sum == Long.MinValue)
    safeSum(seq) shouldBe a[Left[_,_]]
  }

  test("safeSum tricky case 2") {
    val seq = Iterable.fill(4096)(1L<<52)
    assume(seq.sum == 0)
    safeSum(seq) shouldBe a[Left[_,_]]
  }
}
