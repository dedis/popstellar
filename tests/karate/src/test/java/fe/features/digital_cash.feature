Feature:
  Background:
    * call read('classpath:fe/utils/constants.feature')
    * call read(MOCK_CLIENT_FEATURE)

  @name=digital_cash_coin_issuance_base
  Scenario:
    Given def organizer = createMockClient()
    And def lao = organizer.createLao()
    And call read(PLATFORM_FEATURE) { name: '#(JOIN_ROLLCALL)', params: { lao: '#(lao)', organizer: '#(organizer)' } }
    When call read(PLATFORM_FEATURE) { name: '#(SWITCH_TO_DIGITAL_CASH_PAGE)' }
    Then exists(digital_cash_coin_issuance_button)
    And exists(digital_cash_first_roll_call_button)
