@env=go,scala
Feature: Send heartbeats to other servers

  Background:
    # This feature will be called to test sending heartbeats to other servers.
    # Call read(...) makes this feature and the called feature share the same scope
    # Meaning they share def variables, configurations ...
    # Especially JS functions defined in the called features can be directly used here thanks to Karate shared scopes
    * call read('classpath:be/utils/server.feature')
    * call read('classpath:be/mockClient.feature')
    * call read('classpath:be/constants.feature')
    * def server = call createMockClient
    * def lao = server.createValidLao()
    * def validRollCall = server.createValidRollCall(lao)

    # This call executes all the steps to create a valid lao on the server before every scenario
    # (lao creation, subscribe, catchup)
    * call read('classpath:be/utils/simpleScenarios.feature@name=valid_lao') { organizer: '#(server)', lao: '#(lao)' }

  # After lao creation, wait and do nothing (30 seconds for now) and check that a heartbeat message was received
  Scenario: Server should send heartbeat messages automatically after a time interval
    And wait(30)
    When def heartbeatMessages = server.getMessagesByMethod('heartbeat')
    Then assert heartbeatMessages.length > 0

  # Check that after receiving a publish message (in this case a create roll call), the server sends a heartbeat
  Scenario: Server should send heartbeat messages after receiving a publish
    Given def validCreateRollCall =
      """
        {
          "object": "roll_call",
          "action": "create",
          "id": '#(validRollCall.id)',
          "name": '#(validRollCall.name)',
          "creation": '#(validRollCall.creation)',
          "proposed_start": '#(validRollCall.start)',
          "proposed_end": '#(validRollCall.end)',
          "location": '#(validRollCall.location)',
          "description": '#(validRollCall.description)',
        }
      """
    When server.publish(validCreateRollCall, lao.channel)
    And def heartbeatMessages = server.getMessagesByMethod('heartbeat')
    Then assert heartbeatMessages.length == 1
