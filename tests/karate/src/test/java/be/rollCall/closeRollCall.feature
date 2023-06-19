@env=go_client,scala_client
Feature: Close a Roll Call

  Background:

    # This feature will be called to test Roll call open
    # Call read(...) makes this feature and the called feature share the same scope
    # Meaning they share def variables, configurations ...
    # Especially JS functions defined in the called features can be directly used here thanks to Karate shared scopes
    * call read('classpath:be/utils/server.feature')
    * call read('classpath:be/mockClient.feature')
    * call read('classpath:be/constants.feature')
    * def organizer = call createMockClient
    * def lao = organizer.createValidLao()
    * def rollCall = organizer.createValidRollCall(lao)

    # This call executes all the steps to open a valid roll call on the server before every scenario
    # (lao creation, subscribe, catchup, roll call creation, roll call open)
    * call read('classpath:be/utils/simpleScenarios.feature@name=open_roll_call') { organizer: '#(organizer)', lao: '#(lao)', rollCall: '#(rollCall)' }
    * def closeRollCall = rollCall.close()

  # Testing if after setting up a valid lao, subscribing to it, sending a catchup
  # creating a valid roll call and opening it, we send a valid roll call close
  # message and expect to receive a valid response from the backend
  Scenario: Close a valid roll should succeed
    Given def validCloseRollCall =
      """
        {
          "object": "roll_call",
          "action": "close",
          "update_id": '#(closeRollCall.updateId)',
          "closes": '#(closeRollCall.closes)',
          "closed_at": '#(closeRollCall.closedAt)',
          "attendees": '#(closeRollCall.attendees)'
        }
      """
    When organizer.publish(validCloseRollCall, lao.channel)
    And json answer = organizer.getBackendResponse(validCloseRollCall)
    Then match answer contains VALID_MESSAGE
    And match organizer.receiveNoMoreResponses() == true

  Scenario: Non-organizer closing a roll call should fail
    Given def notOrganizer = call createMockClient
    And def validCloseRollCall =
      """
        {
          "object": "roll_call",
          "action": "close",
          "update_id": '#(closeRollCall.updateId)',
          "closes": '#(closeRollCall.closes)',
          "closed_at": '#(closeRollCall.closedAt)',
          "attendees": '#(closeRollCall.attendees)'
        }
      """
    When notOrganizer.publish(validCreateRollCall, lao.channel)
    And json answer = notOrganizer.getBackendResponse(validCreateRollCall)
    Then match answer contains INVALID_MESSAGE_FIELD
    And match notOrganizer.receiveNoMoreResponses() == true

  # After the usual setup open a valid roll call and then send an invalid request for roll call close, here
  # we provide an invalid update_id field in the message. We expect an error message in return
  Scenario: Close a valid roll call with wrong update_id should return an error message
    Given def invalidCloseRollCall =
      """
        {
          "object": "roll_call",
          "action": "close",
          "update_id": '#(random.generateCloseRollCallId())',
          "closes": '#(closeRollCall.closes)',
          "closed_at": '#(closeRollCall.closedAt)',
          "attendees": '#(closeRollCall.attendees)'
        }
      """
    When organizer.publish(invalidCloseRollCall, lao.channel)
    And json answer = organizer.getBackendResponse(invalidCloseRollCall)
    Then match answer contains INVALID_MESSAGE_FIELD
    And match organizer.receiveNoMoreResponses() == true

  Scenario: Closing a Roll Call that was not opened on the server returns an error
    Given def newRollCall = organizer.createValidRollCall(lao)
    # This call creates the new roll call on the server without opening it
    And call read('classpath:be/utils/simpleScenarios.feature@name=valid_roll_call') { organizer: '#(organizer)', lao: '#(lao)', rollCall: '#(newRollCall)' }
    And def closeNewRollCall = newRollCall.close()
    And def validCloseRollCall =
      """
        {
          "object": "roll_call",
          "action": "close",
          "update_id": '#(closeNewRollCall.updateId)',
          "closes": '#(closeNewRollCall.closes)',
          "closed_at": '#(closeNewRollCall.closedAt)',
          "attendees": '#(closeNewRollCall.attendees)'
        }
      """
    When organizer.publish(validCloseRollCall, lao.channel)
    And json answer = organizer.getBackendResponse(validCloseRollCall)
    Then match answer contains INVALID_MESSAGE_FIELD
    And match organizer.receiveNoMoreResponses() == true

