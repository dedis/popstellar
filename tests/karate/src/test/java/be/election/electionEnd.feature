@env=go_client,scala_client
Feature: Terminate an election
  Background:
    # This feature will be called to test End Election
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
    * def question = election.createQuestion()

    # This call executes all the steps to set up a lao, complete a roll call, open an election and cast a vote
    * call read('classpath:be/utils/simpleScenarios.feature@name=cast_vote') { organizer: '#(organizer)', lao: '#(lao)', rollCall: '#(rollCall)',  election: '#(election)', question: '#(question)' }
    * def electionEnd = election.close()

  # After a successful election setup and cast vote sending a valid election end
  # message should succeed
  Scenario: Sending a valid election end should succeed
    Given def validElectionEnd =
      """
        {
          "object": "election",
          "action": "end",
          "lao": '#(lao.id)',
          "election": '#(election.id)',
          "created_at": '#(electionEnd.createdAt)',
          "registered_votes": '#(electionEnd.registeredVotes)'
        }
      """
    When organizer.publish(validElectionEnd, election.channel)
    And json answer = organizer.getBackendResponseWithElectionResults(validElectionEnd)
    Then match answer contains ELECTION_RESULTS
    And match organizer.receiveNoMoreResponses() == true

   # After having a successful election setup and vote casts, sending an election end
   # message that has an invalid election id should return an error form the backend
  Scenario: Sending an election end with invalid election id should return an error
    Given def invalidElectionEnd =
      """
        {
          "object": "election",
          "action": "end",
          "lao": '#(lao.id)',
          "election": '#(random.generateElectionSetupId())',
          "created_at": '#(electionEnd.createdAt)',
          "registered_votes": '#(electionEnd.registeredVotes)'
        }
      """
    When organizer.publish(invalidElectionEnd, election.channel)
    And json answer = organizer.getBackendResponse(invalidElectionEnd)
    Then match answer contains INVALID_MESSAGE_FIELD
    And match organizer.receiveNoMoreResponses() == true

  # After having a successful election setup and vote casts, sending an election end
  # message with a wrongly computed registered votes field should return an error form the backend
  Scenario: Sending an election end message with invalid registered votes field should return an error
    Given def invalidElectionEnd =
      """
        {
          "object": "election",
          "action": "end",
          "lao": '#(lao.id)',
          "election": '#(election.id)',
          "created_at": '#(electionEnd.createdAt)',
          "registered_votes": '#(random.generateRegisteredVotesHash())'
        }
      """
    When organizer.publish(invalidElectionEnd, election.channel)
    And json answer = organizer.getBackendResponse(invalidElectionEnd)
    Then match answer contains INVALID_MESSAGE_FIELD
    And match organizer.receiveNoMoreResponses() == true

  # After having a successful election setup and vote casts, sending an election end
  # message with a wrongly computed registered votes field should return an error form the backend
  Scenario: Sending an election end message with registered votes field set to valid election setup id should return an error
    Given def invalidElectionEnd =
      """
        {
          "object": "election",
          "action": "end",
          "lao": '#(lao.id)',
          "election": '#(election.id)',
          "created_at": '#(electionEnd.createdAt)',
          "registered_votes": '#(election.id)'
        }
      """
    When organizer.publish(invalidElectionEnd, election.channel)
    And json answer = organizer.getBackendResponse(invalidElectionEnd)
    Then match answer contains INVALID_MESSAGE_FIELD
    And match organizer.receiveNoMoreResponses() == true

  # Testing if sending an election end message with timestamp that is before the election setup
  # creation time should result in an error from the back-end
  Scenario: Sending a valid election end too early should fail
    Given def invalidElectionEnd =
      """
        {
          "object": "election",
          "action": "end",
          "lao": '#(lao.id)',
          "election": '#(election.id)',
          "created_at": '#(election.creation - 1)',
          "registered_votes": '#(electionEnd.registeredVotes)'
        }
      """
    When organizer.publish(invalidElectionEnd, election.channel)
    And json answer = organizer.getBackendResponse(invalidElectionEnd)
    Then match answer contains INVALID_MESSAGE_FIELD
    And match organizer.receiveNoMoreResponses() == true
