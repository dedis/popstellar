@env=android,web
Feature: Create RollCall

  Background:
    * call read('classpath:fe/utils/simpleScenarios.feature@name=create_lao')


  Scenario: Creating a Roll Call send right message to backend
    When click(add_event_selector)
    And click(add_roll_call_selector)
    And input(roll_call_title_selector, 'RC name')
    Given backend.clearBuffer()
    And click(roll_call_confirm_selector)

    # Retrieving sent messages
    * json create_rc = buffer.takeTimeout(timeout)


    # TODO Test consensus subscription when it is implemented on both fe
    # * json subscribe_consensus = backend.takeTimeout(withMethod('subscribe'), timeout)
    # * json catchup_consensus = backend.takeTimeout(withMethod('catchup'), timeout)

    Then match create_rc contains deep { method: 'publish'}
    And match backend.receiveNoMoreResponses() == true
    # check display
