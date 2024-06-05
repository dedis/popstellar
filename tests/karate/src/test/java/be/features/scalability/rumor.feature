@env=scalability
Feature: Rumor
  Background:
    * def LinearServerNetwork = Java.type("be.utils.LinearServerNetwork")
    * def network = LinearServerNetwork.withOnlyGoServers(2)
    * def inputClient = network.getInputNodeClient()
    * def outputClient = network.getOutputNodeClient()
    * configure afterScenario =
      """
        function() {
          outputClient.close()
          inputClient.close()
          network.stopAll()
          network.cleanAll()
        }
      """

  @name=rumor_simple
  Scenario:
    * def lao = inputClient.createLao()
    * java.lang.Thread.sleep(5000)
    * def subscribeRequest = outputClient.subscribeToChannel(lao.channel)
    * karate.log("subscribe request:\n", karate.pretty(subscribeRequest))
    * def subscribeResponse = outputClient.takeTimeout(timeout)
    * karate.log("subscribe response:\n", karate.pretty(subscribeResponse))
    * def catchupRequest = outputClient.catchupToChannel(lao.channel)
    * karate.log("catchup request:\n", karate.pretty(catchupRequest))
    * def catchupResponse = outputClient.takeTimeout(timeout)
    * karate.log("catchup response:\n", karate.pretty(catchupResponse))
