Feature: Wallet

  Scenario: Create a new wallet
    * def page_object = 'classpath:fe/utils/<env>.feature@name=basic_setup'
    * replace page_object.env = karate.env
    * call read(page_object)
    * delay(1000).screenshot()





