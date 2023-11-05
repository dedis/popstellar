@env=go_server,scala_server
Feature: Send heartbeats to other servers

  Background:
    # This feature will be called to test how the server sends heartbeat messages.
    # Call read(...) makes this feature and the called feature share the same scope
    # Meaning they share def variables, configurations ...
    # Especially JS functions defined in the called features can be directly used here thanks to Karate shared scopes
    * call read('classpath:be/features/utils/constants.feature')
    * call read(serverFeature)
    * call read(mockClientFeature)
    * def mockServer = call createMockClient
    * def mockFrontend = call createMockClient
    * def lao = mockFrontend.createValidLao()
    * def validRollCall = mockFrontend.createValidRollCall(lao)

    # This call executes all the steps to create a valid lao on the server before every scenario
    # (lao creation, subscribe, catchup)
    * call read(createLaoScenario) { organizer: '#(mockFrontend)', lao: '#(lao)' }
    * call read(greetServerScenario) { mockServer: '#(mockServer)' }

  # After lao creation, wait and do nothing (40 seconds for now) and check that more than one heartbeat message was received.
  # (The initial one would be a response to publishing lao creation)
  Scenario: Server should send heartbeat messages automatically after a time interval
    Given wait(40)

    When def heartbeatMessages = mockServer.getMessagesByMethod('heartbeat')

    Then assert heartbeatMessages.length > 1

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

    When mockFrontend.publish(validCreateRollCall, lao.channel)
    Then assert mockServer.receivedHeartbeatContainingMessageId(validCreateRollCall)
