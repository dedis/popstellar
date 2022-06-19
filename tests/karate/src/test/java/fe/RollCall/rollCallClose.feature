Feature:

  Scenario: Closing a roll call without attendees include only organizer
    # Do all the steps until (and included) opening a roll call
    * call read('classpath:fe/utils/simpleScenarios.feature@name=open_roll_call')

    # Close the opened roll-call
    * def rc_page_object = 'classpath:fe/utils/<env>.feature@name=close_roll_call'
    * replace rc_page_object.env = karate.env
    * backend.clearBuffer()
    And call read(rc_page_object)

#    * backend.setRollCallCreateMode()
#    # Unused, just needed to clear the buffer when the create message arrives
#    * json create_msg = buffer.takeTimeout(timeout)
#    * print create_msg
#    * backend.clearBuffer()

    # Retrieving sent messages
    * json close_rc_json = buffer.takeTimeout(timeout)
    * print close_rc_json
    * string close_rc_string = close_rc_json

    Then match close_rc_json contains deep { method: 'publish'}
    And match backend.checkPublishMessage(close_rc_string) == true
    And match backend.checkRollCallCloseMessage(close_rc_string) == true
    And match backend.receiveNoMoreResponses() == true


#  Scenario: RC close several attendees
#    * string token1 = "asfda"
#    * string token2
#    * string token3
