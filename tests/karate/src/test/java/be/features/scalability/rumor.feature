Feature: Rumor

  @name=rumor_simple
  Scenario:
    * def sleep =
    """
    function(seconds){
      for(i = 0; i <= seconds; i++)
      {
        java.lang.Thread.sleep(1*1000);
        karate.log(i);
      }
    }
    """
    * def createMockClient =
      """
        function(frontendUrl){
          var MockClient = Java.type('common.utils.MockClient')
          var client = new MockClient(frontendUrl)
          return client
        }
      """
    * def GoServer = Java.type("be.utils.GoServer")
    * def ScalaServer = Java.type("be.utils.ScalaServer")
    * def createGoServer =
      """
        function(host, clientPort, serverPort, authPort){
          return new GoServer(host, clientPort, serverPort, authPort, null, null);
        }
      """
    * def createScalaServer =
      """
        function(host, port){
          return new ScalaServer(host, port, null, null);
        }
      """
    * def goServer = createGoServer("localhost", 8080, 8081, 8082)
    * def scalaServer = createScalaServer("localhost", 8083)
    * goServer.pairWith(scalaServer)
    * goServer.start()
    * scalaServer.start()
    * sleep(5)
    * def clientGo = createMockClient("ws://localhost:8080/client")
    * def clientScala = createMockClient("ws://localhost:8083/client")
    * def lao = clientGo.createLao()
    * sleep(5)
    And def subscribe =
    """
      {
        "method": "subscribe",
        "id": 2,
        "params": {
            "channel": '#(lao.channel)',
        },
        "jsonrpc": "2.0"
      }
    """
    * karate.log("sending a subscribe to lao channel:\n", karate.pretty(subscribe))
    * clientScala.send(subscribe)
    * def resSub = clientScala.takeTimeout(timeout)
    * karate.log("subscribe response:\n", karate.pretty(resSub))

    And def catchup =
      """
        {
          "method": "catchup",
          "id": 5,
          "params": {
              "channel": '#(lao.channel)',
            },
          "jsonrpc": "2.0"
        }
      """
    * karate.log("sending a catchup to lao channel:\n", karate.pretty(catchup))
    * clientScala.send(catchup)
    * def catchupRes = clientScala.takeTimeout(timeout)
    * karate.log("catchup response:\n", karate.pretty(catchupRes))
    * clientGo.close()
    * clientScala.close()
    * goServer.stop()
    * scalaServer.stop()
