@env=android,web
Feature: Create RollCall

  Background:
    * call read('classpath:fe/utils/simpleScenarios.feature@name=create_lao')


  Scenario: Opening a Roll Call send right message to backend
    When click(add_event_selector)
    And click(add_roll_call_selector)
    And input(roll_call_title_selector, 'RC name')
    Given backend.clearBuffer()
    And click(roll_call_open_selector)

    # Retrieving sent messages
    * json create_lao = buffer.takeTimeout(timeout)
    * json subscribe = buffer.takeTimeout(withMethod('subscribe'), timeout)
    * json catchup = buffer.takeTimeout(withMethod('catchup'), timeout)

    # TODO Test consensus subscription when it is implemented on both fe
    # * json subscribe_consensus = backend.takeTimeout(withMethod('subscribe'), timeout)
    # * json catchup_consensus = backend.takeTimeout(withMethod('catchup'), timeout)

    Then match create_lao contains deep { method: 'publish', params: { channel: '/root' }}
    Then match subscribe contains deep { method: 'subscribe' }
    Then match catchup contains deep { method: 'catchup' }