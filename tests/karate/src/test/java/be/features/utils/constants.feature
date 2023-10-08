@ignore @report=false
Feature: Constants
  Scenario: Creates constants that will be used by other features
    * def INVALID_ACTION =          {error: {code: -1, description: '#string'}}
    * def INVALID_RESOURCE =        {error: {code: -2, description: '#string'}}
    * def RESOURCE_ALREADY_EXISTS = {error: {code: -3, description: '#string'}}
    * def INVALID_MESSAGE_FIELD =   {error: {code: -4, description: '#string'}}
    * def ACCESS_DENIED =           {error: {code: -5, description: '#string'}}
    * def INTERNAL_SERVER_ERROR =   {error: {code: -6, description: '#string'}}
    * def VALID_MESSAGE =           {result: 0}
    * def ELECTION_RESULTS =        {"object": "election", "action": "result"}

    * def rootChannel = '/root'
    * def random = Java.type('be.utils.RandomUtils')
