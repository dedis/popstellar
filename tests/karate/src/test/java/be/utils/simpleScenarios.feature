@ignore @report=false
  Feature: Create valid simple Scenarios

    Background:
    # This file contains a set of simple scenarios that can be used when
    # testing the validity of other features. By calling one scenario from
    # this file simply use the allocated name for the particular feature.
      * call read('classpath:be/mockClient.feature')
      * call read('classpath:be/constants.feature')

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
      * karate.log("sending a lao create request :\n", karate.pretty(laoCreateRequest))
      * organizer.publish(laoCreateRequest, rootChannel)
      * json answer = organizer.getBackendResponse(laoCreateRequest)

      And def subscribe =
        """
          {
            "method": "subscribe",
            "id": 2,
            "params": {
                "channel": '#(lao.channel)',
            },
            "jsonrpc": "2.0"
          }
        """
      * karate.log("sending a subscribe to lao channel:\n", karate.pretty(subscribe))
      * organizer.send(subscribe)
      * def subs = organizer.takeTimeout(timeout)

      And def catchup =
        """
          {
            "method": "catchup",
            "id": 5,
            "params": {
                "channel": '#(lao.channel)',
              },
            "jsonrpc": "2.0"
          }
        """
      * karate.log("sending a catchup to lao channel:\n", karate.pretty(catchup))
      * organizer.send(catchup)
      * def catchup_response = organizer.takeTimeout(timeout)

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
      * karate.log("sending a roll call create request :\n", karate.pretty(validCreateRollCall))
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
      * karate.log("sending a roll call open request :\n", karate.pretty(validOpenRollCall))
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
      * karate.log("sending a roll call close request :\n", karate.pretty(validRollCallClose))
      * organizer.publish(validRollCallClose, lao.channel)
      * json answer = organizer.getBackendResponse(validRollCallClose)

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
      * karate.log("sending an election setup request :\n", karate.pretty(validElectionSetup))
      When organizer.publish(validElectionSetup, lao.channel)
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
      * karate.log("sending a subscribe to election channel :\n", karate.pretty(subscribe))
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
      * karate.log("sending a catchup to election channel :\n", karate.pretty(catchup))
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
      * karate.log("sending an election open request :\n", karate.pretty(validElectionOpen))
      * organizer.publish(validElectionOpen, election.channel)
      * json answer = organizer.getBackendResponse(validElectionOpen)

    # organizer, lao, rollCall, election and the question need to be passed as arguments when calling this scenario
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
      * karate.log("sending a cast vote :\n", karate.pretty(validCastVote))
      * organizer.publish(validCastVote, election.channel)
      * json answer = organizer.getBackendResponse(validCastVote)

    # organizer, lao and rollCall need to be passed as arguments when calling this scenario
    @name=setup_coin_channel
    Scenario: Sets up the coin channel and subscribes to it
      * call read('classpath:be/utils/simpleScenarios.feature@name=close_roll_call') { organizer: '#(organizer)', lao: '#(lao)', rollCall: '#(rollCall)' }
      Given def subscribe =
        """
          {
            "method": "subscribe",
            "id": 233,
            "params": {
                "channel": '#(lao.cashChannel)',
            },
            "jsonrpc": "2.0"
          }
        """
      * karate.log("sending a subscribe to coin channel :\n", karate.pretty(subscribe))
      * organizer.send(subscribe)
      * def subs = organizer.takeTimeout(timeout)

      And def catchup =
        """
          {
            "method": "catchup",
            "id": 533,
            "params": {
                "channel": '#(lao.cashChannel)',
            },
            "jsonrpc": "2.0"
          }
        """
      * karate.log("sending a catchup to coin channel :\n", karate.pretty(catchup))
      * organizer.send(catchup)
      * def catchup_response = organizer.takeTimeout(timeout)

    # organizer, lao, rollCall, recipient and amount need to be passed as arguments when calling this scenario
    @name=valid_coin_issuance
    Scenario: Issues a certain amount of coins to an attendee
      * call read('classpath:be/utils/simpleScenarios.feature@name=setup_coin_channel') { organizer: '#(organizer)', lao: '#(lao)', rollCall: '#(rollCall)' }
      * def transaction = organizer.issueCoins(recipient, amount);
      * def postTransaction = transaction.post()
      * def input = transaction.inputs[0]
      * def output = transaction.outputs[0]
      * def validTransaction =
        """
          {
              "object": "coin",
              "action": "post_transaction",
              "transaction_id": '#(postTransaction.transactionId)',
              "transaction": {
                "version": '#(transaction.version)',
                "inputs": [{
                  "tx_out_hash": '#(input.txOutHash)',
                  "tx_out_index": '#(input.txOutIndex)',
                  "script": {
                    "type": '#(input.script.type)',
                    "pubkey": '#(input.script.pubKeyRecipient)',
                    "sig": '#(input.script.sig)'
                  }
                }],
                "outputs": [{
                  "value": '#(output.value)',
                  "script": {
                    "type": '#(output.script.type)',
                    "pubkey_hash": '#(output.script.pubKeyHash)',
                  }
                }],
                "lock_time": '#(transaction.lockTime)',
              }
          }
        """
      * karate.log("sending a transaction to issue coins :\n", karate.pretty(validTransaction))
      * organizer.publish(validTransaction, lao.cashChannel)
      * json answer = organizer.getBackendResponse(validTransaction)
