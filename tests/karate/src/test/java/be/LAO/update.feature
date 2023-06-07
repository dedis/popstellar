@env=go_client,scala_client
Feature: Update a LAO

  Background:
    # This feature will be called to test Update LAO messages
    # Call read(...) makes this feature and the called feature share the same scope
    # Meaning they share def variables, configurations ...
    # Especially JS functions defined in the called features can be directly used here thanks to Karate shared scopes
    * call read('classpath:be/utils/server.feature')
    * call read('classpath:be/mockClient.feature')
    * call read('classpath:be/constants.feature')
    * def organizer = call createMockClient
    * def lao = organizer.createValidLao()

    # This call executes all the steps to create a valid lao on the server before every scenario
    # (lao creation, subscribe, catchup)
    * call read('classpath:be/utils/simpleScenarios.feature@name=valid_lao') { organizer: '#(organizer)', lao: '#(lao)' }

  Scenario: Update Lao should succeed with a valid update request with new lao name
    Given def updateLaoRequest =
      """
        {
          "object": "lao",
          "action": "update_properties",
          "id": '#(lao.id)',
          "name": '#(random.generateName())',
          "last_modified": '#(lao.creation)',
          "witnesses": '#(lao.witnesses)'
        }
      """
    When organizer.publish(updateLaoRequest, lao.channel)
    And json answer = organizer.getBackendResponse(updateLaoRequest)
    Then match answer contains VALID_MESSAGE
    And match organizer.receiveNoMoreResponses() == true

  Scenario: Update Lao request with empty lao name should fail with an error response
    Given def badUpdateLaoReq =
      """
        {
          "object": "lao",
          "action": "update_properties",
          "id": '#(lao.id)',
          "name": "",
          "last_modified": '#(lao.creation)',
          "witnesses": '#(lao.witnesses)'
        }
      """
    When organizer.publish(badUpdateLaoReq, lao.channel)
    And json answer = organizer.getBackendResponse(badUpdateLaoReq)
    Then match answer contains INVALID_MESSAGE_FIELD
    And match organizer.receiveNoMoreResponses() == true


  Scenario: Update Lao with last_modified before creation time should fail with an error response
    Given def badUpdateLaoReq =
         """
        {
          "object": "lao",
          "action": "update_properties",
          "id": '#(lao.id)',
          "name": '#(random.generateName())',
          "last_modified": '#(lao.creation - 1)',
          "witnesses": '#(lao.witnesses)'
        }
      """
    When organizer.publish(badUpdateLaoReq, lao.channel)
    And json answer = organizer.getBackendResponse(badUpdateLaoReq)
    Then match answer contains INVALID_MESSAGE_FIELD
    And match organizer.receiveNoMoreResponses() == true

