@env=go,scala
Feature: Cast a vote
  Background:
    # This feature will be called to test Cast Vote
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

    # This call executes all the steps to set up a lao, complete a roll call and open an election with one question
    * call read('classpath:be/utils/simpleScenarios.feature@name=election_open') { organizer: '#(organizer)', lao: '#(lao)', rollCall: '#(rollCall)',  election: '#(election)', question: '#(question)' }
    * def vote = question.createVote(0)
    * def castVote = election.castVote(vote)

  # Testing if after creating an election correctly, casting a valid vote succeeds
  Scenario: Casting a valid vote on a started election
    Given def validCastVote =
      """
        {
          "object": "election",
          "action": "cast_vote",
          "lao": '#(lao.id)',
          "election": '#(election.id)',
          "created_at": '#(castVote.createdAt)',
          "votes": [
            {
              "id": '#(vote.id)',
              "question": '#(question.id)',
              "vote": '#(vote.index)'
            }
          ]
        }
      """
    When organizer.publish(validCastVote, election.channel)
    And json answer = organizer.getBackendResponse(validCastVote)
    Then match answer contains VALID_MESSAGE
    And match organizer.receiveNoMoreResponses() == true

  # Testing if after creating an election correctly, the backend returns an error
  # upon casting a vote on an LAO channel instead of an election one
  Scenario: Casting a vote on a lao channel should return an error
    Given def validCastVote =
      """
        {
          "object": "election",
          "action": "cast_vote",
          "lao": '#(lao.id)',
          "election": '#(election.id)',
          "created_at": '#(castVote.createdAt)',
          "votes": [
            {
              "id": '#(vote.id)',
              "question": '#(question.id)',
              "vote": '#(vote.index)'
            }
          ]
        }
      """
    When organizer.publish(validCastVote, lao.channel)
    And json answer = organizer.getBackendResponse(validCastVote)
    Then match answer contains INVALID_MESSAGE_FIELD
    And match organizer.receiveNoMoreResponses() == true

  # Testing if before creating an election, the backend returns an error
  # upon casting a vote
  Scenario: Casting a valid vote on non existent election should return an error
    Given def newElection = organizer.createValidElection(lao)
    And def newQuestion = newElection.createQuestion()
    And def newVote = newQuestion.createVote(0)
    And def newCastVote = newElection.castVote(newVote)
    And def validCastVote =
      """
        {
          "object": "election",
          "action": "cast_vote",
          "lao": '#(lao.id)',
          "election": '#(newElection.id)',
          "created_at": '#(newCastVote.createdAt)',
          "votes": [
            {
              "id": '#(newVote.id)',
              "question": '#(newQuestion.id)',
              "vote": '#(newVote.index)'
            }
          ]
        }
      """
    When organizer.publish(validCastVote, newElection.channel)
    And json answer = organizer.getBackendResponse(validCastVote)
    Then match answer contains INVALID_MESSAGE_FIELD
    And match organizer.receiveNoMoreResponses() == true

  # Testing if after creating an election correctly, the backend returns an error
  # upon casting a vote but with wrong vote id
  Scenario: Casting a vote with wrong vote id should return an error
    Given def invalidCastVote =
       """
        {
          "object": "election",
          "action": "cast_vote",
          "lao": '#(lao.id)',
          "election": '#(election.id)',
          "created_at": '#(castVote.createdAt)',
          "votes": [
            {
              "id": '#(random.generateElectionVoteId())',
              "question": '#(question.id)',
              "vote": '#(vote.index)'
            }
          ]
        }
      """
    When organizer.publish(invalidCastVote, election.channel)
    And json answer = organizer.getBackendResponse(invalidCastVote)
    Then match answer contains INVALID_MESSAGE_FIELD
    And match organizer.receiveNoMoreResponses() == true

  # Testing if after creating an election correctly, the backend returns an error
  # upon casting a vote but with lao id as vote id
  Scenario: Casting a vote with lao id as vote id should return an error
    Given def invalidCastVote =
     """
        {
          "object": "election",
          "action": "cast_vote",
          "lao": '#(lao.id)',
          "election": '#(election.id)',
          "created_at": '#(castVote.createdAt)',
          "votes": [
            {
              "id": '#(lao.id)',
              "question": '#(question.id)',
              "vote": '#(vote.index)'
            }
          ]
        }
      """
    When organizer.publish(invalidCastVote, election.channel)
    And json answer = organizer.getBackendResponse(invalidCastVote)
    Then match answer contains INVALID_MESSAGE_FIELD
    And match organizer.receiveNoMoreResponses() == true

  # Testing if after creating an election correctly, the backend returns an error
  # upon a non-attendee casting a valid vote.
  Scenario: Non attendee casting a vote should return an error
    Given def nonAttendee = call createMockClient
    And def validCastVote =
      """
        {
          "object": "election",
          "action": "cast_vote",
          "lao": '#(lao.id)',
          "election": '#(election.id)',
          "created_at": '#(castVote.createdAt)',
          "votes": [
            {
              "id": '#(vote.id)',
              "question": '#(question.id)',
              "vote": '#(vote.index)'
            }
          ]
        }
      """
    When nonAttendee.publish(validCastVote, election.channel)
    And json answer = nonAttendee.getBackendResponse(validCastVote)
    Then match answer contains INVALID_MESSAGE_FIELD
    And match nonAttendee.receiveNoMoreResponses() == true

  # Testing if casting a valid vote at a time before election creation should fail
  Scenario: Casting a valid vote before creation time should fail
    Given def invalidCastVote =
      """
        {
          "object": "election",
          "action": "cast_vote",
          "lao": '#(lao.id)',
          "election": '#(election.id)',
          "created_at": '#(election.creation - 1)',
          "votes": [
            {
              "id": '#(vote.id)',
              "question": '#(question.id)',
              "vote": '#(vote.index)'
            }
          ]
        }
      """
    When organizer.publish(invalidCastVote, election.channel)
    And json answer = organizer.getBackendResponse(invalidCastVote)
    Then match answer contains INVALID_MESSAGE_FIELD
    And match organizer.receiveNoMoreResponses() == true
