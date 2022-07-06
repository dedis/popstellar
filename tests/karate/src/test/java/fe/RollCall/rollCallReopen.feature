Feature:

  Scenario:
     # Do all the steps up until (and including) closing a roll call
    * call read('classpath:fe/utils/simpleScenarios.feature@name=close_roll_call')

    # Reopen the closed roll-call
    * def rc_page_object = 'classpath:fe/utils/<env>.feature@name=reopen_roll_call'
    * replace rc_page_object.env = karate.env
    And call read(rc_page_object)

    # Retrieving sent messages
    * json reopen_rc_json = buffer.takeTimeout(timeout)
    * string reopen_rc_string = reopen_rc_json

     # General message verification
    Then match reopen_rc_json contains deep { method: 'publish' }
    * match messageVerification.verifyMessageIdField(reopen_rc_string) == true
    And match messageVerification.verifyMessageSignature(reopen_rc_string) == true

    # Roll Call specific verification
    And match verificationUtils.getObject(reopen_rc_string) == constants.ROLL_CALL
    * match verificationUtils.getAction(reopen_rc_string) == constants.REOPEN
    And match (rollCallVerification.verifyRollCallUpdateId(reopen_rc_string, constants.REOPEN)) == true

    And match backend.receiveNoMoreResponses() == true
