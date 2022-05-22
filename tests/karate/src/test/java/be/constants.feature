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

    * def laoValidCreationTime = function(){ return 1633035721 }
    * def laoInvalidCreationTime = function(){ return -1633035721 }
    * def rollCallValidCreationTime = function() { return 1633098853 }
    * def electionSetupCreationTime = function() { return 1633098941 }

    * def laoValidName = function(){ return "LAO"}
    * def laoInvalidName = function(){ return ""}
    * def electionSetupName = function() { return "Election" }

    * def getLaoValidName = call laoValidName
    * def getLaoInvalidName = call laoValidName
    * def getElectionSetupName = call electionSetupName

    * def getLaoValidCreationTime = call laoValidCreationTime
    * def getLaoInvalidCreationTime = call laoInvalidCreationTime
    * def getElectionSetupCreationTime = call electionSetupCreationTime
    * def rollCallValidName = function() {return "Roll Call "}

    * def getLaoValidName = call laoValidName
    * def getLaoInvalidName = call laoValidName
    * def getRolCallValidName = call rollCallValidName

    * def getLaoValidCreationTime = call laoValidCreationTime
    * def getLaoInvalidCreationTime = call laoInvalidCreationTime
    * def getRollCallValidCreationTime = call rollCallValidCreationTime

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

    * def getLaoValid = constructLaoId(getLaoValidName, getLaoValidCreationTime)
    * def getLaoIdNegativeTime = constructLaoId(getLaoValidName, getLaoInvalidCreationTime)
    * def getLaoIdEmptyName = constructLaoId(getLaoValidName, getLaoValidCreationTime)

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
        function(laoId, update, openedAt){
          var JsonConverter = Java.type('be.utils.JsonConverter')
          var jsonConverter = new JsonConverter()
          var String = Java.type('java.lang.String')
          var timeString = String.format("%d",openedAt)
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
    * def createElectionId =
      """
        function(name, time){
          var JsonConverter = Java.type('be.utils.JsonConverter')
          var jsonConverter = new JsonConverter()
          var electionConstant = "Election"
          var lao = getLaoValid
          var String = Java.type('java.lang.String')
          var timeString = String.format("%d",time)
          return jsonConverter.hash(electionConstant.getBytes(), lao.getBytes(),
                                    timeString.getBytes(), name.getBytes())
        }
      """
    * def createInvalidElectionSetupId =
      """
        function(){
          return "zG1olgFZwA0m3mLyUqeOqrG0MbjtfqShkyZ6hlyx1tg="
        }
      """
    * def getValidElectionSetupId = createElectionId(getElectionSetupName, getElectionSetupCreationTime)

    * def createIsThisProjectFunQuestionId =
      """
        function(){
          var JsonConverter = Java.type('be.utils.JsonConverter')
          var jsonConverter = new JsonConverter()
          var questionConstant = "Question"
          var electionId = getValidElectionSetupId
          var question = "Is this project fun?"
          return jsonConverter.hash(questionConstant.getBytes(), electionId.getBytes(), question.getBytes())
        }
      """
    * def createInvalidQuestionId =
      """
        function(){
          return "2PLwVvqxMqW5hQJXkFpNCvBI9MZwuN8rf66V1hS-iZU="
        }
      """
    * def getIsThisProjectFunQuestionId = call createIsThisProjectFunQuestionId
    * def createIsThisProjectFunVoteId =
      """
        function(vote){
          var JsonConverter = Java.type('be.utils.JsonConverter')
          var String = Java.type('java.lang.String')
          var jsonConverter = new JsonConverter()
          var voteConstant = "Vote"
          var electionId = getValidElectionSetupId
          var questionId = getIsThisProjectFunQuestionId
          return jsonConverter.hash(voteConstant.getBytes(), electionId.getBytes(),
                                     questionId.getBytes(), vote.getBytes())
        }
      """
    * def createIsThisProjectFunVoteIdVoteYes =
      """
        function(){
          return createIsThisProjectFunVoteId("0")
        }
      """
    * def createIsThisProjectFunVoteIdVoteNo =
      """
        function(){
          return createIsThisProjectFunVoteId("1")
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

    * def getRollCallValidId = constructRollCallId(getLaoValid, getRolCallValidName, getRollCallValidCreationTime)
    * def getRollCallInvalidId = constructRollCallId(getLaoValid, getLaoValidName, 1633098853)

    * def getRollCallOpenValidId = call createValidRollCallOpenId
    * def getRollCallOpenValidUpdateId = constructRollCallUpdateId(getLaoValid, getRollCallOpenValidId, 1633099127)
    * def getRollCallOpenInvalidUpdateId = call createInvalidRollCallOpenUpdateId

    * def getRollCallCloseValidId = call createValidRollCallCloseId
    * def getRollCallCloseValidUpdateId = constructRollCallUpdateId(getLaoValid, getRollCallCloseValidId, 1633099135)
    * def getRollCallCloseInvalidUpdateId = call createInvalidRollCallCloseUpdateId

    * def getInvalidElectionSetupId = call createInvalidElectionSetupId
    * def getInvalidQuestionId = call createInvalidQuestionId
    * def getIsThisProjectFunVoteIdVoteYes = call createIsThisProjectFunVoteIdVoteYes
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
