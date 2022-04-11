@ignore report=false
  Feature: Creates valid simple Scenarios

    @name=valid_lao_creation
    Scenario: Creates and enter a valid lao
      * def page_object = 'classpath:fe/utils/<env>.feature@name=basic_setup'
      * replace page_object.env = karate.env
      * call read(page_object)
      * click(tab_launch_selector)
      * input(tab_launch_lao_name_selector, 'Lao Name')
      * click(tab_launch_create_lao_selector)
      * click(lao_list_2)
      * click('{}Lao Name')