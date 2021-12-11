package ch.epfl.pop.model.network

import org.scalatest.{FunSuite, Matchers}

class JsonRpcResponseSuite extends FunSuite with Matchers {
    test("Constructor/apply works as intended"){
        val rpc: String = "rpc"
        val res: Option[ResultObject] = Some(new ResultObject(2))
        val err: Option[ErrorObject] = Some(new ErrorObject(1, "Error"))
        val id: Option[Int] = Some(0)

        val response: JsonRpcResponse = JsonRpcResponse(rpc, res, id)
        response.jsonrpc should equal(rpc)
        response.result should equal(res)
        response.error should equal(None)
        response.id should equal(id)

        val response2: JsonRpcResponse = JsonRpcResponse(rpc, err, id)
        response2.result should equal(None)
        response2.error should equal(err)
    }

    test("isPositive returns right Result"){
        val rpc: String = "rpc"
        val res: Option[ResultObject] = Some(new ResultObject(2))
        val err: Option[ErrorObject] = Some(new ErrorObject(1, "Error"))
        val id: Option[Int] = Some(0)

        val response: JsonRpcResponse = JsonRpcResponse(rpc, res, id)
        val response2: JsonRpcResponse = JsonRpcResponse(rpc, err, id)

        response.isPositive should equal(true)
        response2.isPositive should equal(false)
    }
}