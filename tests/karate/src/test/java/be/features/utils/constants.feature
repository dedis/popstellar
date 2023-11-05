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

    # Paths to util features
    * def utilsPath = 'classpath:be/features/utils/'
    * def serverFeature = utilsPath + 'server.feature'
    * def mockClientFeature = utilsPath + 'mockClient.feature'

    # Paths to scenarios defined in simpleScenarios
    * def simpleScenario = 'classpath:be/features/utils/simpleScenarios.feature@name='
    * def createLaoScenario = simpleScenario + 'create_lao'
    * def createRollCallScenario = simpleScenario + 'create_roll_call'
    * def openRollCallScenario = simpleScenario + 'open_roll_call'
    * def closeRollCallScenario = simpleScenario + 'close_roll_call'
    * def setupElectionScenario = simpleScenario + 'election_setup'
    * def openElectionScenario = simpleScenario + 'election_open'
    * def castVoteScenario = simpleScenario + 'cast_vote'
    * def setupCoinChannelScenario = simpleScenario + 'setup_coin_channel'
    * def validCoinIssuanceScenario = simpleScenario + 'valid_coin_issuance'
    * def greetServerScenario = simpleScenario + 'greet_server'
