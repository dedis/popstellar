@env=go,scala
Feature: Roll Call Open

  Background:

    # This feature will be called to test Roll call open
    * call read('classpath:be/features/utils/constants.feature')
    * call read(serverFeature)
    * call read(mockClientFeature)
    * def organizer = call createMockFrontend
    * def lao = organizer.generateValidLao()
    * def rollCall = organizer.generateValidRollCall(lao)

    # This call executes all the steps to create a valid roll call on the server before every scenario
    # (lao creation, subscribe, catchup, roll call creation)
    * call read(createRollCallScenario) { organizer: '#(organizer)', lao: '#(lao)', rollCall: '#(rollCall)' }
    * def openRollCall = rollCall.open()

  # This scenario tests a valid roll call open message.
  # The roll call is already created and we now send a valid open Roll Call message. We expect a message
  # containing the same id as the request and a result back from the server.
  @openRollCall1
  Scenario: Open a valid Roll Call
    Given def validOpenRollCall =
      """
        {
          "object": "roll_call",
          "action": "open",
          "update_id": '#(openRollCall.updateId)',
          "opens": '#(openRollCall.opens)',
          "opened_at": '#(openRollCall.openedAt)'
        }
      """
    When organizer.publish(validOpenRollCall, lao.channel)
    And json answer = organizer.getBackendResponse(validOpenRollCall)
    Then match answer contains VALID_MESSAGE
    And match organizer.receiveNoMoreResponses() == true

  @openRollCall2
  Scenario: Opening a Roll Call with non-organizer as sender should fail
    Given def notOrganizer = call createMockFrontend
    And def validOpenRollCall =
      """
        {
          "object": "roll_call",
          "action": "open",
          "update_id": '#(openRollCall.updateId)',
          "opens": '#(openRollCall.opens)',
          "opened_at": '#(openRollCall.openedAt)'
        }
      """
    When notOrganizer.publish(validCreateRollCall, lao.channel)
    And json answer = notOrganizer.getBackendResponse(validCreateRollCall)
    Then match answer contains INVALID_MESSAGE_FIELD
    And match notOrganizer.receiveNoMoreResponses() == true

  @openRollCall3
  Scenario: Opening a Roll Call that was not created on the server returns an error
    Given def newRollCall = organizer.generateValidRollCall(lao)
    And def openNewRollCall = newRollCall.open()
    And def validOpenRollCall =
      """
        {
          "object": "roll_call",
          "action": "open",
          "update_id": '#(openNewRollCall.updateId)',
          "opens": '#(openNewRollCall.opens)',
          "opened_at": '#(openNewRollCall.openedAt)'
        }
      """
    When organizer.publish(validOpenRollCall, lao.channel)
    And json answer = organizer.getBackendResponse(validOpenRollCall)
    Then match answer contains INVALID_MESSAGE_FIELD
    And match organizer.receiveNoMoreResponses() == true

  @openRollCall4
  Scenario: Opening a Roll Call with invalid update_id should return an error
    Given def invalidOpenRollCall =
      """
        {
          "object": "roll_call",
          "action": "open",
          "update_id": '#(random.generateOpenRollCallId())',
          "opens": '#(openRollCall.opens)',
          "opened_at": '#(openRollCall.openedAt)'
        }
      """
    When organizer.publish(invalidOpenRollCall, lao.channel)
    And json answer = organizer.getBackendResponse(invalidOpenRollCall)
    Then match answer contains INVALID_MESSAGE_FIELD
    And match organizer.receiveNoMoreResponses() == true

  # Testing idempotency (Not guaranteed by the backend for now, so the test fails)
  @openRollCall5
  Scenario: Opening a Roll Call for which with already received an error should return an error again
    And def invalidOpenRollCall =
      """
        {
          "object": "roll_call",
          "action": "open",
          "update_id": '#(random.generateOpenRollCallId())',
          "opens": '#(openRollCall.opens)',
          "opened_at": '#(openRollCall.openedAt)'
        }
      """
    When organizer.publish(invalidOpenRollCall, lao.channel)
    And json answer = organizer.getBackendResponse(invalidOpenRollCall)
    Then match answer contains INVALID_MESSAGE_FIELD
    When organizer.publish(invalidOpenRollCall, lao.channel)
    And json answer = organizer.getBackendResponse(invalidOpenRollCall)
    And match answer contains INVALID_MESSAGE_FIELD
    And match organizer.receiveNoMoreResponses() == true

