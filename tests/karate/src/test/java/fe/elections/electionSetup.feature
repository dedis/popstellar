Feature:

  Scenario: Creating an election sends the correct message to the backend
    # Do all the steps up until (and including) closing a roll call
    * call read('classpath:fe/utils/simpleScenarios.feature@name=close_roll_call')


    # Setup an election
    * def election_page_object = 'classpath:fe/utils/<env>.feature@name=setup_election'
    * replace election_page_object.env = karate.env
    And call read(election_page_object)

    # Retrieving sent messages
    * json election_json = buffer.takeTimeout(timeout)
    * print election_json
    * string election_string = election_json
    * json subscribe = buffer.takeTimeout(withMethod('subscribe'), timeout)
    * print subscribe
    * json catchup = buffer.takeTimeout(withMethod('catchup'), timeout)
    * print catchup

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

    And match backend.receiveNoMoreResponses() == true