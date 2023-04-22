@env=go,scala
Feature: Update a LAO

  Background:
    # This is feature will be called to test Update LAO messages
    # This call makes this feature and server.feature share the same scope
    # Meaning they share def variables, configurations ...
    # Especially JS functions defined in server.feature can be directly used here thanks to Karate shared scopes
    * call read('classpath:be/utils/server.feature')
    * call read('classpath:be/mockFrontEnd.feature')
    * call read('classpath:be/constants.feature')
    * string channel = "/root/p_EYbHyMv6sopI5QhEXBf40MO_eNoq7V_LygBd4c9RA="

  # After setting up a valid lao, subscribing to it and sending a catchup, test that
  # sending a valid update lao message receives a valid response from the backend.
  Scenario: Update Lao should succeed with a valid update request
    Given call read('classpath:be/utils/simpleScenarios.feature@name=valid_lao')
    And def updateLaoRequest =
      """
        {
          "object": "lao",
          "action": "update_properties",
          "id": "p_EYbHyMv6sopI5QhEXBf40MO_eNoq7V_LygBd4c9RA=",
          "name": "LAO2",
          "last_modified": 1633099140,
          "witnesses": []
        }
      """
    When frontend.publish(JSON.stringify(updateLaoRequest), channel)
    And json answer = frontend.getBackendResponse(JSON.stringify(updateLaoRequest))
    Then match answer contains VALID_MESSAGE
    And match frontend.receiveNoMoreResponses() == true

  # After setting up a valid lao, subscribing to it and sending a catchup, test that
  # sending an update lao message with empty name receives an error message from the backend.
  Scenario: Update Lao request with empty lao name should fail with an error response
    Given call read('classpath:be/utils/simpleScenarios.feature@name=valid_lao')
    And def badUpdateLaoReq =
      """
        {
          "object": "lao",
          "action": "update_properties",
          "id": "p_EYbHyMv6sopI5QhEXBf40MO_eNoq7V_LygBd4c9RA=",
          "name": "",
          "last_modified": 1633099140,
          "witnesses": []
        }
      """
    When frontend.publish(JSON.stringify(badUpdateLaoReq), channel)
    And json answer = frontend.getBackendResponse(JSON.stringify(badUpdateLaoReq))
    Then match answer contains INVALID_MESSAGE_FIELD
    And match frontend.receiveNoMoreResponses() == true


  # After setting up a valid lao, subscribing to it and sending a catchup, test that
  # sending an update lao message negative time receives an error message from the backend.
  Scenario: Update Lao with negative last_modified should fail with an error response
    Given call read('classpath:be/utils/simpleScenarios.feature@name=valid_lao')
    And def badUpdateLaoReq =
      """
        {
          "object": "lao",
          "action": "update_properties",
          "id": "p_EYbHyMv6sopI5QhEXBf40MO_eNoq7V_LygBd4c9RA=",
          "name": "LAO",
          "last_modified": -1633099140,
          "witnesses": []
        }
      """
    When frontend.publish(JSON.stringify(badUpdateLaoReq), channel)
    And json answer = frontend.getBackendResponse(JSON.stringify(badUpdateLaoReq))
    Then match answer contains INVALID_MESSAGE_FIELD
    And match frontend.receiveNoMoreResponses() == true

