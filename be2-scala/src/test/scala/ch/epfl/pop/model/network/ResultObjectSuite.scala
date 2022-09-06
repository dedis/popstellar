package ch.epfl.pop.model.network

import org.scalatest.funsuite.{AnyFunSuite => FunSuite}
import org.scalatest.matchers.should.Matchers

class ResultObjectSuite extends FunSuite with Matchers {
  test("Int constructor works") {
    val obj: ResultObject = new ResultObject(1)

    obj.resultInt should equal(Some(1))
    obj.resultMessages should equal(None)
  }

  test("List constructor works") {
    val obj: ResultObject = new ResultObject(List.empty)

    obj.resultInt should equal(None)
    obj.resultMessages should equal(Some(List.empty))
  }

  test("isIntResult returns right result") {
    val obj: ResultObject = new ResultObject(1)
    val obj2: ResultObject = new ResultObject(List.empty)

    obj.isIntResult should equal(true)
    obj2.isIntResult should equal(false)
  }

  test("equals works") {
    val obj: ResultObject = new ResultObject(1)
    val obj2: ResultObject = new ResultObject(List.empty)
    val obj3: ResultObject = new ResultObject(1)
    val obj4: ResultObject = new ResultObject(List.empty)

    obj.equals(obj3) should equal(true)
    obj2.equals(obj4) should equal(true)
    obj2.equals(obj) should equal(false)
  }
}
