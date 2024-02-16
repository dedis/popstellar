@env=go,scala
Feature: Setup an Election
  Background:
    # This feature will be called to test Election Setup
    * call read('classpath:be/features/utils/constants.feature')
    * call read(serverFeature)
    * call read(mockClientFeature)
    * def organizer = call createMockFrontend
    * def lao = organizer.createValidLao()
    * def rollCall = organizer.createValidRollCall(lao)
    * def election = organizer.createValidElection(lao)
    * def question = election.createQuestion()

    # This call executes all the steps to set up a lao and complete a roll call, to get a valid pop token
    # (lao creation, subscribe, catchup, roll call creation, roll call open, roll call close)
    * call read(closeRollCallScenario) { organizer: '#(organizer)', lao: '#(lao)', rollCall: '#(rollCall)' }

  # Testing if after a successful roll call, sending a valid election
  # setup results in a valid response from the backend
  @electionSetup1
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
  @electionSetup2
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
  @electionSetup3
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
  @electionSetup4
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
    When organizer.publish(invalidElectionSetup, lao.channel)
    And json answer = organizer.getBackendResponse(invalidElectionSetup)
    Then match answer contains INVALID_MESSAGE_FIELD
    And match organizer.receiveNoMoreResponses() == true

#  # Testing if after a successful roll call, sending an election setup message
#  # with a non supported voting method results in an error message from the backend
  @electionSetup5
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
    When organizer.publish(invalidElectionSetup, lao.channel)
    And json answer = organizer.getBackendResponse(invalidElectionSetup)
    Then match answer contains INVALID_MESSAGE_FIELD
    And match organizer.receiveNoMoreResponses() == true
