package com.github.dedis.popstellar.model.network.serializer.network;

import com.google.gson.JsonObject;

public class JSONRPCRequest {

  private String jsonrpc;

  private String method;

  private JsonObject params;

  public JSONRPCRequest(String jsonrpc, String method, JsonObject params) {
    this.jsonrpc = jsonrpc;
    this.method = method;
    this.params = params;
  }

  public String getJsonrpc() {
    return jsonrpc;
  }

  public void setJsonrpc(String jsonrpc) {
    this.jsonrpc = jsonrpc;
  }

  public String getMethod() {
    return method;
  }

  public void setMethod(String method) {
    this.method = method;
  }

  public JsonObject getParams() {
    return params;
  }

  public void setParams(JsonObject params) {
    this.params = params;
  }
}
