Feature: LAO
  Background:
    * def mock = call read('classpath:fe/utils/mock_client.feature')
    Given call read('classpath:fe/utils/platform.feature') { name: 'create_new_wallet' }


  @name=lao_create
  Scenario: Create a new LAO
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
    * def organizer = mock.create_client()
    * def lao = organizer.createValidLao()
    * mock.create_lao(organizer, lao)
    * waitFor(lao_join_button).click()
    * waitFor(lao_enter_manually_button).click()
    And waitFor(lao_enter_manually_server_input).clear().input(serverURL)
    And waitFor(lao_enter_manually_lao_input).clear().input(lao.id)
    And screenshot()
    And click(lao_enter_manually_submit_button)
    Then waitFor(event_create_button)
    * organizer.stop()
    Then screenshot()
