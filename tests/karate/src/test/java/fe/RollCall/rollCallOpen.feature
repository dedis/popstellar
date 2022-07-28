@env=android,web
Feature: Open Roll Call

  Scenario: Creating a Roll Call send right message to backend and displays element
    Given call read('classpath:fe/utils/simpleScenarios.feature@name=create_roll_call')
    * def rc_page_object = 'classpath:fe/utils/<env>.feature@name=open_roll_call'
    * replace rc_page_object.env = karate.env


    * backend.setValidBroadcastMode()
    And call read(rc_page_object)

    # Retrieving sent messages
    * json open_rc_json = buffer.takeTimeout(timeout)
    * string open_rc_string = open_rc_json

     # General message verification
    Then match open_rc_json contains deep { method: 'publish' }
    * match messageVerification.verifyMessageIdField(open_rc_string) == true
    And match messageVerification.verifyMessageSignature(open_rc_string) == true

    # Roll Call specific verification
    And match verificationUtils.getObject(open_rc_string) == constants.ROLL_CALL
    * match verificationUtils.getAction(open_rc_string) == constants.OPEN
    And match (rollCallVerification.verifyRollCallUpdateId(open_rc_string, constants.OPEN)) == true

    And match backend.receiveNoMoreResponses() == true
