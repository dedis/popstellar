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
    * string election_string = election_json

    # General message verification
    Then match election_json contains deep { method: 'publish' }
    * match messageVerification.verifyMessageIdField(election_string) == true
    And match messageVerification.verifyMessageSignature(election_string) == true

    # Roll Call specific verification
    And match verificationUtils.getObject(election_string) == constants.ELECTION
    * match verificationUtils.getAction(election_string) == constants.SETUP