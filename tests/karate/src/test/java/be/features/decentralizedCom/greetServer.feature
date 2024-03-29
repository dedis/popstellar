@env=go,scala
Feature: Send greetServer messages to other servers

  Background:
    * call read('classpath:be/features/utils/constants.feature')
    * call read(serverFeature)
    * call read(mockClientFeature)
    * def mockBackend = call createMockBackend

  @greetServer1
  Scenario: Server should reply to a greetServer message with its own greetServer
      Given def validGreetServer =
        """
          {
            "jsonrpc": "2.0",
            "method": "greet_server",
            "params": {
              "public_key": "#(mockBackend.publicKey)",
              "client_address": "#(frontendWsURL)",
              "server_address": "#(backendWsURL)"
              }
          }
        """
      When mockBackend.send(validGreetServer)
      And karate.log("sending a greetServer :\n", karate.pretty(validGreetServer))
      And def greetServers = mockBackend.getGreetServer()
      Then assert greetServers.length == 1
      And json greetServer = greetServers[0]

  @greetServer2
  Scenario: Server should not reply to a greetServer message with invalid public key (not base64 encoded)
    Given def invalidGreetServer =
        """
          {
            "jsonrpc": "2.0",
            "method": "greet_server",
            "params": {
              "public_key": "invalid",
              "client_address": "#(frontendWsURL)",
              "server_address": "#(backendWsURL)"
              }
          }
        """
    When mockBackend.send(invalidGreetServer)
    And karate.log("sending a greetServer :\n", karate.pretty(invalidGreetServer))
    And def greetServers = mockBackend.getGreetServer()
    Then assert greetServers.length == 0

  @greetServer3
  Scenario: Server should not reply to a greetServer message with invalid client address (does not fit regex pattern)
    Given def invalidGreetServer =
        """
          {
            "jsonrpc": "2.0",
            "method": "greet_server",
            "params": {
              "public_key": "#(mockBackend.publicKey)",
              "client_address": "invalid://localhost:9000/client",
              "server_address": "#(backendWsURL)"
              }
          }
        """
    When mockBackend.send(invalidGreetServer)
    And karate.log("sending a greetServer :\n", karate.pretty(invalidGreetServer))
    And def greetServers = mockBackend.getGreetServer()
    Then assert greetServers.length == 0

  @greetServer4
  Scenario: Server should not reply to a greetServer message with invalid server address (does not fit regex pattern)
    Given def invalidGreetServer =
        """
          {
            "jsonrpc": "2.0",
            "method": "greet_server",
            "params": {
              "public_key": "#(mockBackend.publicKey)",
              "client_address": "#(frontendWsURL)",
              "server_address": "invalid://localhost:9001/server",
              }
          }
        """
    When mockBackend.send(invalidGreetServer)
    And karate.log("sending a greetServer :\n", karate.pretty(invalidGreetServer))
    And def greetServers = mockBackend.getGreetServer()
    Then assert greetServers.length == 0
