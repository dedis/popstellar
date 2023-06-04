@ignore @report=false
  Feature: Create valid simple Scenarios

    Background:
    # This file contains a set of simple scenarios that can be used when
    # testing the validity of other features. By calling one scenario from
    # this file simply use the allocated name for the particular feature.
      * call read('classpath:be/mockClient.feature')
      * call read('classpath:be/constants.feature')
      * def laoCreateId = 1
      * def rollCallCreateId = 3
      * def openRollCallId = 32
      * def closeRollCallId = 33
      * def electionSetupId = 4
      * def castVoteId = 41
      * def frontend = call createMockClient

    # organizer and lao need to be passed as arguments when calling this scenario
    @name=valid_lao
    Scenario: Creates valid lao
      Given def laoCreateRequest =
      """
        {
          "object": "lao",
          "action": "create",
          "id": '#(lao.id)',
          "name": '#(lao.name)',
          "creation": '#(lao.creation)',
          "organizer": '#(lao.organizerPk)',
          "witnesses": "#(lao.witnesses)"
        }
      """
      * karate.log("sending a lao create request : ", karate.pretty(laoCreateRequest))
      * organizer.publish(laoCreateRequest, rootChannel)
      * json answer = organizer.getBackendResponse(laoCreateRequest)
      * karate.log("Received an answer for lao create request : ", karate.pretty(answer))
      * string laoChannel = rootChannel + '/' + lao.id

      And def subscribe =
        """
          {
            "method": "subscribe",
            "id": 2,
            "params": {
                "channel": '#(laoChannel)',
            },
            "jsonrpc": "2.0"
          }
        """
      * karate.log("sending a subscribe to lao channel: ", karate.pretty(subscribe))
      * organizer.send(subscribe)
      * def subs = organizer.takeTimeout(timeout)
      * karate.log("subscribe message received : " + subs)

      And def catchup =
        """
          {
            "method": "catchup",
            "id": 5,
            "params": {
                "channel": '#(laoChannel)',
              },
            "jsonrpc": "2.0"
          }
        """
      * karate.log("sending a catchup to lao channel: ", karate.pretty(catchup))
      * organizer.send(catchup)
      * def catchup_response = organizer.takeTimeout(timeout)
      * karate.log("catchup message received : " + catchup_response)

    # organizer, lao and rollCall need to be passed as arguments when calling this scenario
    @name=valid_roll_call
    Scenario: Creates a valid Roll Call
      * call read('classpath:be/utils/simpleScenarios.feature@name=valid_lao') { organizer: '#(organizer)', lao: '#(lao)' }
      * def validCreateRollCall =
         """
           {
            "object": "roll_call",
            "action": "create",
            "id": '#(rollCall.id)',
            "name": '#(rollCall.name)',
            "creation": '#(rollCall.creation)',
            "proposed_start": '#(rollCall.start)',
            "proposed_end": '#(rollCall.end)',
            "location": '#(rollCall.location)',
            "description": '#(rollCall.description)',
           }
         """
      * karate.log("sending a roll call create request : ", karate.pretty(validCreateRollCall))
      * organizer.publish(validCreateRollCall, lao.channel)
      * json answer = organizer.getBackendResponse(validCreateRollCall)

    # organizer, lao and rollCall need to be passed as arguments when calling this scenario
    @name=open_roll_call
    Scenario: Opens a valid Roll Call
      * call read('classpath:be/utils/simpleScenarios.feature@name=valid_roll_call') { organizer: '#(organizer)', lao: '#(lao)', rollCall: '#(rollCall)' }
      * def openRollCall = rollCall.open()
      * def validOpenRollCall =
        """
          {
            "object": "roll_call",
            "action": "open",
            "update_id": '#(openRollCall.updateId)',
            "opens": '#(openRollCall.opens)',
            "opened_at": '#(openRollCall.openedAt)'
          }
        """
      * karate.log("sending a roll call open request : ", karate.pretty(validOpenRollCall))
      * organizer.publish(validOpenRollCall, lao.channel)
      * json answer = organizer.getBackendResponse(validOpenRollCall)

    # organizer, lao and rollCall need to be passed as arguments when calling this scenario
    @name=close_roll_call
    Scenario: Closes a valid Roll Call
      * call read('classpath:be/utils/simpleScenarios.feature@name=open_roll_call') { organizer: '#(organizer)', lao: '#(lao)', rollCall: '#(rollCall)' }
      * def closeRollCall = rollCall.close()
      * def validRollCallClose =
        """
          {
            "object": "roll_call",
            "action": "close",
            "update_id": '#(closeRollCall.updateId)',
            "closes": '#(closeRollCall.closes)',
            "closed_at": '#(closeRollCall.closedAt)',
            "attendees": '#(closeRollCall.attendees)'
          }
      """
      * karate.log("sending a roll call close request : ", karate.pretty(validRollCallClose))
      * organizer.publish(validRollCallClose, lao.channel)
      * json answer = organizer.getBackendResponse(validRollCallClose)
      * karate.log("received an answer to the roll call close : ", karate.pretty(answer))

    # organizer, lao, rollCall, election and the question need to be passed as arguments when calling this scenario
    @name=election_setup
    Scenario: Sets up a valid election with one question
      * call read('classpath:be/utils/simpleScenarios.feature@name=close_roll_call') { organizer: '#(organizer)', lao: '#(lao)', rollCall: '#(rollCall)' }
      Given def validElectionSetup =
      """
        {
          "object": "election",
          "action": "setup",
          "id": '#(election.id)',
          "lao": '#(lao.id)',
          "name": '#(election.name)',
          "version": '#(election.version)',
          "created_at": '#(election.creation)',
          "start_time": '#(election.start)',
          "end_time": '#(election.end)',
          "questions": [
            {
              "id": '#(question.id)',
              "question": '#(question.question)',
              "voting_method": '#(question.votingMethod)',
              "ballot_options": '#(question.ballotOptions)',
              "write_in": '#(question.writeIn)'
            }
          ]
        }
      """
      * karate.log("sending an election setup request : ", karate.pretty(validElectionSetup))
      When organizer.publish(validElectionSetup, laoChannel)
      And json answer = organizer.getBackendResponse(validElectionSetup)

      And def subscribe =
        """
          {
            "method": "subscribe",
            "id": 200,
            "params": {
                "channel":  '#(election.channel)',
            },
            "jsonrpc": "2.0"
          }
        """
      * karate.log("sending a subscribe to election channel : ", karate.pretty(subscribe))
      * organizer.send(subscribe)
      * def subs = organizer.takeTimeout(timeout)

      And def catchup =
        """
          {
            "method": "catchup",
            "id": 500,
            "params": {
               "channel":  '#(election.channel)',
            },
            "jsonrpc": "2.0"
           }
        """
      * karate.log("sending a catchup to election channel : ", karate.pretty(catchup))
      * organizer.send(catchup)
      * def catchup_response = organizer.takeTimeout(timeout)

    # organizer, lao, rollCall, election and the question need to be passed as arguments when calling this scenario
    @name=election_open
    Scenario: Opens an election with one question
      * call read('classpath:be/utils/simpleScenarios.feature@name=election_setup') { organizer: '#(organizer)', lao: '#(lao)', rollCall: '#(rollCall)',  election: '#(election)', question: '#(question)' }
      * def electionOpen = election.open()
      * def validElectionOpen =
        """
          {
            "object": "election",
            "action": "open",
            "lao": '#(lao.id)',
            "election": '#(election.id)',
            "opened_at": '#(electionOpen.openedAt)'
          }
        """
      * karate.log("sending an election open request : ", karate.pretty(catchup))
      * organizer.publish(validElectionOpen, election.channel)
      * json answer = organizer.getBackendResponse(validElectionOpen)

    @name=cast_vote
    Scenario: Casts a valid vote
      * call read('classpath:be/utils/simpleScenarios.feature@name=election_open') { organizer: '#(organizer)', lao: '#(lao)', rollCall: '#(rollCall)',  election: '#(election)', question: '#(question)' }
      * def vote = question.createVote(0)
      * def castVote = election.castVote(vote)
      * def validCastVote =
        """
          {
            "object": "election",
            "action": "cast_vote",
            "lao": '#(lao.id)',
            "election": '#(election.id)',
            "created_at": '#(castVote.createdAt)',
            "votes": [
              {
                "id": '#(vote.id)',
                "question": '#(question.id)',
                "vote": '#(vote.index)'
              }
            ]
          }
        """
      * karate.log("sending a cast vote : ", karate.pretty(validCastVote))
      * organizer.publish(validCastVote, election.channel)
      * json answer = organizer.getBackendResponse(validCastVote)

    @name=setup_coin_channel
    Scenario: Sets up the coin channel and subscribes to it
      * call read('classpath:be/utils/simpleScenarios.feature@name=close_roll_call')
      * def subscribe =
        """
          {
            "method": "subscribe",
            "id": 233,
            "params": {
                "channel": "/root/p_EYbHyMv6sopI5QhEXBf40MO_eNoq7V_LygBd4c9RA=/coin",
            },
            "jsonrpc": "2.0"
          }
        """
      * frontend.send(subscribe)
      * def subs = frontend.takeTimeout(timeout)
      * karate.log("subscribe message received : " + subs)
      * def catchup =
        """
          {
            "method": "catchup",
            "id": 533,
            "params": {
                "channel": "/root/p_EYbHyMv6sopI5QhEXBf40MO_eNoq7V_LygBd4c9RA=/coin",
            },
            "jsonrpc": "2.0"
          }
        """
      * frontend.send(catchup)
      * def catchup_response = frontend.takeTimeout(timeout)

    @name=valid_coin_issuance
    Scenario: Issues a certain amount of coins to an attendee
      * call read('classpath:be/utils/simpleScenarios.feature@name=setup_coin_channel')
      * def validTransaction =
        """
          {
            "object": "coin",
            "action": "post_transaction",
            "transaction_id": "yVMgw2E9IMX7JtNfizTqTOR1scMVSHfEe8WBbiAgsA8=",
            "transaction": {
              "version": 1,
              "inputs": [{
                "tx_out_hash": "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
                "tx_out_index": 0,
                "script": {
                  "type": "P2PKH",
                  "pubkey": "J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM=",
                  "sig": "wVoIYoQFoepkosPxXK8CmnvhRmq0IUczGQR5JOJjX8R7vqrgMOdI311bgzrOIwtACMfGFTJcnryiHiOuB5Z3Dg=="
                }
              }],
              "outputs": [{
                "value": 32,
                "script": {
                  "type": "P2PKH",
                  "pubkey_hash": "-_qR4IHwsiq50raa8jURNArds54="
                }
              }],
              "lock_time": 0
            }
          }
        """
      * frontend.publish(validTransaction, cashChannel)
      * json answer = frontend.getBackendResponse(validTransaction)
