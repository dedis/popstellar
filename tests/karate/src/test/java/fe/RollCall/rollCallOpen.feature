Feature: Open Roll Call

  Scenario: Creating a Roll Call send right message to backend and displays element
    * def rc_page_object = 'classpath:fe/utils/<env>.feature@name=create_roll_call'
    * replace rc_page_object.env = karate.env

    Given call read('classpath:fe/utils/simpleScenarios.feature@name=create_roll_call')
    * backend.clearBuffer()
    * backend.setRollCallCreateMode()
    And call read(rc_page_object)

        # Retrieving sent messages
    * json create_rc_json = buffer.takeTimeout(timeout)
    * string create_rc_string = create_rc_json

    Then match create_rc_json contains deep { method: 'publish'}
    And match backend.checkPublishMessage(create_rc_string) == true
    And match backend.checkRollCallOpenMessage(create_rc_string) == true
    And match backend.receiveNoMoreResponses() == true
