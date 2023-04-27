package ch.epfl.pop.model.network

import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.objects.Channel
import org.scalatest.funsuite.{AnyFunSuite => FunSuite}
import org.scalatest.matchers.should.Matchers

class ResultObjectSuite extends FunSuite with Matchers {
  test("Int constructor works") {
    val obj: ResultObject = new ResultObject(1)

    obj.resultInt should equal(Some(1))
    obj.resultMessages should equal(None)
    obj.resultMap should equal(None)
  }

  test("List constructor works") {
    val obj: ResultObject = new ResultObject(List.empty)

    obj.resultInt should equal(None)
    obj.resultMap should equal(None)
    obj.resultMessages should equal(Some(List.empty))
  }

  test("Map constructor works") {
    val obj: ResultObject = new ResultObject(Map[Channel, Set[Message]]())

    obj.resultInt should equal(None)
    obj.resultMessages should equal(None)
    obj.resultMap should equal(Some(Map.empty))

  }

  test("isIntResult returns right result") {
    val obj: ResultObject = new ResultObject(1)
    val obj2: ResultObject = new ResultObject(List.empty)
    val obj3: ResultObject = new ResultObject(Map[Channel, Set[Message]]())

    obj.isIntResult should equal(true)
    obj2.isIntResult should equal(false)
    obj3.isIntResult should equal(false)

  }

  test("equals works") {
    val obj: ResultObject = new ResultObject(1)
    val obj2: ResultObject = new ResultObject(List.empty)
    val obj5: ResultObject = new ResultObject(Map[Channel, Set[Message]]())
    val obj3: ResultObject = new ResultObject(1)
    val obj4: ResultObject = new ResultObject(List.empty)
    val obj6: ResultObject = new ResultObject(Map[Channel, Set[Message]]())

    obj.equals(obj3) should equal(true)
    obj2.equals(obj4) should equal(true)
    obj2.equals(obj) should equal(false)
    obj5.equals(obj6) should equal(true)
    obj5.equals(obj) should equal(false)
    obj6.equals(obj2) should equal(false)
  }
}
