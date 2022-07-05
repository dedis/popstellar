Feature:

  Scenario: Closing a roll call without attendees include only organizer
    # Do all the steps until (and included) opening a roll call
    * call read('classpath:fe/utils/simpleScenarios.feature@name=open_roll_call')

    # Close the opened roll-call
    * def rc_page_object = 'classpath:fe/utils/<env>.feature@name=close_roll_call'
    * replace rc_page_object.env = karate.env
    And call read(rc_page_object)

    # Retrieving sent messages
    * json close_rc_json = buffer.takeTimeout(timeout)
    * string close_rc_string = close_rc_json

    # General message verification
    Then match close_rc_json contains deep { method: 'publish' }
    * match messageVerification.verifyMessageIdField(close_rc_string) == true
    And match messageVerification.verifyMessageSignature(close_rc_string) == true

    # Roll Call specific verification
    And match verificationUtils.getObject(close_rc_string) == constants.ROLL_CALL
    * match verificationUtils.getAction(close_rc_string) == constants.CLOSE
    And match (rollCallVerification.verifyRollCallUpdateId(close_rc_string, constants.CLOSE)) == true
    * match rollCallVerification.verifyAttendeesPresence(close_rc_string) == true

    And match backend.receiveNoMoreResponses() == true

  Scenario: RC close several attendees
    # Do all the steps until (and included) opening a roll call
    * call read('classpath:fe/utils/simpleScenarios.feature@name=open_roll_call')

    # Close the opened roll-call
    * def rc_page_object = 'classpath:fe/utils/<env>.feature@name=close_roll_call_w_attendees'
    * replace rc_page_object.env = karate.env
    And call read(rc_page_object) {token1 : 'J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM=', token2: 'oKHk3AivbpNXk_SfFcHDaVHcCcY8IBfHE7auXJ7h4ms='}

    # Retrieving sent messages
    * json close_rc_json = buffer.takeTimeout(timeout)
    * string close_rc_string = close_rc_json

    # General message verification
    Then match close_rc_json contains deep { method: 'publish' }
    * match messageVerification.verifyMessageIdField(close_rc_string) == true
    And match messageVerification.verifyMessageSignature(close_rc_string) == true

    # Roll Call specific verification
    And match verificationUtils.getObject(close_rc_string) == constants.ROLL_CALL
    * match verificationUtils.getAction(close_rc_string) == constants.CLOSE
    And match (rollCallVerification.verifyRollCallUpdateId(close_rc_string, constants.CLOSE)) == true
    * match (rollCallVerification.verifyAttendeesPresence(close_rc_string, token1, token2)) == true

    And match backend.receiveNoMoreResponses() == true
