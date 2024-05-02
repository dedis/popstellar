@env=go,scala
Feature: Create a Roll Call

  Background:

    # This feature will be called to test LAO creation
    * call read('classpath:be/features/utils/constants.feature')
    * call read(serverFeature)
    * call read(mockClientFeature)
    * def organizer = call createMockFrontend
    * def lao = organizer.generateValidLao()
    * def validRollCall = organizer.generateValidRollCall(lao)

    # This call executes all the steps to create a valid lao on the server before every scenario
    # (lao creation, subscribe, catchup)
    * call read(createLaoScenario) { organizer: '#(organizer)', lao: '#(lao)' }

  # Testing if after setting up a valid lao, subscribing to it and sending a catchup
  # we send a valid roll call create request and expect to receive a valid response
  # from the backend
  @createRollCall1
  Scenario: Valid Roll Call
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
    When organizer.publish(validCreateRollCall, lao.channel)
    And json answer = organizer.getBackendResponse(validCreateRollCall)
    Then match answer contains VALID_MESSAGE
    And match organizer.receiveNoMoreResponses() == true

  # Setting up the lao correctly but send an invalid roll call create request, containing
  # an empty roll call name should result in an error message from the backend.
  @createRollCall2
  Scenario: Roll Call Creation with empty name should return an error code
    Given def rollCall = validRollCall.setName('')
    And def badCreateRollCall =
       """
        {
          "object": "roll_call",
          "action": "create",
          "id": '#(rollCall.id)',
          "name": '#(rollCall.name)',
          "creation": '#(rollCall.creation)',
          "proposed_start": '#(rollCall.start)',
          "proposed_end": '#(rollCall.end)',
          "location": '#(rollCall.location)',
          "description": '#(rollCall.description)',
        }
      """
    When organizer.publish(badCreateRollCall, lao.channel)
    And json answer = organizer.getBackendResponse(badCreateRollCall)
    Then match answer contains INVALID_MESSAGE_FIELD
    And match organizer.receiveNoMoreResponses() == true

  # Setting up the lao correctly and sending a roll call create message that comes from
  # a non-organizer should result in an error message being sent by the backend.
  @createRollCall3
  Scenario: Roll Call Creation with non-organizer as sender should return an error
    Given def notOrganizer = call createMockClient
    And def validCreateRollCall =
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

    When notOrganizer.publish(validCreateRollCall, lao.channel)
    And json answer = notOrganizer.getBackendResponse(validCreateRollCall)
    Then match answer contains INVALID_MESSAGE_FIELD
    And match notOrganizer.receiveNoMoreResponses() == true

  # Setting up a lao correctly but sending a valid roll call create message on the
  # root channel should result in backend rejecting the message and sending an error message
  @createRollCall4
  Scenario: Roll Call Creation sent on root channel should return an error
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
    When organizer.publish(validCreateRollCall, rootChannel)
    And json answer = organizer.getBackendResponse(validCreateRollCall)
    Then match answer contains INVALID_MESSAGE_FIELD
    And match organizer.receiveNoMoreResponses() == true


  # Setting up the lao correctly but send an invalid roll call create request, containing
  # a proposed start time larger than proposed end time should result in an error message
  # from the backend.
  @createRollCall5
  Scenario: Roll Call Creation with proposed start > proposed end should return and error
    Given def rollCall = validRollCall.switchStartAndEnd()
    And def badCreateRollCall =
       """
        {
          "object": "roll_call",
          "action": "create",
          "id": '#(rollCall.id)',
          "name": '#(rollCall.name)',
          "creation": '#(rollCall.creation)',
          "proposed_start": '#(rollCall.start)',
          "proposed_end": '#(rollCall.end)',
          "location": '#(rollCall.location)',
          "description": '#(rollCall.description)',
        }
      """
    When organizer.publish(badCreateRollCall, lao.channel)
    And json answer = organizer.getBackendResponse(badCreateRollCall)
    Then match answer contains INVALID_MESSAGE_FIELD
    And match organizer.receiveNoMoreResponses() == true

  # Setting up the lao correctly but send an invalid roll call create request, containing
  # a negative creation time should result in an error message from the backend.
  @createRollCall6
  Scenario: Roll Call Creation with negative creation time should return an error
    Given def rollCall = validRollCall.setCreation(-1)
    And def badCreateRollCall =
       """
        {
          "object": "roll_call",
          "action": "create",
          "id": '#(rollCall.id)',
          "name": '#(rollCall.name)',
          "creation": '#(rollCall.creation)',
          "proposed_start": '#(rollCall.start)',
          "proposed_end": '#(rollCall.end)',
          "location": '#(rollCall.location)',
          "description": '#(rollCall.description)',
        }
      """
    When organizer.publish(badCreateRollCall, lao.channel)
    And json answer = organizer.getBackendResponse(badCreateRollCall)
    Then match answer contains INVALID_MESSAGE_FIELD
    And match organizer.receiveNoMoreResponses() == true

  # Setting up the lao correctly but send an invalid roll call create request, containing
  # a creation time larger than proposed start time should result in an error message
  # from the backend.
  @createRollCall7
  Scenario: Roll Call Creation with creation time > proposed start should return and error
    Given def rollCall = validRollCall.switchCreationAndStart()
    And def badCreateRollCall =
       """
        {
          "object": "roll_call",
          "action": "create",
          "id": '#(rollCall.id)',
          "name": '#(rollCall.name)',
          "creation": '#(rollCall.creation)',
          "proposed_start": '#(rollCall.start)',
          "proposed_end": '#(rollCall.end)',
          "location": '#(rollCall.location)',
          "description": '#(rollCall.description)',
        }
      """
    When organizer.publish(badCreateRollCall, lao.channel)
    And json answer = organizer.getBackendResponse(badCreateRollCall)
    Then match answer contains INVALID_MESSAGE_FIELD
    And match organizer.receiveNoMoreResponses() == true

  # Setting up the lao correctly but send an invalid roll call create request, containing
  # an invalid roll_call id should result in an error message from the backend.
  @createRollCall8
  Scenario: Roll Call Creation with invalid roll_call id should return an error
    Given def badCreateRollCall =
      """
        {
          "object": "roll_call",
          "action": "create",
          "id": '#(random.generateCreateRollCallId())',
          "name": '#(validRollCall.name)',
          "creation": '#(validRollCall.creation)',
          "proposed_start": '#(validRollCall.start)',
          "proposed_end": '#(validRollCall.end)',
          "location": '#(validRollCall.location)',
          "description": '#(validRollCall.description)',
        }
      """
    When organizer.publish(badCreateRollCall, lao.channel)
    And json answer = organizer.getBackendResponse(badCreateRollCall)
    Then match answer contains INVALID_MESSAGE_FIELD
    And match organizer.receiveNoMoreResponses() == true

  # Sending a valid roll call create request for a lao that does not exist should result
  # in an error message from the backend.
  @createRollCall9
  Scenario: Roll Call Creation for non existent lao should return an error
    Given def randomLao = organizer.generateValidLao()
    And def randomRollCall = organizer.generateValidRollCall(randomLao)
    Given def validCreateRollCall =
      """
        {
          "object": "roll_call",
          "action": "create",
          "id": '#(randomRollCall.id)',
          "name": '#(randomRollCall.name)',
          "creation": '#(randomRollCall.creation)',
          "proposed_start": '#(randomRollCall.start)',
          "proposed_end": '#(randomRollCall.end)',
          "location": '#(randomRollCall.location)',
          "description": '#(randomRollCall.description)',
        }
      """
    When organizer.publish(validCreateRollCall, randomLao.channel)
    And json answer = organizer.getBackendResponse(validCreateRollCall)
    Then match answer contains INVALID_MESSAGE_FIELD
    And match organizer.receiveNoMoreResponses() == true
