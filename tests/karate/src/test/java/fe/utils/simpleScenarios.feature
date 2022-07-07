@env=android,web
Feature: Simple Scenarios

  @name=basic_setup
  Scenario: Basic setup
    * def page_object = 'classpath:fe/utils/<env>.feature@name=basic_setup'
    * replace page_object.env = karate.env
    * call read(page_object)

  @name=create_lao
  Scenario: Create a LAO send the right messages to the backend
    Given call read('classpath:fe/utils/simpleScenarios.feature@name=basic_setup')
    * backend.setLaoCreateMode()
    * input(tab_launch_lao_name_selector, 'Lao Name')
    And click(tab_launch_create_lao_selector)

  @name=create_roll_call
  Scenario: Create a roll-call and everything needed before
    Given call read('classpath:fe/utils/simpleScenarios.feature@name=create_lao')
    * def rc_page_object = 'classpath:fe/utils/<env>.feature@name=create_roll_call'
    * replace rc_page_object.env = karate.env
    * call read(rc_page_object)
    * backend.clearBuffer()
    * backend.setValidBroadcastMode()
    And click(roll_call_confirm_selector)

  @name=open_roll_call
  Scenario: Open a roll call
  Given call read('classpath:fe/utils/simpleScenarios.feature@name=create_roll_call')
  * def rc_page_object = 'classpath:fe/utils/<env>.feature@name=open_roll_call'
  * replace rc_page_object.env = karate.env
  * call read(rc_page_object)

  @name=close_roll_call
  Scenario: Close a roll-call
    Given call read('classpath:fe/utils/simpleScenarios.feature@name=open_roll_call')
    * def rc_page_object = 'classpath:fe/utils/<env>.feature@name=close_roll_call'
    * replace rc_page_object.env = karate.env
    * call read(rc_page_object)

  @name=setup_election
  Scenario: Setup an election
    Given call read('classpath:fe/utils/simpleScenarios.feature@name=close_roll_call')
    * def election_page_object = 'classpath:fe/utils/<env>.feature@name=setup_election'
    * replace election_page_object.env = karate.env
    * call read(election_page_object)

  @name=open_election
  Scenario: Setup an election
    Given call read('classpath:fe/utils/simpleScenarios.feature@name=setup_election')
    * def election_page_object = 'classpath:fe/utils/<env>.feature@name=open_election'
    * replace election_page_object.env = karate.env
    * call read(election_page_object)

  @name=cast_vote
  Scenario: Setup an election
    Given call read('classpath:fe/utils/simpleScenarios.feature@name=open_election')
    * def election_page_object = 'classpath:fe/utils/<env>.feature@name=cast_vote'
    * replace election_page_object.env = karate.env
    * call read(election_page_object)