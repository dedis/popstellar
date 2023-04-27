@env=go,scala
Feature: Create a pop LAO

  Background:
        # This feature will be called to test LAO creation
        # This call makes this feature and server.feature share the same scope
        # Meaning they share def variables, configurations ...
        # Especially JS functions defined in server.feature can be directly used here thanks to Karate shared scopes
    * call read('classpath:be/utils/server.feature')
    * call read('classpath:be/mockFrontEnd.feature')
    * call read('classpath:be/constants.feature')
    * string channel = "/root"

    * def organizer = createUser()
    * def attendee = createUser()
    * string laoId = generateLaoId(organizer)
    * string laoChannel = "/root/" + laoId

  Scenario: Create Lao request with empty lao name should fail with an error response 2
    Given def badLaoReq =
      """
        {
          "object": "lao",
          "action": "create",
          "id": '#(getLaoIdEmptyName)',
          "name": "",
          "creation": 1633098234,
          "organizer": '#(getOrganizer)',
          "witnesses": []
        }
      """
    When frontend.publish(JSON.stringify(badLaoReq), channel)
    And json answer = frontend.getBackendResponse(JSON.stringify(badLaoReq))
    Then match answer contains INVALID_MESSAGE_FIELD
    And match frontend.receiveNoMoreResponses() == true


  Scenario: Create Lao with negative time should fail with an error response
    Given def badLaoReq =
      """
        {
          "object": "lao",
          "action": "create",
          "id": '#(getLaoIdNegativeTime)',
          "name": "LAO",
          "creation": -1633098234,
          "organizer": '#(getOrganizer)',
          "witnesses": []
        }
      """
    When frontend.publish(JSON.stringify(badLaoReq), channel)
    And json answer = frontend.getBackendResponse(JSON.stringify(badLaoReq))
    Then match answer contains INVALID_MESSAGE_FIELD
    And match frontend.receiveNoMoreResponses() == true
  Scenario: Create Lao with invalid id hash should fail with an error response
    Given def badLaoReq =
      """
        {
          "object": "lao",
          "action": "create",
          "id": '#(getOrganizer)',
          "name": "LAO",
          "creation": 1633098234,
          "organizer": '#(getOrganizer)',
          "witnesses": []
        }
      """
    When frontend.publish(JSON.stringify(badLaoReq), channel)
    And json answer = frontend.getBackendResponse(JSON.stringify(badLaoReq))
    Then match answer contains INVALID_MESSAGE_FIELD
    And match frontend.receiveNoMoreResponses() == true
  Scenario: Create should succeed with a valid creation request
    Given def laoCreateRequest =
      """
        {
          "object": "lao",
          "action": "create",
          "id": '#(getLaoValid)',
          "name": "LAO",
          "creation": '#(getLaoValidCreationTime)',
          "organizer": '#(getOrganizer)',
          "witnesses": []
        }
      """
    When frontend.publish(JSON.stringify(laoCreateRequest), channel)
    And json answer = frontend.getBackendResponse(JSON.stringify(laoCreateRequest))
    Then match answer contains VALID_MESSAGE
    And match frontend.receiveNoMoreResponses() == true

  Scenario: Create should fail if signature is invalid
    Given def laoCreateRequest =
      """
        {
          "object": "lao",
          "action": "create",
          "id": '#(getLaoValid)',
          "name": "LAO",
          "creation": 1633035721,
          "organizer": '#(getOrganizer)',
          "witnesses": []
        }
      """
    * frontend.setWrongSignature()
    When frontend.publish(JSON.stringify(laoCreateRequest), channel)
    And json answer = frontend.getBackendResponse(JSON.stringify(laoCreateRequest))
    Then match answer contains INVALID_MESSAGE_FIELD
    And match frontend.receiveNoMoreResponses() == true

  Scenario: Create Lao with different public key from the organizer should fail with error response
    Given def laoCreateRequest =
      """
        {
          "object": "lao",
          "action": "create",
          "id": '#(getLaoValid)',
          "name": "LAO",
          "creation": 1633035721,
          "organizer": '#(getOrganizer)',
          "witnesses": []
        }
      """

    * frontend.changeSenderToBeNonAttendee()
    When frontend.publish(JSON.stringify(laoCreateRequest), channel)
    And json answer = frontend.getBackendResponse(JSON.stringify(laoCreateRequest))
    Then match answer contains ACCESS_DENIED
    And match frontend.receiveNoMoreResponses() == true


  Scenario: Create Lao with different public key from the organizer should fail with error response
    Given def laoCreateRequest =
      """
        {
          "object": "lao",
          "action": "create",
          "id": '#(laoId)',
          "name": "LAO",
          "creation": // needs to be the one laoId was created with,
          "organizer": '#(organizer.getPublicKey)',
          "witnesses": []
        }
      """
    When attendee.publish(laoCreateRequest, channel)
    And json answer = attendee.getBackendResponse(laoCreateRequest)
    Then match answer contains ACCESS_DENIED
    And match attendee.receiveNoMoreResponses() == true
