@env=go,scala
Feature: Cast a vote
  Background:
    # This feature will be called to test Cast Vote
    * call read('classpath:be/features/utils/constants.feature')
    * call read(serverFeature)
    * call read(mockClientFeature)
    * def organizer = call createMockFrontend
    * def lao = organizer.generateValidLao()
    * def rollCall = organizer.generateValidRollCall(lao)
    * def election = organizer.generateValidElection(lao)
    * def question = election.createQuestion()

    # This call executes all the steps to set up a lao, complete a roll call and open an election with one question
    * call read(openElectionScenario) { organizer: '#(organizer)', lao: '#(lao)', rollCall: '#(rollCall)',  election: '#(election)', question: '#(question)' }
    * def vote = question.createVote(0)
    * def castVote = election.castVote(vote)

  # Testing if after creating an election correctly, casting a valid vote succeeds
  @castVote1
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
  @castVote2
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
  @castVote3
  Scenario: Casting a valid vote on non existent election should return an error
    Given def newElection = organizer.generateValidElection(lao)
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
  @castVote4
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
  @castVote5
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
  @castVote6
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
  @castVote7
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
