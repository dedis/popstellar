Feature: Simple hello world test 

Background: 
    * def HelloWorld = Java.type("examples.hello.HelloWorld")

Scenario: print a hello world to the console 
    * print "Hello world"


Scenario: greet a null user 
 
    * def user = null
    * def greeting = HelloWorld.greet(user)
    * print greeting
    * assert greeting == 'Who are you !'

Scenario: greet a null user BDD way
    Given  def user = null
    When   def greeting = HelloWorld.greet(user)
    Then   print greeting
    And    assert greeting == 'Who are you !'

Scenario: greet a correct user BDD way
    Given   def user = "PoP"
    And     def greeting = HelloWorld.greet(user)
    Then    print greeting
    And     assert greeting == "Hello PoP !"