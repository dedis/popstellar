package ch.epfl.pop.model.network

import org.scalatest.funsuite.{AnyFunSuite => FunSuite}
import org.scalatest.matchers.should.Matchers

class JsonRpcResponseSuite extends FunSuite with Matchers {
  test("Constructor/apply works as intended") {
    val rpc: String = "rpc"
    val res: ResultObject = new ResultObject(2)
    val err: ErrorObject = ErrorObject(1, "Error")
    val id: Option[Int] = Some(0)

    val response: JsonRpcResponse = JsonRpcResponse(rpc, res, id)
    response.jsonrpc should equal(rpc)
    response.result should equal(Some(res))
    response.error should equal(None)
    response.id should equal(id)

    val response2: JsonRpcResponse = JsonRpcResponse(rpc, err, id)
    response2.result should equal(None)
    response2.error should equal(Some(err))
  }

  test("isPositive returns right Result") {
    val rpc: String = "rpc"
    val res: ResultObject = new ResultObject(2)
    val err: ErrorObject = ErrorObject(1, "Error")
    val id: Option[Int] = Some(0)

    val response: JsonRpcResponse = JsonRpcResponse(rpc, res, id)
    val response2: JsonRpcResponse = JsonRpcResponse(rpc, err, id)

    response.isPositive should equal(true)
    response2.isPositive should equal(false)
  }

  test("getId returns the correct rpc id") {
    JsonRpcResponse("", ErrorObject(-1, ""), Some(0)).getId should equal(Some(0))
    JsonRpcResponse("", ErrorObject(-1, ""), None).getId should equal(None)
  }

}
