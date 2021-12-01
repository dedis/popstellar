package ch.epfl.pop.model.network

import org.scalatest.{FunSuite, Matchers}

class JsonRpcResponseSuite extends FunSuite with Matchers {
    test("Constructor/apply works as intended"){
        val rpc: String = "rpc"
        val res: Option[ResultObject] = Some(new ResultObject(2))
        val err: Option[ErrorObject] = Some(new ErrorObject(1, "Error"))
        val id: Option[Int] = Some(0)

        val response: JsonRpcResponse = JsonRpcResponse(rpc, res, err, id)
        response.jsonrpc should equal(rpc)
        response.result should equal(res)
        response.error should equal(err)
        response.id should equal(id)
    }

    test("isPositive returns right Result"){
        val rpc: String = "rpc"
        val res: Option[ResultObject] = Some(new ResultObject(2))
        val err: Option[ErrorObject] = Some(new ErrorObject(1, "Error"))
        val id: Option[Int] = Some(0)

        val response: JsonRpcResponse = JsonRpcResponse(rpc, res, err, id)
        val response2: JsonRpcResponse = JsonRpcResponse(rpc, None, err, id)

        response.isPositive should equal(true)
        response2.isPositive should equal(false)
    }
}