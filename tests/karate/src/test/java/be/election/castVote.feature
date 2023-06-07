@env=go_client,scala_client
Feature: Cast a vote
  Background:
        # This feature will be called to test Cast Vote
        # For every test a file containing the json representation of the message is read
        # then sent to the backend and stored there.
        # This is done via :
        # eval frontend.send(<message>) where a mock frontend sends a message to backend
        # Then the response sent by the backend and stored in a buffer :
        # json response = frontend_buffer.takeTimeout(timeout)
        # and checked if it contains the desired fields with :
        # match response contains deep <desired fields>

    # The following calls makes this feature, mockFrontEnd.feature and server.feature share the same scope
    * call read('classpath:be/utils/server.feature')
    * call read('classpath:be/mockFrontEnd.feature')
    * call read('classpath:be/constants.feature')
    * string electionChannel = "/root/p_EYbHyMv6sopI5QhEXBf40MO_eNoq7V_LygBd4c9RA=/rdv-0minecREM9XidNxnQotO7nxtVVnx-Zkmfm7hm2w="
    * string laoChannel = "/root/p_EYbHyMv6sopI5QhEXBf40MO_eNoq7V_LygBd4c9RA="

  # Testing if after creating an election correctly, casting a valid vote succeeds
  Scenario: Casting a valid vote on a started election
    Given call read('classpath:be/utils/simpleScenarios.feature@name=election_open')
    And def validCastVote =
      """
        {
          "object": "election",
          "action": "cast_vote",
          "lao": '#(getLaoValid)',
          "election": '#(getValidElectionSetupId)',
          "created_at": 1633098941,
          "votes": [
            {
              "id": '#(getIsThisProjectFunVoteIdVoteYes)',
              "question": '#(getIsThisProjectFunQuestionId)',
              "vote": 0
            }
          ]
        }
      """
    When frontend.publish(validCastVote, electionChannel)
    And json answer = frontend.getBackendResponse(validCastVote)
    Then match answer contains VALID_MESSAGE
    And match frontend.receiveNoMoreResponses() == true

  # Testing if after creating an election correctly, the backend returns an error
  # upon casting a vote on an LAO channel instead of an election one
  Scenario: Casting a vote on a lao channel should return an error
    Given call read('classpath:be/utils/simpleScenarios.feature@name=election_open')
    And def validCastVote =
      """
        {
          "object": "election",
          "action": "cast_vote",
          "lao": '#(getLaoValid)',
          "election": '#(getValidElectionSetupId)',
          "created_at": 1633098941,
          "votes": [
            {
              "id": '#(getIsThisProjectFunVoteIdVoteYes)',
              "question": '#(getIsThisProjectFunQuestionId)',
              "vote": 0
            }
          ]
        }
      """
    When frontend.publish(validCastVote, laoChannel)
    And json answer = frontend.getBackendResponse(validCastVote)
    Then match answer contains INVALID_MESSAGE_FIELD
    And match frontend.receiveNoMoreResponses() == true

# Testing if before creating an election, the backend returns an error
# upon casting a vote
  Scenario: Casting a valid vote on non existent election should return an error
    Given call read('classpath:be/utils/simpleScenarios.feature@name=election_open')
    And def validCastVote =
      """
        {
          "object": "election",
          "action": "cast_vote",
          "lao": '#(getLaoValid)',
          "election": '#(getInvalidElectionSetupId)',
          "created_at": 1633098941,
          "votes": [
            {
              "id": '#(getIsThisProjectFunVoteIdVoteYes)',
              "question": '#(getIsThisProjectFunQuestionId)',
              "vote": 0
            }
          ]
        }
      """
    When frontend.publish(validCastVote, electionChannel)
    And json answer = frontend.getBackendResponse(validCastVote)
    Then match answer contains INVALID_MESSAGE_FIELD
    And match frontend.receiveNoMoreResponses() == true

  # Testing if after creating an election correctly, the backend returns an error
  # upon casting a vote but with wrong vote id
  Scenario: Casting a vote with wrong vote id should return an error
    Given call read('classpath:be/utils/simpleScenarios.feature@name=election_open')
    And def invalidCastVote =
      """
        {
          "object": "election",
          "action": "cast_vote",
          "lao": '#(getLaoValid)',
          "election": '#(getValidElectionSetupId)',
          "created_at": 1633098941,
          "votes": [
            {
              "id": '#(getInvalidVoteId)',
              "question": '#(getIsThisProjectFunQuestionId)',
              "vote": 0
            }
          ]
        }
      """
    When frontend.publish(invalidCastVote, electionChannel)
    And json answer = frontend.getBackendResponse(invalidCastVote)
    Then match answer contains INVALID_MESSAGE_FIELD
    And match frontend.receiveNoMoreResponses() == true

  # Testing if after creating an election correctly, the backend returns an error
  # upon casting a vote but with lao id as vote id
  Scenario: Casting a vote with lao id as vote id should return an error
    Given call read('classpath:be/utils/simpleScenarios.feature@name=election_open')
    And def invalidCastVote =
      """
        {
          "object": "election",
          "action": "cast_vote",
          "lao": '#(getLaoValid)',
          "election": '#(getValidElectionSetupId)',
          "created_at": 1633098941,
          "votes": [
            {
              "id": '#(getLaoValid)',
              "question": '#(getIsThisProjectFunQuestionId)',
              "vote": 0
            }
          ]
        }
      """
    When frontend.publish(invalidCastVote, electionChannel)
    And json answer = frontend.getBackendResponse(invalidCastVote)
    Then match answer contains INVALID_MESSAGE_FIELD
    And match frontend.receiveNoMoreResponses() == true

  # Testing if after creating an election correctly, the backend returns an error
  # upon a non-attendee casting a valid vote.
  Scenario: Non attendee casting a vote should return an error
    Given call read('classpath:be/utils/simpleScenarios.feature@name=election_open')
    And def invalidCastVote =
      """
        {
          "object": "election",
          "action": "cast_vote",
          "lao": '#(getLaoValid)',
          "election": '#(getValidElectionSetupId)',
          "created_at": 1633098941,
          "votes": [
            {
              "id": '#(getIsThisProjectFunVoteIdVoteYes)',
              "question": '#(getIsThisProjectFunQuestionId)',
              "vote": 0
            }
          ]
        }
      """
    And frontend.changeSenderToBeNonAttendee()
    When frontend.publish(invalidCastVote, electionChannel)
    And json answer = frontend.getBackendResponse(invalidCastVote)
    Then match answer contains INVALID_MESSAGE_FIELD
    And match frontend.receiveNoMoreResponses() == true

  # Testing if casting a valid vote at a time after election creation should fail
  Scenario: Casting a valid vote before time should fail
    Given call read('classpath:be/utils/simpleScenarios.feature@name=election_open')
    And def invalidCastVote =
      """
        {
          "object": "election",
          "action": "cast_vote",
          "lao": '#(getLaoValid)',
          "election": '#(getValidElectionSetupId)',
          "created_at": 1633098900,
          "votes": [
            {
              "id": '#(getIsThisProjectFunVoteIdVoteYes)',
              "question": '#(getIsThisProjectFunQuestionId)',
              "vote": 0
            }
          ]
        }
      """
    When frontend.publish(invalidCastVote, electionChannel)
    And json answer = frontend.getBackendResponse(invalidCastVote)
    Then match answer contains INVALID_MESSAGE_FIELD
    And match frontend.receiveNoMoreResponses() == true
