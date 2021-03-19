package ch.epfl.pop.model.network.requests.rollCall

import ch.epfl.pop.model.network.{JsonRpcRequest, Method}
import ch.epfl.pop.model.network.method.Params

final case class JsonRpcRequestOpenRollCall(
                                             override val jsonrpc: String,
                                             override val method: Method.Method,
                                             override val params: Params,
                                             override val id: Option[Int]
                                           ) extends JsonRpcRequest(jsonrpc, method, params, id)
