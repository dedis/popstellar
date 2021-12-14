Feature: Create LAO

  Background: Driver setup
    * call read('classpath:fe/utils/web.feature')

  Scenario: Create LAO
    When driver.click(tab_launch_selector)
    * delay(1000)
    And driver.input(tab_launch_lao_name_selector, 'Lao Name')
    And driver.click(tab_launch_create_lao_selector)

    * json create_lao = backend.takeTimeout(1000)
    * json subscribe = backend.takeTimeout(withMethod('subscribe'), 1000)
    * json catchup = backend.takeTimeout(withMethod('catchup'), 1000)

    # TODO Test consensus subscription when it is implemented on both fe
    # * json subscribe_consensus = backend.takeTimeout(withMethod('subscribe'), 1000)
    # * json catchup_consensus = backend.takeTimeout(withMethod('catchup'), 1000)

    Then match create_lao contains deep { method: 'publish', params: { channel: '/root' }}
    Then match subscribe contains deep { method: 'subscribe' }
    Then match catchup contains deep { method: 'catchup' }
