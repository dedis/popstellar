package ch.epfl.pop.pubsub.graph

import org.scalatest.{Matchers, FunSuite, BeforeAndAfterAll}
import ch.epfl.pop.model.network.JsonRpcRequest
import scala.io.Source
import ch.epfl.pop.model.network.JsonRpcResponse
import ch.epfl.pop.pubsub.graph.validators.RpcValidator
import ch.epfl.pop.model.network.ResultObject
import ch.epfl.pop.pubsub.graph.validator.SchemaValidatorSuite._




class AnswerGeneratorSuite extends FunSuite with Matchers with BeforeAndAfterAll{

  test("Publish: correct response test"){
    val path = "../protocol/examples/query/publish/publish.json"
    val jsonStr = getJsonStringFromFile(path)
    val rpcRequest: JsonRpcRequest = JsonRpcRequest.buildFromJson(jsonStr)
    val message: GraphMessage = Left(rpcRequest)
    val expected: GraphMessage =  Left(JsonRpcResponse(
        RpcValidator.JSON_RPC_VERSION, Some(new ResultObject(0)), None, rpcRequest.id))

    expected should be (message)
  }

  //FIXME: Requires Actor intervention
  test("Catchup: correct response test"){
    val path = "../protocol/examples/query/catchup/catchup.json"
    val jsonStr = getJsonStringFromFile(path)
    val rpcRequest : JsonRpcRequest = JsonRpcRequest.buildFromJson(jsonStr)

    val channel = rpcRequest.getParamsChannel //Channel to be used for Catchup
    val message : GraphMessage = Left(rpcRequest)
    def objectNumber: Int = ??? //Should probably be definid by mocking the DBactor and =3
    val expected =  Left(JsonRpcResponse(
        RpcValidator.JSON_RPC_VERSION, Some(new ResultObject(objectNumber)), None, rpcRequest.id))

    expected should be (message)

  }
}
