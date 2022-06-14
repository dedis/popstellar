@env=android,web
Feature: Create RollCall

  Background:
    * call read('classpath:fe/utils/simpleScenarios.feature@name=create_lao')
    * def rc_page_object = 'classpath:fe/utils/<env>.feature@name=create_roll_call'
    * replace rc_page_object.env = karate.env
    * call read(rc_page_object)

  Scenario: Creating a Roll Call send right message to backend and displays element
    Given backend.clearBuffer()
    And backend.setRollCallCreateMode()

    When click(roll_call_confirm_selector)

    # Retrieving sent messages
    * json create_rc_json = buffer.takeTimeout(timeout)
    * string create_rc_string = create_rc_json

    Then match create_rc_json contains deep { method: 'publish'}
    And match backend.checkRollCallCreateMessage(create_rc_string) == true
    And match backend.receiveNoMoreResponses() == true
    And match text(event_name_selector) == 'RC name'
    # check display



