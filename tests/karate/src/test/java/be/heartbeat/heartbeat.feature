@env=go,scala
Feature: Send heartbeats to other servers

  Background:
    # This feature will be called to test sending heartbeats to other servers.
    # This call makes this feature and server.feature share the same scope
    # Meaning they share def variables, configurations ...
    # Especially JS functions defined in server.feature can be directly used here thanks to Karate shared scopes
    # This also sets up a valid lao by sending a create lao message, subscribing to the lao and sending a catchup message
    * call read('classpath:be/utils/server.feature')
    * call read('classpath:be/mockFrontEnd.feature')
    * call read('classpath:be/constants.feature')
    * call read('classpath:be/utils/simpleScenarios.feature@name=valid_lao')
    * string channel = "/root/p_EYbHyMv6sopI5QhEXBf40MO_eNoq7V_LygBd4c9RA="

    # After lao creation, wait (6 seconds for now) and check that a heartbeat message was received
  Scenario: Server should send heartbeat messages
    Given wait(6)
    When def heartbeatMessages = frontend.getMessagesByMethod('heartbeat')
    Then assert heartbeatMessages.length > 0

    # Check that after sending a heartbeat message with unknown messages, the server responds with a getMessagesByID request
  Scenario: Server should request the missing message id's in a heartbeat
    Given def heartbeat =
            """
          JSON.stringify(
              {
              "method": "heartbeat",
              "params": {
                  "/root/p_EYbHyMv6sopI5QhEXBf40MO_eNoq7V_LygBd4c9RA=": [
                     "DCBX48EuNO6q-Sr42ONqsj7opKiNeXyRzrjqTbZ_aMI="
                  ],
              },
              "jsonrpc": "2.0"
          })
        """
    When frontend.send(heartbeat)
    And def getMessagesByIdMessages = frontend.getMessagesByMethod('get_messages_by_id')
    Then assert getMessagesByIdMessages.length == 1
