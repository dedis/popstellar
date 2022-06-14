@env=go,scala
Feature: Setup an Election
  Background:
        # This is feature will be called to test Election Setup
        # For every test a file containing the json representation of the message is read
        # then sent to the backend and stored there.
        # This is done via :
        # eval frontend.send(<message>) where a mock frontend sends a message to backend
        # Then the response sent by the backend and stored in a buffer :
        # json response = frontend_buffer.takeTimeout(timeout)
        # and checked if it contains the desired fields with :
        # match response contains deep <desired fields>

    # The following calls make this feature, mockFrontEnd.feature and server.feature share the same scope
    * call read('classpath:be/utils/server.feature')
    * call read('classpath:be/mockFrontEnd.feature')
    * call read('classpath:be/constants.feature')
    * string laoChannel = "/root/p_EYbHyMv6sopI5QhEXBf40MO_eNoq7V_LygBd4c9RA="

  # Testing if after a successful roll call, sending a valid election
  # setup results in a valid response from the backend
  Scenario: Setting up a valid election should succeed
    Given call read('classpath:be/utils/simpleScenarios.feature@name=valid_roll_call')
    And def validElectionSetup =
      """
        {
          "object": "election",
          "action": "setup",
          "id": '#(getValidElectionSetupId)',
          "lao": '#(getLaoValid)',
          "name": "Election",
          "version": "1.0.0",
          "created_at": 1633098941,
          "start_time": 1633098941,
          "end_time": 1633099812,
          "questions": [
            {
              "id": '#(getIsThisProjectFunQuestionId)',
              "question": "Is this project fun?",
              "voting_method": "Plurality",
              "ballot_options": ["Yes", "No"],
              "write_in": false
            }
          ]
        }
      """
    When frontend.publish(JSON.stringify(validElectionSetup), laoChannel)
    And json answer = frontend.getBackendResponse(JSON.stringify(validElectionSetup))
    Then match answer contains VALID_MESSAGE
    And match frontend.receiveNoMoreResponses() == true

  # Testing if after a successful roll call, sending an election
  # setup message with invalid election id results in an error message
  # from the backend
  Scenario: Setting up an election with invalid election id should fail
    Given call read('classpath:be/utils/simpleScenarios.feature@name=valid_roll_call')
    And def validElectionSetup =
      """
        {
          "object": "election",
          "action": "setup",
          "id": '#(getInvalidElectionSetupId)',
          "lao": '#(getLaoValid)',
          "name": "Election",
          "version": "1.0.0",
          "created_at": 1633098941,
          "start_time": 1633098941,
          "end_time": 1633099812,
          "questions": [
            {
              "id": '#(getIsThisProjectFunQuestionId)',
              "question": "Is this project fun?",
              "voting_method": "Plurality",
              "ballot_options": ["Yes", "No"],
              "write_in": false
            }
          ]
        }
      """
    When frontend.publish(JSON.stringify(validElectionSetup), laoChannel)
    And json answer = frontend.getBackendResponse(JSON.stringify(validElectionSetup))
    Then match answer contains INVALID_MESSAGE_FIELD
    And match frontend.receiveNoMoreResponses() == true
  # Testing if after a successful roll call, sending an election setup message
  # invalid question id results in an error message from the backend
  Scenario: Setting up an election with invalid question id should fail
    Given call read('classpath:be/utils/simpleScenarios.feature@name=valid_roll_call')
    And def validElectionSetup =
      """
        {
          "object": "election",
          "action": "setup",
          "id": '#(getValidElectionSetupId)',
          "lao": '#(getLaoValid)',
          "name": "Election",
          "version": "1.0.0",
          "created_at": 1633098941,
          "start_time": 1633098941,
          "end_time": 1633099812,
          "questions": [
            {
              "id": '#(getInvalidQuestionId)',
              "question": "Is this project fun?",
              "voting_method": "Plurality",
              "ballot_options": ["Yes", "No"],
              "write_in": false
            }
          ]
        }
      """
    When frontend.publish(JSON.stringify(validElectionSetup), laoChannel)
    And json answer = frontend.getBackendResponse(JSON.stringify(validElectionSetup))
    Then match answer contains INVALID_MESSAGE_FIELD
    And match frontend.receiveNoMoreResponses() == true
#  # Testing if after a successful roll call, sending an election setup message
#  # containing empty ballot options results in an error message from the backend
  Scenario: Setting up an election with a question containing empty ballot options should return an error
    Given call read('classpath:be/utils/simpleScenarios.feature@name=valid_roll_call')
    And def validElectionSetup =
      """
        {
          "object": "election",
          "action": "setup",
          "id": '#(getValidElectionSetupId)',
          "lao": '#(getLaoValid)',
          "name": "Election",
          "version": "1.0.0",
          "created_at": 1633098941,
          "start_time": 1633098941,
          "end_time": 1633099812,
          "questions": [
            {
              "id": '#(getIsThisProjectFunQuestionId)',
              "question": "Is this project fun?",
              "voting_method": "Plurality",
              "ballot_options": [],
              "write_in": false
            }
          ]
        }
      """
    When frontend.publish(JSON.stringify(validElectionSetup), laoChannel)
    And json answer = frontend.getBackendResponse(JSON.stringify(validElectionSetup))
    Then match answer contains INVALID_MESSAGE_FIELD
    And match frontend.receiveNoMoreResponses() == true
#  # Testing if after a successful roll call, sending an election setup message
#  # with a non supported voting method results in an error message from the backend
  Scenario: Setting up an election with a non-supported voting method should return an error
    Given call read('classpath:be/utils/simpleScenarios.feature@name=valid_roll_call')
    And def validElectionSetup =
      """
        {
          "object": "election",
          "action": "setup",
          "id": '#(getValidElectionSetupId)',
          "lao": '#(getLaoValid)',
          "name": "Election",
          "version": "1.0.0",
          "created_at": 1633098941,
          "start_time": 1633098941,
          "end_time": 1633099812,
          "questions": [
            {
              "id": '#(getIsThisProjectFunQuestionId)',
              "question": "Is this project fun?",
              "voting_method": "Random",
              "ballot_options": ["Yes", "No"],
              "write_in": false
            }
          ]
        }
      """
    When frontend.publish(JSON.stringify(validElectionSetup), laoChannel)
    And json answer = frontend.getBackendResponse(JSON.stringify(validElectionSetup))
    Then match answer contains INVALID_MESSAGE_FIELD
    And match frontend.receiveNoMoreResponses() == true
