Feature:

  Scenario:
     # Do all the steps up until (and including) closing a roll call
    * call read('classpath:fe/utils/simpleScenarios.feature@name=close_roll_call')

    # Reopen the closed roll-call
    * def rc_page_object = 'classpath:fe/utils/<env>.feature@name=reopen_roll_call'
    * replace rc_page_object.env = karate.env
    * backend.clearBuffer()
    And call read(rc_page_object)

    # Retrieving sent messages
    * json reopen_rc_json = buffer.takeTimeout(10000)
    * print reopen_rc_json
    * string reopen_rc_string = reopen_rc_json

    Then match reopen_rc_json contains deep { method: 'publish'}
    And match backend.checkPublishMessage(reopen_rc_string) == true
    And match backend.checkRollCallReopenMessage(reopen_rc_string) == true
    And match backend.receiveNoMoreResponses() == true