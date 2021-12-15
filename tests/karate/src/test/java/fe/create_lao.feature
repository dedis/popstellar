@env=android,web
Feature: Create LAO

  Background: Driver setup
    * def page_object = 'classpath:fe/utils/<env>.feature'
    * replace page_object.env = karate.env
    * call read(page_object)

  Scenario: Create LAO
    When click(tab_launch_selector)
    And fill(tab_launch_lao_name_selector, 'Lao Name')
    And click(tab_launch_create_lao_selector)

    * json create_lao = backend.takeTimeout(1000000)
    * json subscribe = backend.takeTimeout(withMethod('subscribe'), 1000)
    * json catchup = backend.takeTimeout(withMethod('catchup'), 1000)

    # TODO Test consensus subscription when it is implemented on both fe
    # * json subscribe_consensus = backend.takeTimeout(withMethod('subscribe'), 1000)
    # * json catchup_consensus = backend.takeTimeout(withMethod('catchup'), 1000)

    Then match create_lao contains deep { method: 'publish', params: { channel: '/root' }}
    Then match subscribe contains deep { method: 'subscribe' }
    Then match catchup contains deep { method: 'catchup' }
