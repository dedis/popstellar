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
    * def frontend = call createFrontend
    * string channel = "/root"

  Scenario: Create Lao request with empty lao name should fail with an error response 2
    * def lao = frontend.createLaoWithName("")
    Given def badLaoReq =
      """
        {
          "object": "lao",
          "action": "create",
          "id": "#(lao.getLaoId())",
          "name": "",
          "creation": "#(lao.getCreation())",
          "organizer": "#(lao.getOrganizerPublicKey())",
          "witnesses": []
        }
      """
    When frontend.publish(badLaoReq, channel)
    And json answer = frontend.getBackendResponse(badLaoReq)
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
    When frontend.publish(badLaoReq, channel)
    And json answer = frontend.getBackendResponse(badLaoReq)
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
    When frontend.publish(badLaoReq, channel)
    And json answer = frontend.getBackendResponse(badLaoReq)
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
    When frontend.publish(laoCreateRequest, channel)
    And json answer = frontend.getBackendResponse(laoCreateRequest)
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
    When frontend.publish(laoCreateRequest, channel)
    And json answer = frontend.getBackendResponse(laoCreateRequest)
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
    When frontend.publish(laoCreateRequest, channel)
    And json answer = frontend.getBackendResponse(laoCreateRequest)
    Then match answer contains ACCESS_DENIED
    And match frontend.receiveNoMoreResponses() == true
