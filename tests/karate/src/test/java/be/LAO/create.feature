@env=go,scala
Feature: Create a pop LAO

  Background:
        # This feature will be called to test LAO creation
        # Call read(...) makes this feature and the called feature share the same scope
        # Meaning they share def variables, configurations ...
        # Especially JS functions defined can be directly used here thanks to Karate shared scopes
    * call read('classpath:be/utils/server.feature')
    * call read('classpath:be/mockFrontEnd.feature')
    * call read('classpath:be/constants.feature')
    * def frontend = call createFrontend
    * def random =  Java.type('be.utils.Random')
    * def organizer = call createFrontend

  Scenario: Create Lao request with empty lao name should fail with an error response
    Given def lao = organizer.createValidLao().setName('')
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
    When organizer.publish(badLaoReq, rootChannel)
    And json answer = organizer.getBackendResponse(badLaoReq)
    Then match answer contains INVALID_MESSAGE_FIELD
    And match organizer.receiveNoMoreResponses() == true


  Scenario: Create Lao request with negative creation time should fail with an error response
    Given def lao = organizer.createValidLao().setCreation(-1)
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
    When organizer.publish(badLaoReq, rootChannel)
    And json answer = organizer.getBackendResponse(badLaoReq)
    Then match answer contains INVALID_MESSAGE_FIELD
    And match organizer.receiveNoMoreResponses() == true

  Scenario: Create Lao request with invalid id hash should fail with an error response
    Given def lao = organizer.createValidLao()
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
    When organizer.publish(badLaoReq, rootChannel)
    And json answer = organizer.getBackendResponse(badLaoReq)
    Then match answer contains INVALID_MESSAGE_FIELD
    And match organizer.receiveNoMoreResponses() == true

  Scenario: Valid Create Lao request should succeed
    Given def lao = organizer.createValidLao()
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
    When organizer.publish(laoCreateRequest, rootChannel)
    And json answer = organizer.getBackendResponse(laoCreateRequest)
    Then match answer contains VALID_MESSAGE
    And match organizer.receiveNoMoreResponses() == true

  Scenario: Create Lao request with invalid signature should fail
    Given def lao = organizer.createValidLao()
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
    When organizer.useWrongSignature()
    And organizer.publish(laoCreateRequest, rootChannel)
    And json answer = organizer.getBackendResponse(laoCreateRequest)
    Then match answer contains INVALID_MESSAGE_FIELD
    And match organizer.receiveNoMoreResponses() == true

  Scenario: Create Lao request with public key different from the sender public key should fail
    Given def lao = organizer.createValidLao()
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
    When def notOrganizer = call createFrontend
    And notOrganizer.publish(laoCreateRequest, rootChannel)
    And json answer = notOrganizer.getBackendResponse(laoCreateRequest)
    Then match answer contains ACCESS_DENIED
    And match notOrganizer.receiveNoMoreResponses() == true
