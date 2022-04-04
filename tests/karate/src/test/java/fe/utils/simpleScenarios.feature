@env=android,web

Feature: create valid simple Scenarios
   # This file contains a set of simple scenarios that can be used when
   # testing the validity of other features. By calling one scenario from
   # this file simply use the allocated name for the particular feature.

  @name=create_valid_lao
  Scenario: Create valid lao
    * def page_object = 'classpath:fe/utils/<env>.feature@name=basic_setup'
    * replace page_object.env = karate.env
    * call read(page_object)
    * click(tab_launch_selector)
    * input(tab_launch_lao_name_selector, 'Lao Name')
    * click(tab_launch_create_lao_selector)
    * below(tab_home_selector).click()