Feature: Social Media
  Background:
    * call read('classpath:fe/utils/constants.feature')
    * call read(MOCK_CLIENT_FEATURE)
    * def organizer = createMockClient()
    * def lao = organizer.createLao()

  @name=social_open_without_pop_token
  Scenario: Open social media page without pop token
    # Delay are added to give time for the menu to open/close
    * call read(PLATFORM_FEATURE) { name: '#(JOIN_LAO)', params: { lao: '#(lao)' } }
    Given waitFor(drawer_menu_button).click()
    And delay(500)
    When click(drawer_menu_social)
    Then waitFor(social_home_page)
    And delay(500)
    And screenshot()

  @name=social_send_message
  Scenario: Open social media page with pop token
    Given call read(PLATFORM_FEATURE) { name: '#(JOIN_ROLLCALL)', params: { lao: '#(lao)', organizer: '#(organizer)' } }
    And waitFor(drawer_menu_button).click()
    And delay(500)
    And click(drawer_menu_social)
    And waitFor(social_home_page)
    And delay(500)
    When def social_chirp_message = 'Hello from the test'
    And waitFor(social_chirp_input).input(social_chirp_message)
    And screenshot()
    And click(social_chirp_publish_button)
    And delay(500)
    Then waitFor('{}' + social_chirp_message)
    And screenshot()




