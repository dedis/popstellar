Feature:

  Scenario: Creating an election sends the correct message to the backend
    # Do all the steps up until (and including) closing a roll call
    Given call read('classpath:fe/utils/simpleScenarios.feature@name=close_roll_call')

    # Setup an election
    When def election_page_object = 'classpath:fe/utils/<env>.feature@name=setup_election'
    * replace election_page_object.env = karate.env
    And call read(election_page_object)

    # Retrieving sent messages
    * json election_json = buffer.takeTimeout(timeout)
    * string election_string = election_json
    * json subscribe = buffer.takeTimeout(withMethod('subscribe'), timeout)
    * json catchup = buffer.takeTimeout(withMethod('catchup'), timeout)

    # General message verification
    Then match election_json contains deep { method: 'publish' }
    Then match subscribe contains deep { method: 'subscribe' }
    Then match catchup contains deep { method: 'catchup' }
    * match messageVerification.verifyMessageIdField(election_string) == true
    And match messageVerification.verifyMessageSignature(election_string) == true

    # Election specific verification
    And match verificationUtils.getObject(election_string) == constants.ELECTION
    * match verificationUtils.getAction(election_string) == constants.SETUP
    And match verificationUtils.getVersion(election_string) == constants.OPEN_BALLOT
    * match verificationUtils.getName(election_string) == constants.ELECTION_NAME
    And match electionVerification.verifyElectionId(election_string) == true
    * match electionVerification.verifyQuestionId(election_string) == true
    And match electionVerification.getQuestionContent(election_string) == constants.QUESTION_CONTENT
    * match (electionVerification.getBallotOption(election_string, 0)) == constants.BALLOT_1
    And match (electionVerification.getBallotOption(election_string, 1)) == constants.BALLOT_2
    * match electionVerification.getVotingMethod(election_string) == constants.PLURALITY

    And match backend.receiveNoMoreResponses() == true
