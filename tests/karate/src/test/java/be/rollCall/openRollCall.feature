@env=go,scala
Feature: Roll Call Open
  Background:
        # This feature will be called to test Roll call open
        # The following calls makes this feature, mockFrontEnd.feature and server.feature
        # share the same scope
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

  # This scenario test a valid roll call open message. We first use the features
  # that were created previously in scenarios to setup the test for valid roll call open.
  # We create send a valid lao creation message to the backend followed by a subscribe
  # a catchup, and a roll call create then we wait for the response from the backend.
  # Once the response arrives we send an open Roll Call valid message and expect a message
  # containing the same id as the request and a result.
  Scenario: Open a valid Roll Call
    Given call read('classpath:be/utils/simpleScenarios.feature@name=valid_roll_call')
    And def validOpenRollCall =
      """
        {
          "object": "roll_call",
          "action": "open",
          "update_id": '#(getRollCallOpenValidUpdateId)',
          "opens": '#(getRollCallOpenValidId)',
          "opened_at": '#(getRollCallOpenValidCreationTime)'
        }
      """
    When frontend.publish(validOpenRollCall, laoChannel)
    And json answer = frontend.getBackendResponse(validOpenRollCall)
    Then match answer contains VALID_MESSAGE
    And match frontend.receiveNoMoreResponses() == true

  Scenario: Opening a roll call with non-organizer as sender should fail
    Given call read('classpath:be/utils/simpleScenarios.feature@name=valid_roll_call')
    And def validOpenRollCall =
      """
        {
          "object": "roll_call",
          "action": "open",
          "update_id": '#(getRollCallOpenValidUpdateId)',
          "opens": '#(getRollCallOpenValidId)',
          "opened_at": '#(getRollCallOpenValidCreationTime)'
        }
      """
    * frontend.changeSenderToBeNonAttendee()
    When frontend.publish(validOpenRollCall, laoChannel)
    And json answer = frontend.getBackendResponse(validOpenRollCall)
    Then match answer contains INVALID_MESSAGE_FIELD
    And match frontend.receiveNoMoreResponses() == true

  # First creates a valid lao followed by subscribe and catchup but we don't send a roll call
  # create message but send a valid roll call open message. Since the roll call create message
  # was never sent opening a roll call is illegal and we expect an error message from the backend.
  Scenario: Opening a Roll Call that does not exist should return an error
    Given call read('classpath:be/utils/simpleScenarios.feature@name=valid_lao')
    And def validOpenRollCall =
      """
        {
          "object": "roll_call",
          "action": "open",
          "update_id": '#(getRollCallOpenValidUpdateId)',
          "opens": '#(getRollCallOpenValidId)',
          "opened_at": '#(getRollCallOpenValidCreationTime)'
        }
      """
    When frontend.publish(validOpenRollCall, laoChannel)
    And json answer = frontend.getBackendResponse(validOpenRollCall)
    Then match answer contains INVALID_MESSAGE_FIELD
    And match frontend.receiveNoMoreResponses() == true

  # Create a valid roll call and then send an invalid request for roll call open, her
  # we provide an invalid update_id field in the message. We expect an error message in return
  Scenario: Opening a Roll Call with invalid update_id should return an error
    Given call read('classpath:be/utils/simpleScenarios.feature@name=valid_roll_call')
    And def validOpenRollCall =
      """
        {
          "object": "roll_call",
          "action": "open",
          "update_id": '#(getRollCallOpenInvalidUpdateId)',
          "opens": '#(getRollCallOpenValidId)',
          "opened_at": '#(getRollCallOpenValidCreationTime)'
        }
      """
    When frontend.publish(validOpenRollCall, laoChannel)
    And json answer = frontend.getBackendResponse(validOpenRollCall)
    Then match answer contains INVALID_MESSAGE_FIELD
    And match frontend.receiveNoMoreResponses() == true

  # Testing idempotency (Not guaranteed by the backend for now, so the test fails)
  Scenario: Opening a Roll Call for which with already received an error should return an error again
    Given call read('classpath:be/utils/simpleScenarios.feature@name=valid_roll_call')
    And def validOpenRollCall =
      """
        {
          "object": "roll_call",
          "action": "open",
          "update_id": '#(getRollCallOpenInvalidUpdateId)',
          "opens": '#(getRollCallOpenValidId)',
          "opened_at": '#(getRollCallOpenValidCreationTime)'
        }
      """
    When frontend.publish(validOpenRollCall, laoChannel)
    And json answer = frontend.getBackendResponse(validOpenRollCall)
    Then match answer contains INVALID_MESSAGE_FIELD
    When frontend.publish(validOpenRollCall, laoChannel)
    And json answer = frontend.getBackendResponse(validOpenRollCall)
    And match answer contains INVALID_MESSAGE_FIELD
    And match frontend.receiveNoMoreResponses() == true

