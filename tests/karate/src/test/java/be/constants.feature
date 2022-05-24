@ignore @report=false
Feature: Constants
  Scenario: Creates constants that will be used by other features
    # TODO: make the function depend on all the attributes the lao id depends on
    * def organizerPk =
      """
        function(){ return "J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM=" }
      """
    * def attendeePk =
      """
        function(){
          return "M5ZychEi5rwm22FjwjNuljL1qMJWD2sE7oX9fcHNMDU="
        }
      """

    * def getOrganizer = call organizerPk
    * def getAttendee = call attendeePk

    # This creation time is arbitrary and corresponds the starting point
    # of all events that will occur inside a lao all upcoming messages
    # need to have a creation time larger than this one
    # TODO: be changed if backend starts rejecting messages that are too far in the past or too far in the future
    * def laoValidCreationTime = function(){ return 1633035721 }
    * def laoInvalidCreationTime = function(){ return -1633035721 }

    # Represents the starting point a valid roll call and should be created after the lao
    * def rollCallValidCreationTime = function()   { return 1633098853 }
    # Creating a Roll Call before a LAO was created should be considered invalid
    * def rollCallInvalidCreationTime = function() { return 1630000000 }
    # Creation time of open Roll Call should be after the creation time of Roll Call Create
    * def rollCallOpenValidCreationTime = function(){ return rollCallValidCreationTime() + 100}
    # Creation time of close Roll Call should be after the creation time of Roll Call Open
    * def rollCallCloseValidCreationTime = function(){ return rollCallOpenValidCreationTime() + 100}

    * def laoValidName = function(){ return "LAO"}
    * def laoInvalidName = function(){ return ""}
    * def rollCallValidName = function() {return "Roll Call "}

    * def getLaoValidName = call laoValidName
    * def getLaoInvalidName = call laoValidName
    * def getRolCallValidName = call rollCallValidName

    * def getLaoValidCreationTime = call laoValidCreationTime
    * def getLaoInvalidCreationTime = call laoInvalidCreationTime
    * def getRollCallValidCreationTime = call rollCallValidCreationTime
    * def getRollCallInvalidCreationTime = call rollCallInvalidCreationTime
    * def getRollCallOpenValidCreationTime = call rollCallOpenValidCreationTime
    * def getRollCallCloseValidCreationTime = call rollCallCloseValidCreationTime

    * def constructLaoId =
       """
          function(laoName, time){
            var JsonConverter = Java.type('be.utils.JsonConverter')
            var String = Java.type('java.lang.String')
            var jsonConverter = new JsonConverter()
            var organizer = getOrganizer
            var timeString = String.format("%d",time)
            return jsonConverter.hash(organizer.getBytes(), timeString.getBytes(), laoName.getBytes())
          }
       """
    * def constructRollCallId =
      """
        function(laoId, rollCallName, time){
          var JsonConverter = Java.type('be.utils.JsonConverter')
          var jsonConverter = new JsonConverter()
          var String = Java.type('java.lang.String')
          var timeString = String.format("%d",time)
          return jsonConverter.hash("R".getBytes(), laoId.getBytes(), timeString.getBytes(), rollCallName.getBytes())
        }
      """
    * def createValidRollCallOpenId =
      """
        function(){
          return "VSsRrcHoOTQJ-nU_VT_FakiMkezZA86z2UHNZKCxbN8="
        }
      """
    * def constructRollCallUpdateId =
      """
        function(laoId, update, creationTime){
          var JsonConverter = Java.type('be.utils.JsonConverter')
          var jsonConverter = new JsonConverter()
          var String = Java.type('java.lang.String')
          var timeString = String.format("%d", creationTime)
          return jsonConverter.hash("R".getBytes(), laoId.getBytes(), update.getBytes(), timeString.getBytes())
        }
      """
    * def createInvalidRollCallOpenUpdateId =
      """
        function(){
          return "krCHh6OFWIjSHQiUSrWyx1FV0Jp8deC3zUyelhPG-Yk="
        }
      """
    * def createValidRollCallCloseId =
      """
        function(){
          return "N9DNfliEA9lrcDNAnw5PXjOS84kbq2fLFz8GzIxzCwU="
        }
      """
    * def createInvalidRollCallCloseUpdateId =
      """
        function(){
          return "lM5Lntpk4Y4SpKjzV2ICYpe4YnMOvWz1eeREB_RVVRg="
        }
      """
    * def createValidElectionSetupId =
      """
        function(){
          return "rdv-0minecREM9XidNxnQotO7nxtVVnx-Zkmfm7hm2w="
        }
      """
    * def createInvalidElectionSetupId =
      """
        function(){
          return "zG1olgFZwA0m3mLyUqeOqrG0MbjtfqShkyZ6hlyx1tg="
        }
      """
    * def createIsThisProjectFunQuestionId =
      """
        function(){
          return "3iPxJkdUiCgBd0c699KA9tU5U0zNIFau6spXs5Kw6Pg="
        }
      """
    * def createInvalidQuestionId =
      """
        function(){
          return "2PLwVvqxMqW5hQJXkFpNCvBI9MZwuN8rf66V1hS-iZU="
        }
      """
    * def createIsThisProjectFunVoteId =
      """
        function(){
          return "d60B94lVWm84lBHc9RE5H67oH-Ad3O1WFflK3NSY3Yk="
        }
      """
    * def createInvalidVoteId =
      """
        function(){
          return "sa3fa23saPso4mas5493fn290sasGl8asm3qalDFGsn42="
        }
      """
    * def createValidRegisteredVotes =
      """
        function(){
          return "fgz7BlGQVRu_yk69ijz3eTnJEmRIMoode_-nI6abK70="
        }
      """
    * def createInvalidRegisteredVotes =
      """
        function(){
          return "nas8r4aF0wq9ad4isfp4nsfiMFPMPS9sdsF8lsd8sopfd0="
        }
      """
    * def getLaoValid = constructLaoId(getLaoValidName, getLaoValidCreationTime)
    * def getLaoIdNegativeTime = constructLaoId(getLaoValidName, getLaoInvalidCreationTime)
    * def getLaoIdEmptyName = constructLaoId(getLaoValidName, getLaoValidCreationTime)

    * def getRollCallValidId = constructRollCallId(getLaoValid, getRolCallValidName, getRollCallValidCreationTime)
    * def getRollCallInvalidId = constructRollCallId(getLaoValid, getLaoValidName,  getRollCallInvalidCreationTime)

    * def getRollCallOpenValidId = call createValidRollCallOpenId
    * def getRollCallOpenValidUpdateId = constructRollCallUpdateId(getLaoValid, getRollCallOpenValidId, getRollCallOpenValidCreationTime)
    * def getRollCallOpenInvalidUpdateId = call createInvalidRollCallOpenUpdateId

    * def getRollCallCloseValidId = call createValidRollCallCloseId
    * def getRollCallCloseValidUpdateId = constructRollCallUpdateId(getLaoValid, getRollCallCloseValidId, getRollCallCloseValidCreationTime)
    * def getRollCallCloseInvalidUpdateId = call createInvalidRollCallCloseUpdateId

    * def getValidElectionSetupId = call createValidElectionSetupId
    * def getInvalidElectionSetupId = call createInvalidElectionSetupId
    * def getIsThisProjectFunQuestionId = call createIsThisProjectFunQuestionId
    * def getInvalidQuestionId = call createInvalidQuestionId
    * def getIsThisProjectFunVoteId = call createIsThisProjectFunVoteId
    * def getInvalidVoteId = call createInvalidVoteId
    * def getValidRegisteredVotes = call createValidRegisteredVotes
    * def getInvalidRegisteredVotes = call createInvalidRegisteredVotes

    * def INVALID_ACTION =          {error: {code: -1, description: '#string'}}
    * def INVALID_RESOURCE =        {error: {code: -2, description: '#string'}}
    * def RESOURCE_ALREADY_EXISTS = {error: {code: -3, description: '#string'}}
    * def INVALID_MESSAGE_FIELD =   {error: {code: -4, description: '#string'}}
    * def ACCESS_DENIED =           {error: {code: -5, description: '#string'}}
    * def INTERNAL_SERVER_ERROR =   {error: {code: -6, description: '#string'}}
    * def VALID_MESSAGE =           {result: 0}
    * def ELECTION_RESULTS =        {"object": "election", "action": "result"}
