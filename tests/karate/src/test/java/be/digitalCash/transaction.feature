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
    * string cashChannel = "/root/p_EYbHyMv6sopI5QhEXBf40MO_eNoq7V_LygBd4c9RA="

  Scenario: Valid transaction sent by the organiser
  Scenario: Valid Roll Call
    Given call read('classpath:be/utils/simpleScenarios.feature@name=valid_lao')
    And def validTransaction =
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
    When frontend.publish(JSON.stringify(validTransaction), laoChannel)
    And json answer = frontend.getBackendResponse(JSON.stringify(validTransaction))
    Then match answer contains VALID_MESSAGE
    And match frontend.receiveNoMoreResponses() == true
