Feature: LAO
  Scenario: Create a new LAO
    Given call read('classpath:fe/utils/platform.feature') { name: 'create_new_wallet' }
    And def organization_name = 'My test organization'
    When waitFor(lao_create_button).click()
    And waitFor(lao_organization_name_input).input(organization_name)
    And waitFor(lao_server_url_input).clear().input(serverURL)
    And screenshot()
    And click(lao_launch_button)
    Then waitFor(event_create_button)
    Then screenshot()
