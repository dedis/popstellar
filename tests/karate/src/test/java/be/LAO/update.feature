@env=go,scala
Feature: Update a LAO

  Background:
    # This feature will be called to test Update LAO messages
    # This call makes this feature and server.feature share the same scope
    # Meaning they share def variables, configurations ...
    # Especially JS functions defined in server.feature can be directly used here thanks to Karate shared scopes
    * call read('classpath:be/utils/server.feature')
    * call read('classpath:be/mockFrontEnd.feature')
    * call read('classpath:be/constants.feature')
    * call read('classpath:be/utils/simpleScenarios.feature@name=valid_lao')
    * def frontend = call createFrontend
    * string channel = "/root/p_EYbHyMv6sopI5QhEXBf40MO_eNoq7V_LygBd4c9RA="

  Scenario: Update Lao should succeed with a valid update request
    Given def updateLaoRequest =
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
    When frontend.publish(updateLaoRequest, channel)
    And json answer = frontend.getBackendResponse(updateLaoRequest)
    Then match answer contains VALID_MESSAGE
    And match frontend.receiveNoMoreResponses() == true

  Scenario: Update Lao request with empty lao name should fail with an error response
    Given def badUpdateLaoReq =
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
    When frontend.publish(badUpdateLaoReq, channel)
    And json answer = frontend.getBackendResponse(badUpdateLaoReq)
    Then match answer contains INVALID_MESSAGE_FIELD
    And match frontend.receiveNoMoreResponses() == true


  Scenario: Update Lao with negative last_modified should fail with an error response
    Given def badUpdateLaoReq =
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
    When frontend.publish(badUpdateLaoReq, channel)
    And json answer = frontend.getBackendResponse(badUpdateLaoReq)
    Then match answer contains INVALID_MESSAGE_FIELD
    And match frontend.receiveNoMoreResponses() == true

