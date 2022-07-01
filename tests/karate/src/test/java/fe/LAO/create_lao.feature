@env=android,web
Feature: Create LAO

  Background: Driver basic setup
    # This page object will add view selectors variables to the current scope, start the app and setup mock backend
    # The is a basic setup (selected by its name tag): it will start on the home page being connected to the backend
    # More info on tag selection: https://github.com/karatelabs/karate#call-tag-selector
    * def page_object = 'classpath:fe/utils/<env>.feature@name=basic_setup'
    * replace page_object.env = karate.env
    * call read(page_object)

  Scenario: Create a LAO send the right messages to the backend
    When backend.setLaoCreateMode()
    And input(tab_launch_lao_name_selector, 'Lao Name')
    And click(tab_launch_create_lao_selector)

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