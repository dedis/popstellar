Feature: LAO
  Background:
    Given call read('classpath:fe/utils/mock_client.feature')


  @name=lao_create
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

  @name=lao_join
  Scenario: Manually connect to an existing LAO
    Given def organizer = createMockClient()
    And def lao = organizer.createLao()
    When call read('classpath:fe/utils/platform.feature') { name: 'lao_join', params: { lao: '#(lao)' } }
    Then assert !exists(event_create_button)
    And screenshot()
