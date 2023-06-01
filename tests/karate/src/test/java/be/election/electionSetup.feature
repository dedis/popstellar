@env=go,scala
Feature: Setup an Election
  Background:
    # This feature will be called to test Election Setup
    # Call read(...) makes this feature and the called feature share the same scope
    # Meaning they share def variables, configurations ...
    # Especially JS functions defined in the called features can be directly used here thanks to Karate shared scopes
    * call read('classpath:be/utils/server.feature')
    * call read('classpath:be/mockClient.feature')
    * call read('classpath:be/constants.feature')
    * def organizer = call createMockClient
    * def lao = organizer.createValidLao()
    * def rollCall = organizer.createValidRollCall(lao)
    * def election = organizer.createValidElection(lao)
    * def question = election.addRandomQuestion()

    # This call executes all the steps to set up a lao and complete a roll call, to get a valid pop token
    # (lao creation, subscribe, catchup, roll call creation, roll call open, roll call close)
    * call read('classpath:be/utils/simpleScenarios.feature@name=close_roll_call') { organizer: '#(organizer)', lao: '#(lao)', rollCall: '#(rollCall)' }

  # Testing if after a successful roll call, sending a valid election
  # setup results in a valid response from the backend
  Scenario: Setting up a valid election should succeed
    And def validElectionSetup =
      """
        {
          "object": "election",
          "action": "setup",
          "id": '#(election.id)',
          "lao": '#(lao.id)',
          "name": '#(election.name)',
          "version": '#(election.version)',
          "created_at": '#(election.creation)',
          "start_time": '#(election.start)',
          "end_time": '#(election.end)',
          "questions": [
            {
              "id": '#(question.id)',
              "question": '#(question.question)',
              "voting_method": '#(question.votingMethod)',
              "ballot_options": '#(question.ballotOptions)',
              "write_in": '#(question.writeIn)',
            }
          ]
        }
      """
    When organizer.publish(validElectionSetup, lao.channel)
    And json answer = organizer.getBackendResponse(validElectionSetup)
    Then match answer contains VALID_MESSAGE
    And match organizer.receiveNoMoreResponses() == true

  # Testing if after a successful roll call, sending an election
  # setup message with invalid election id results in an error message
  # from the backend
  Scenario: Setting up an election with invalid election id should fail
    And def invalidElectionSetup =
        """
        {
          "object": "election",
          "action": "setup",
          "id": '#(random.generateElectionSetupId())',
          "lao": '#(lao.id)',
          "name": '#(election.name)',
          "version": '#(election.version)',
          "created_at": '#(election.creation)',
          "start_time": '#(election.start)',
          "end_time": '#(election.end)',
          "questions": [
            {
              "id": '#(question.id)',
              "question": '#(question.question)',
              "voting_method": '#(question.votingMethod)',
              "ballot_options": '#(question.ballotOptions)',
              "write_in": '#(question.writeIn)',
            }
          ]
        }
      """
    When organizer.publish(invalidElectionSetup, lao.channel)
    And json answer = organizer.getBackendResponse(invalidElectionSetup)
    Then match answer contains INVALID_MESSAGE_FIELD
    And match organizer.receiveNoMoreResponses() == true

  # Testing if after a successful roll call, sending an election setup message
  # invalid question id results in an error message from the backend
  Scenario: Setting up an election with invalid question id should fail
    And def invalidElectionSetup =
       """
        {
          "object": "election",
          "action": "setup",
          "id": '#(election.id)',
          "lao": '#(lao.id)',
          "name": '#(election.name)',
          "version": '#(election.version)',
          "created_at": '#(election.creation)',
          "start_time": '#(election.start)',
          "end_time": '#(election.end)',
          "questions": [
            {
              "id": '#(random.generateElectionQuestionId())',
              "question": '#(question.question)',
              "voting_method": '#(question.votingMethod)',
              "ballot_options": '#(question.ballotOptions)',
              "write_in": '#(question.writeIn)',
            }
          ]
        }
      """
    When organizer.publish(invalidElectionSetup, lao.channel)
    And json answer = organizer.getBackendResponse(invalidElectionSetup)
    Then match answer contains INVALID_MESSAGE_FIELD
    And match organizer.receiveNoMoreResponses() == true

#  # Testing if after a successful roll call, sending an election setup message
#  # containing empty ballot options results in an error message from the backend
  Scenario: Setting up an election with a question containing empty ballot options should return an error
    And def invalidElectionSetup =
     """
        {
          "object": "election",
          "action": "setup",
          "id": '#(election.id)',
          "lao": '#(lao.id)',
          "name": '#(election.name)',
          "version": '#(election.version)',
          "created_at": '#(election.creation)',
          "start_time": '#(election.start)',
          "end_time": '#(election.end)',
          "questions": [
            {
              "id": '#(question.id)',
              "question": '#(question.question)',
              "voting_method": '#(question.votingMethod)',
              "ballot_options": [],
              "write_in": '#(question.writeIn)',
            }
          ]
        }
      """
    When organizer.publish(invalidElectionSetup, laoChannel)
    And json answer = organizer.getBackendResponse(invalidElectionSetup)
    Then match answer contains INVALID_MESSAGE_FIELD
    And match organizer.receiveNoMoreResponses() == true

#  # Testing if after a successful roll call, sending an election setup message
#  # with a non supported voting method results in an error message from the backend
  Scenario: Setting up an election with a non-supported voting method should return an error
    And def invalidElectionSetup =
        """
        {
          "object": "election",
          "action": "setup",
          "id": '#(election.id)',
          "lao": '#(lao.id)',
          "name": '#(election.name)',
          "version": '#(election.version)',
          "created_at": '#(election.creation)',
          "start_time": '#(election.start)',
          "end_time": '#(election.end)',
          "questions": [
            {
              "id": '#(question.id)',
              "question": '#(question.question)',
              "voting_method": "Random",
              "ballot_options": '#(question.ballotOptions)',
              "write_in": '#(question.writeIn)',
            }
          ]
        }
      """
    When organizer.publish(invalidElectionSetup, laoChannel)
    And json answer = organizer.getBackendResponse(invalidElectionSetup)
    Then match answer contains INVALID_MESSAGE_FIELD
    And match organizer.receiveNoMoreResponses() == true
