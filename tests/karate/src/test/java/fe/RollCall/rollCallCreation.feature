@env=android,web
Feature: Create RollCall

  Scenario: Creating a Roll Call send right message to backend and displays element

    # Call the lao creation scenario that is front-end agnostic
    * call read('classpath:fe/utils/simpleScenarios.feature@name=create_lao')

    # Call the roll call creation util that is front-end dependant
    * string rc_name = "Roll-Call"
    * def rc_page_object = 'classpath:fe/utils/<env>.feature@name=create_roll_call'
    * replace rc_page_object.env = karate.env

    Given call read(rc_page_object)
    * backend.clearBuffer()
    And backend.setValidBroadcastMode()

    When click(roll_call_confirm_selector)

    # Retrieving sent messages
    * json create_rc_json = buffer.takeTimeout(timeout)
    * string create_rc_string = create_rc_json
    * print create_rc_string

    # General message verification
    Then match create_rc_json contains deep { method: 'publish' }
    * match messageVerification.verifyMessageIdField(create_rc_string) == true
    And match messageVerification.verifyMessageSignature(create_rc_string) == true

    # Roll Call specific verification
    * match rollCallVerification.verifyCreateAction(create_rc_string) == true
    And match rollCallVerification.verifyObject(create_rc_string) == true
    * match (rollCallVerification.verifyRollCallName(create_rc_string, rc_name)) == true
    And match rollCallVerification.verifyRollCallId(create_rc_string) == true

    And match backend.receiveNoMoreResponses() == true

    # check display
    And match text(event_name_selector) contains rc_name



