@env=android,web
Feature: Create RollCall

  Scenario: Creating a Roll Call sends right message to backend and displays element

    # Call the lao creation scenario that is front-end agnostic
    * call read('classpath:fe/utils/simpleScenarios.feature@name=create_lao')

    # Call the roll call creation util that is front-end dependant
    * def rc_page_object = 'classpath:fe/utils/<env>.feature@name=create_roll_call'
    * replace rc_page_object.env = karate.env

    Given call read(rc_page_object)
    * backend.clearBuffer()
    And backend.setValidBroadcastMode()

    When click(roll_call_confirm_selector)

    # Retrieving sent messages
    * json create_rc_json = buffer.takeTimeout(timeout)
    * string create_rc_string = create_rc_json

    # General message verification
    Then match create_rc_json contains deep { method: 'publish' }
    * match messageVerification.verifyMessageIdField(create_rc_string) == true
    And match messageVerification.verifyMessageSignature(create_rc_string) == true

    # Roll Call specific verification
    And match verificationUtils.getObject(create_rc_string) == constants.ROLL_CALL
    * match verificationUtils.getAction(create_rc_string) == constants.CREATE
    * match verificationUtils.getName(create_rc_string) == constants.RC_NAME
    And match rollCallVerification.verifyRollCallId(create_rc_string) == true

    And match backend.receiveNoMoreResponses() == true

    # check display
    And match text(event_name_selector) contains constants.RC_NAME



