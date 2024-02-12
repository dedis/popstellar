package com.github.dedis.popstellar.model.network.serializer.network

import com.google.gson.JsonObject

class JSONRPCRequest(var jsonrpc: String, var method: String, var params: JsonObject)
