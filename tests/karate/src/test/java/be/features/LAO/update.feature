@env=go_client,scala_client
Feature: Update a LAO

  Background:
    # This feature will be called to test Update LAO messages
    * call read('classpath:be/features/utils/constants.feature')
    * call read(serverFeature)
    * call read(mockClientFeature)
    * def organizer = call createMockClient
    * def lao = organizer.createValidLao()

    # This call executes all the steps to create a valid lao on the server before every scenario
    # (lao creation, subscribe, catchup)
    * call read(createLaoScenario) { organizer: '#(organizer)', lao: '#(lao)' }

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

