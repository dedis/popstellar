Feature: LAO
  Background:
    * call read('classpath:fe/utils/constants.feature')
    * call read(MOCK_CLIENT_FEATURE)

  @name=lao_create
  Scenario: Create a new LAO
    Given call read(PLATFORM_FEATURE) { name: '#(CREATE_LAO)', params: { organization_name: 'My test organization' } }
    When waitFor(event_create_button)
    Then screenshot()

  @name=lao_join
  Scenario: Manually connect to an existing LAO
    Given def organizer = createMockClient()
    And def lao = organizer.createLao()
    When call read(PLATFORM_FEATURE) { name: '#(JOIN_LAO)', params: { lao: '#(lao)' } }
    Then assert !exists(event_create_button)
    And screenshot()
