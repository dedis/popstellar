package ch.epfl.pop.model.network.requests.lao

import ch.epfl.pop.model.network.method.Params
import ch.epfl.pop.model.network.{JsonRpcRequest, Method}

final case class JsonRpcRequestCreateLao(
                                          override val jsonrpc: String,
                                          override val method: Method.Method,
                                          override val params: Params,
                                          override val id: Option[Int]
                                        ) extends JsonRpcRequest(jsonrpc, method, params, id)
