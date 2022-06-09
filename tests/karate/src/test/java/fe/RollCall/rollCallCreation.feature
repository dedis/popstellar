@env=android,web
Feature: Create RollCall

  Background:
    * call read('classpath:fe/utils/simpleScenarios.feature@name=create_lao')


  Scenario: Creating a Roll Call send right message to backend and displays element
    When click(add_event_selector)
    And click(add_roll_call_selector)
    And input(roll_call_title_selector, 'RC name')

    Given backend.clearBuffer()
    And backend.setRollCallCreateMode()

    When click(roll_call_confirm_selector)

    # Retrieving sent messages
    * json create_rc = buffer.takeTimeout(timeout)

    Then match create_rc contains deep { method: 'publish'}
    And match backend.receiveNoMoreResponses() == true
    And match text(event_name_selector) == 'RC name'
    # check display



