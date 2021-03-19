package ch.epfl.pop.model.network.requests.meeting

import ch.epfl.pop.model.network.{JsonRpcRequest, Method}
import ch.epfl.pop.model.network.method.Params

final case class JsonRpcRequestCreateMeeting(
                                              override val jsonrpc: String,
                                              override val method: Method.Method,
                                              override val params: Params,
                                              override val id: Option[Int]
                                            ) extends JsonRpcRequest(jsonrpc, method, params, id)
