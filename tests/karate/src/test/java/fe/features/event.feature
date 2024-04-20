Feature: Event
  Background:
    * call read('classpath:fe/utils/constants.feature')
    * call read(MOCK_CLIENT_FEATURE)

  @name=event_create_rollcall
  Scenario:
    Given call read(PLATFORM_FEATURE) { name: '#(CREATE_LAO)', params: { organization_name: 'Create Roll-Call Org' } }
    And def rollCallName = 'My Roll-Call'
    When waitFor(event_create_button).click()
    And actionSheetClick(event_create_rollcall)
    And waitFor(event_rollcall_name_input).input(rollCallName)
    And waitFor(event_rollcall_location_input).input('Between 1 and 0s')
    And waitFor(event_rollcall_confirm_button).click()
    Then waitForText(event_first_current_event, rollCallName)
    And screenshot()

  @name=event_join_rollcall
  Scenario:
    Given def organizer = createMockClient()
    And def lao = organizer.createLao()
    And def rollCall = organizer.createRollCall(lao)
    And organizer.openRollCall(lao, rollCall)
    And call read(PLATFORM_FEATURE) { name: '#(JOIN_LAO)', params: { lao: '#(lao)' } }
    When waitFor(event_first_current_event).click()
    And waitFor(event_rollcall_pop_token)
    And delay(1000)
    And def popToken = text(event_rollcall_pop_token)
    And organizer.closeRollCall(lao, rollCall, [popToken])
    Then waitForText(event_rollcall_first_attendee, popToken)
    And screenshot()
