@env=android,web
Feature: Simple Scenarios

    @name=basic_setup
  Scenario: Basic setup
    * def page_object = 'classpath:fe/utils/<env>.feature@name=basic_setup'
    * replace page_object.env = karate.env
    * call read(page_object)

  @name=create_lao
  Scenario: Create a LAO send the right messages to the backend
    * call read('classpath:fe/utils/simpleScenarios.feature@name=basic_setup')
    When click(tab_launch_selector)
    * backend.setLaoCreateMode()
    And input(tab_launch_lao_name_selector, 'Lao Name')
    And click(tab_launch_create_lao_selector)