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
    # Creation time of Election Setup should be after the creation time of Roll Call Close
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
    # The argument "0" represents the index of the ballot option Yes
    * def createIsThisProjectFunVoteIdVoteYes =
      """
        function(){
          return createIsThisProjectFunVoteId("0")
        }
      """
    # The argument "1" represents the index of the ballot option No
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

    # In the specification there is a Pubkey that will never be used by am attendee so we use
    # it as a supreme source of digital coin distribution (will maybe change later)
    * def createIssuancePubKey = function(){ return "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=" }
    # The signature is constant since the backend will not check it either way, it is the front end's
    # job to perform is check so we can leave it as a constant value
    * def createSignatureForCoinIssuance = function() { return "CAFEBABE" }

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

    * def getInvalidElectionSetupId = call createInvalidElectionSetupId
    * def getInvalidQuestionId = call createInvalidQuestionId
    * def getIsThisProjectFunVoteIdVoteYes = call createIsThisProjectFunVoteIdVoteYes
    * def getInvalidVoteId = call createInvalidVoteId
    * def getValidRegisteredVotes = call createValidRegisteredVotes
    * def getInvalidRegisteredVotes = call createInvalidRegisteredVotes

    * def getCoinIssuancePubKey = call createIssuancePubKey
    * def getCreateSignatureForCoinIssuance = call createSignatureForCoinIssuance
    * def getValidOutputs =   [{ "value": 32, "script": { "type": "P2PKH", "pubkey_hash": "2jmj7l5rSw0yVb-vlWAYkK-YBwk=" } }]
    * def getInvalidOutputs = [{ "value": 32, "script": { "type": "P2PKH", "pubkey_hash": "47DEQpj8HBSa--TImW-5JCeuQeRkm5NMpJWZG3hSuFU=" } }]

    * def INVALID_ACTION =          {error: {code: -1, description: '#string'}}
    * def INVALID_RESOURCE =        {error: {code: -2, description: '#string'}}
    * def RESOURCE_ALREADY_EXISTS = {error: {code: -3, description: '#string'}}
    * def INVALID_MESSAGE_FIELD =   {error: {code: -4, description: '#string'}}
    * def ACCESS_DENIED =           {error: {code: -5, description: '#string'}}
    * def INTERNAL_SERVER_ERROR =   {error: {code: -6, description: '#string'}}
    * def VALID_MESSAGE =           {result: 0}
    * def ELECTION_RESULTS =        {"object": "election", "action": "result"}
