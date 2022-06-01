@env=go,scala
Feature: Create a Roll Call
  Background:
      # This is feature will be called  to test a Roll Call Creation
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
  # Testing if after setting up a valid lao, subscribing to it and sending a catchup
  # we send a valid roll call create request and expect to receive a valid response
  # from the backend
  Scenario: Valid Roll Call
    Given call read('classpath:be/utils/simpleScenarios.feature@name=valid_lao')
    And def validCreateRollCall =
      """
        {
          "object": "roll_call",
          "action": "create",
          "id": '#(getRollCallValidId)',
          "name": "Roll Call ",
          "creation": 1633098853,
          "proposed_start": 1633099125,
          "proposed_end": 1633099140,
          "location": "EPFL",
          "description": "Food is welcome!"
        }
      """
    When frontend.publish(JSON.stringify(validCreateRollCall), laoChannel)
    And json answer = frontend.getBackendResponse(JSON.stringify(validCreateRollCall))
    Then match answer contains VALID_MESSAGE
    And match frontend.receiveNoMoreResponses() == true

  # Setting up the lao correctly but send an invalid roll call create request, containing
  # an empty roll call name should result in an error message from the backend.
  Scenario: Roll Call Creation with empty name should return an error code
    Given call read('classpath:be/utils/simpleScenarios.feature@name=valid_lao')
    And def validCreateRollCall =
      """
        {
          "object": "roll_call",
          "action": "create",
          "id": '#(getRollCallValidId)',
          "name": "",
          "creation": 1633098853,
          "proposed_start": 1633099125,
          "proposed_end": 1633099140,
          "location": "EPFL",
          "description": "Food is welcome!"
        }
      """
    When frontend.publish(JSON.stringify(validCreateRollCall), laoChannel)
    And json answer = frontend.getBackendResponse(JSON.stringify(validCreateRollCall))
    Then match answer contains INVALID_MESSAGE_FIELD
    And match frontend.receiveNoMoreResponses() == true

  # Setting up the lao correctly and sending a roll call create message that comes from
  # a non-organizer should result in an error message being sent by the backend.
  Scenario: Roll Call Creation with non-organizer as sender should return an error
    Given call read('classpath:be/utils/simpleScenarios.feature@name=valid_lao')
    And def validCreateRollCall =
      """
        {
          "object": "roll_call",
          "action": "create",
          "id": '#(getRollCallValidId)',
          "name": "Roll Call ",
          "creation": 1633098853,
          "proposed_start": 1633099125,
          "proposed_end": 1633099140,
          "location": "EPFL",
          "description": "Food is welcome!"
        }
      """
    * frontend.changeSenderToBeNonAttendee()
    When frontend.publish(JSON.stringify(validCreateRollCall), laoChannel)
    And json answer = frontend.getBackendResponse(JSON.stringify(validCreateRollCall))
    Then match answer contains INVALID_MESSAGE_FIELD
    And match frontend.receiveNoMoreResponses() == true

  # Setting up a lao correctly but sending a valid roll call create message on the
  # root channel should result in backend rejecting the message and sending an error message
  Scenario: Roll Call Creation sent on root channel should return an error
    Given call read('classpath:be/utils/simpleScenarios.feature@name=valid_lao')
    And def validCreateRollCall =
      """
        {
          "object": "roll_call",
          "action": "create",
          "id": '#(getRollCallValidId)',
          "name": "Roll Call ",
          "creation": 1633098853,
          "proposed_start": 1633099125,
          "proposed_end": 1633099140,
          "location": "EPFL",
          "description": "Food is welcome!"
        }
      """
    * string rootChannel = "/root"
    When frontend.publish(JSON.stringify(validCreateRollCall), rootChannel)
    And json answer = frontend.getBackendResponse(JSON.stringify(validCreateRollCall))
    Then match answer contains INVALID_MESSAGE_FIELD
    And match frontend.receiveNoMoreResponses() == true


  # Setting up the lao correctly but send an invalid roll call create request, containing
  # a proposed start time larger than proposed end time should result in an error message
  # from the backend.
  Scenario: Roll Call Creation with proposed start > proposed end should return and error
    Given call read('classpath:be/utils/simpleScenarios.feature@name=valid_lao')
    And def validCreateRollCall =
      """
        {
          "object": "roll_call",
          "action": "create",
          "id": '#(getRollCallValidId(rollCallName, creationTime))',
          "name": "Roll Call ",
          "creation": 1633098853,
          "proposed_start": 1633099155,
          "proposed_end": 1633099140,
          "location": "EPFL",
          "description": "Food is welcome!"
        }
      """
    When frontend.publish(JSON.stringify(validCreateRollCall), laoChannel)
    And json answer = frontend.getBackendResponse(JSON.stringify(validCreateRollCall))
    Then match answer contains INVALID_MESSAGE_FIELD
    And match frontend.receiveNoMoreResponses() == true

  # Setting up the lao correctly but send an invalid roll call create request, containing
  # a negative creation time should result in an error message from the backend.
  Scenario: Roll Call Creation with creation time is negative should return an error
    Given call read('classpath:be/utils/simpleScenarios.feature@name=valid_lao')
    And def validCreateRollCall =
      """
        {
          "object": "roll_call",
          "action": "create",
          "id": '#(getRollCallValidId(rollCallName, creationTimeNegative))',
          "name": "Roll Call ",
          "creation": -153,
          "proposed_start": 1633099125,
          "proposed_end": 1633099140,
          "location": "EPFL",
          "description": "Food is welcome!"
        }
      """
    When frontend.publish(JSON.stringify(validCreateRollCall), laoChannel)
    And json answer = frontend.getBackendResponse(JSON.stringify(validCreateRollCall))
    Then match answer contains INVALID_MESSAGE_FIELD
    And match frontend.receiveNoMoreResponses() == true

  # Setting up the lao correctly but send an invalid roll call create request, containing
  # a creation time larger than proposed start time should result in an error message
  # from the backend.
  Scenario: Roll Call Creation with creation time < proposed start should return and error
    Given call read('classpath:be/utils/simpleScenarios.feature@name=valid_lao')
    * def smallCreationTime = 1633099055
    And def validCreateRollCall =
      """
        {
          "object": "roll_call",
          "action": "create",
          "id": '#(getRollCallValidId(rollCallName, smallCreationTime))',
          "name": "Roll Call ",
          "creation": 1633099055,
          "proposed_start": 1633099155,
          "proposed_end": 1633099140,
          "location": "EPFL",
          "description": "Food is welcome!"
        }
      """
    When frontend.publish(JSON.stringify(validCreateRollCall), laoChannel)
    And json answer = frontend.getBackendResponse(JSON.stringify(validCreateRollCall))
    Then match answer contains INVALID_MESSAGE_FIELD
    And match frontend.receiveNoMoreResponses() == true
  # Setting up the lao correctly but send an invalid roll call create request, containing
  # an invalid roll_call id should result in an error message from the backend.
  Scenario: Roll Call Creation with invalid roll_call id should return an error
    Given call read('classpath:be/utils/simpleScenarios.feature@name=valid_lao')
    And def validCreateRollCall =
      """
        {
          "object": "roll_call",
          "action": "create",
          "id": '#(getRollCallInvalidId)',
          "name": "Roll Call ",
          "creation": 1633098853,
          "proposed_start": 1633099125,
          "proposed_end": 1633099140,
          "location": "EPFL",
          "description": "Food is welcome!"
        }
      """
    When frontend.publish(JSON.stringify(validCreateRollCall), laoChannel)
    And json answer = frontend.getBackendResponse(JSON.stringify(validCreateRollCall))
    Then match answer contains INVALID_MESSAGE_FIELD
    And match frontend.receiveNoMoreResponses() == true
