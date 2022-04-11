@env=android,web
Feature: Create a Roll Call
  Background:
    * call read('classpath:fe/utils/simpleScenarios.feature@name=valid_lao_creation')

  Scenario: Create a Roll-call
    Given click(add_event_selector)
    And click('{}Roll-Call Event')