@env=android,web
Feature: Open Roll Call

  Scenario: Creating a Roll Call send right message to backend and displays element
    Given call read('classpath:fe/utils/simpleScenarios.feature@name=create_roll_call')
    * def rc_page_object = 'classpath:fe/utils/<env>.feature@name=open_roll_call'
    * replace rc_page_object.env = karate.env


    * backend.setRollCallCreateMode()
    # Unused, just needed to clear the buffer when the create message arrives
    * json create_msg = buffer.takeTimeout(timeout)
    And call read(rc_page_object)

    # Retrieving sent messages
    * json open_rc_json = buffer.takeTimeout(timeout)
    * string open_rc_string = open_rc_json

    Then match open_rc_json contains deep { method: 'publish'}
    And match backend.checkPublishMessage(open_rc_string) == true
    And match backend.checkRollCallOpenMessage(open_rc_string) == true
    And match backend.receiveNoMoreResponses() == true
