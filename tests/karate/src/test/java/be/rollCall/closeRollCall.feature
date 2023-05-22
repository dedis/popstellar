@env=go,scala
Feature: Close a Roll Call
  Background:
        # This feature will be called to test Roll Call close
        # For every test a file containing the json representation of the message is read
        # and is sent to the backend this is done via :
        # eval frontend.send(<message>) where a mock frontend sends a message to backend
        # Then the response sent by the backend and stored in a buffer :
        # json response = frontend_buffer.takeTimeout(timeout)
        # and checked if it contains the desired fields with :
        # match response contains deep <desired fields>

    # The following calls makes this feature, mockFrontEnd.feature and server.feature share the same scope
    * call read('classpath:be/utils/server.feature')
    * call read('classpath:be/mockFrontEnd.feature')
    * call read('classpath:be/constants.feature')
    * string laoChannel = "/root/p_EYbHyMv6sopI5QhEXBf40MO_eNoq7V_LygBd4c9RA="

  # Testing if after setting up a valid lao, subscribing to it, sending a catchup
  # creating a valid roll call and opening it, we send a valid roll call close
  # message and expect to receive a valid response from the backend
  Scenario: Close a valid roll should succeed
    Given call read('classpath:be/utils/simpleScenarios.feature@name=open_roll_call')
    And def validCloseRollCall =
      """
        {
          "object": "roll_call",
          "action": "close",
          "update_id": '#(getRollCallCloseValidUpdateId)',
          "closes": '#(getRollCallCloseValidId)',
          "closed_at": '#(getRollCallCloseValidCreationTime)',
          "attendees": ['#(getAttendee)']
        }
      """
    When frontend.publish(validCloseRollCall, laoChannel)
    And json answer = frontend.getBackendResponse(validCloseRollCall)
    Then match answer contains VALID_MESSAGE
    And match frontend.receiveNoMoreResponses() == true

  Scenario: Non-organizer closing a roll call should fail
    Given call read('classpath:be/utils/simpleScenarios.feature@name=open_roll_call')
    And def validCloseRollCall =
      """
        {
          "object": "roll_call",
          "action": "close",
          "update_id": '#(getRollCallCloseValidUpdateId)',
          "closes": '#(getRollCallCloseValidId)',
          "closed_at": '#(getRollCallCloseValidCreationTime)',
          "attendees": ['#(getAttendee)']
        }
      """
    * frontend.changeSenderToBeNonAttendee()
    When frontend.publish(validCloseRollCall, laoChannel)
    And json answer = frontend.getBackendResponse(validCloseRollCall)
    Then match answer contains INVALID_MESSAGE_FIELD
    And match frontend.receiveNoMoreResponses() == true

  # After the usual setup open a valid roll call and then send an invalid request for roll call close, here
  # we provide an invalid update_id field in the message. We expect an error message in return
  Scenario: Close a valid roll call with wrong update_id should return an error message
    Given call read('classpath:be/utils/simpleScenarios.feature@name=open_roll_call')
    And def validCloseRollCall =
      """
        {
          "object": "roll_call",
          "action": "close",
          "update_id": '#(getRollCallCloseValidUpdateId)',
          "closes": '#(getRollCallCloseInvalidId)',
          "closed_at": '#(getRollCallCloseValidCreationTime)',
          "attendees": ['#(getAttendee)']
        }
      """
    When frontend.publish(validCloseRollCall, laoChannel)
    And json answer = frontend.getBackendResponse(validCloseRollCall)
    Then match answer contains INVALID_MESSAGE_FIELD
    And match frontend.receiveNoMoreResponses() == true

  # After the usual setup, create a roll cal but never open it. Then trying to send a valid
  # roll call close message should result in an error sent by the backend
  Scenario: Close a valid roll call that was never opened should return an error
    Given call read('classpath:be/utils/simpleScenarios.feature@name=valid_roll_call')
    And def validCloseRollCall =
      """
        {
          "object": "roll_call",
          "action": "close",
          "update_id": '#(getRollCallCloseValidUpdateId)',
          "closes": '#(getRollCallCloseValidId)',
          "closed_at": '#(getRollCallCloseValidCreationTime)',
          "attendees": ['#(getAttendee)']
        }
      """
    When frontend.publish(validCloseRollCall, laoChannel)
    And json answer = frontend.getBackendResponse(validCloseRollCall)
    Then match answer contains INVALID_MESSAGE_FIELD
    And match frontend.receiveNoMoreResponses() == true

