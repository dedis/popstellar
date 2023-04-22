@env=go,scala
Feature: Send heartbeats to other servers

  Background:
    # This is feature will be called to test sending heartbeats to other servers.
    # This call makes this feature and server.feature share the same scope
    # Meaning they share def variables, configurations ...
    # Especially JS functions defined in server.feature can be directly used here thanks to Karate shared scopes
    # This also sets up a valid lao by sending a create lao message, subscribing to the lao and sending a catchup message
    * call read('classpath:be/utils/server.feature')
    * call read('classpath:be/mockFrontEnd.feature')
    * call read('classpath:be/constants.feature')
    * call read('classpath:be/utils/simpleScenarios.feature@name=valid_lao')
    * string channel = "/root/p_EYbHyMv6sopI5QhEXBf40MO_eNoq7V_LygBd4c9RA="

  # message and expect to receive a valid response from the backend
  Scenario: Close a valid roll should succeed
    Given call read('classpath:be/utils/simpleScenarios.feature@name=open_roll_call')
    And def validCloseRollCall =
      """
        {
          "object": "roll_call",
          "action": "close",
          "update_id": '#(getRollCallCloseValidUpdateId)',
          "closes": '#(getRollCallCloseValidId)',
          "closed_at": '#(getRollCallCloseValidCreationTime)',
          "attendees": ['#(getAttendee)']
        }
      """
    When frontend.publish(JSON.stringify(validCloseRollCall), laoChannel)
    And json answer = frontend.getBackendResponse(JSON.stringify(validCloseRollCall))
    Then match answer contains VALID_MESSAGE
    And match frontend.receiveNoMoreResponses() == true
