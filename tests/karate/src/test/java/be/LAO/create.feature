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
    * def random =  Java.type('be.utils.Random')
    * string channel = "/root"

  Scenario: Create Lao request with empty lao name should fail with an error response 2
    Given def lao = frontend.createValidLao().setName('')
    And def badLaoReq =
      """
        {
          "object": "lao",
          "action": "create",
          "id": "#(lao.id)",
          "name": "#(lao.name)",
          "creation": "#(lao.creation)",
          "organizer": "#(lao.organizerPk)",
          "witnesses": []
        }
      """
    When frontend.publish(badLaoReq, channel)
    And json answer = frontend.getBackendResponse(badLaoReq)
    Then match answer contains INVALID_MESSAGE_FIELD
    And match frontend.receiveNoMoreResponses() == true


  Scenario: Create Lao with negative time should fail with an error response
    Given def lao = frontend.createValidLao().setCreation(-1)
    And def badLaoReq =
      """
        {
          "object": "lao",
          "action": "create",
          "id": "#(lao.creation)",
          "name": "#(lao.name)",
          "creation": "#(lao.creation)",
          "organizer": "#(lao.organizerPk)",
          "witnesses": []
        }
      """
    When frontend.publish(badLaoReq, channel)
    And json answer = frontend.getBackendResponse(badLaoReq)
    Then match answer contains INVALID_MESSAGE_FIELD
    And match frontend.receiveNoMoreResponses() == true

  Scenario: Create Lao with invalid id hash should fail with an error response
    Given def lao = frontend.createValidLao()
    And def badLaoReq =
      """
        {
          "object": "lao",
          "action": "create",
          "id": '#(random.generateLaoId())',
          "name": "#(lao.name)",
          "creation": "#(lao.creation)",
          "organizer": "#(lao.organizerPk)",
          "witnesses": []
        }
      """
    When frontend.publish(badLaoReq, channel)
    And json answer = frontend.getBackendResponse(badLaoReq)
    Then match answer contains INVALID_MESSAGE_FIELD
    And match frontend.receiveNoMoreResponses() == true

  Scenario: Create should succeed with a valid creation request
    Given def lao = frontend.createValidLao()
    And def laoCreateRequest =
      """
        {
          "object": "lao",
          "action": "create",
          "id": '#(lao.id)',
          "name": '#(lao.name)',
          "creation": '#(lao.creation)',
          "organizer": '#(lao.organizerPk)',
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
    Given def lao = frontend.createValidLao()
    And def frontend2 = call createFrontend
    And def laoCreateRequest =
      """
        {
          "object": "lao",
          "action": "create",
          "id": '#(lao.id)',
          "name": '#(lao.name)',
          "creation": '#(lao.creation)',
          "organizer": '#(lao.organizerPk)',
          "witnesses": []
        }
      """

    When frontend2.publish(laoCreateRequest, channel)
    And json answer = frontend2.getBackendResponse(laoCreateRequest)
    Then match answer contains ACCESS_DENIED
    And match frontend2.receiveNoMoreResponses() == true
