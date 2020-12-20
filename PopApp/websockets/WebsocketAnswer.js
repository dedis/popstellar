import { sign } from 'tweetnacl';
// eslint-disable-next-line import/no-cycle
import WebsocketLink from './WebsocketLink';
import {
  actions, JSON_RPC_VERSION, objects, PendingRequest, SERVER_ANSWER_FIELD_COUNT, fromString64,
  getCurrentLao, methods, hashStrings,
} from './WebsocketUtils';
import { getStore } from '../Store/configureStore';

/* eslint-disable no-underscore-dangle */

/** Maximum amount of identical queries retries */
const MAX_QUERY_RETRIES = 3;
/** Enumeration of all message object fields in a server answer */
const answerProperties = Object.freeze({
  JSONRPC: 'jsonrpc',
  RESULT: 'result',
  ERROR: 'error',
  ID: 'id',
});

/** Enumeration of all error object fields in a server answer */
const errorProperties = Object.freeze({
  CODE: 'code',
  DESCRIPTION: 'description',
});

/**
 * Handles callbacks when receiving a server answer
 * Note : message is a websocket message (!= JsonMessage)!
 */
const handleServerAnswer = (message) => {
  /** returns true iff the query with id idx exists in the pending queue */
  const _hasPendingQuery = (idx) => WebsocketLink.getPendingProperties().has(idx);

  /** returns the query with id idx from the pending queue */
  const _getPendingQuery = (idx) => WebsocketLink.getPendingProperties().get(idx);

  /** sets or modifies the query with id idx in/from the pending queue to value */
  const _setPendingQuery = (idx, value) => WebsocketLink.getPendingProperties().set(idx, value);

  /** removes the query with id idx from the pending queue */
  const _delPendingQuery = (idx) => WebsocketLink.getPendingProperties().delete(idx);

  /** handles a positive server answer */
  const _handlePositiveServerAnswer = (obj, isPropagation = false) => {
    const answer = isPropagation ? obj : _getPendingQuery(obj.id);

    // handle "callbacks" if needed
    switch (answer.requestObject) { // update_lao
      case objects.LAO:
        if (answer.requestAction === actions.CREATE) {
          // callback for a successful create LAO request
          const jsonMessage = answer.message;
          jsonMessage.params.channel = fromString64(jsonMessage.params.channel);
          jsonMessage.params.message.data = JSON.parse(
            fromString64(jsonMessage.params.message.data),
          );

          // store new LAO
          getStore().dispatch({ type: 'SET_CURRENT_LAO', value: jsonMessage });
        } else if (answer.requestAction === actions.UPDATE_PROPERTIES) {
          // callback for a successful update LAO request
          const jsonMessageData = answer.message.params.message.data;
          const updatedLao = JSON.parse(JSON.stringify(getCurrentLao())); // defensive copy

          // modify elements
          updatedLao.params.message.data.name = jsonMessageData.name;
          updatedLao.params.message.data.last_modified = jsonMessageData.last_modified;
          updatedLao.params.message.data.witnesses = jsonMessageData.witnesses;

          // store updated LAO
          getStore().dispatch({ type: 'SET_CURRENT_LAO', value: updatedLao });
        } else if (answer.requestAction === actions.STATE) {
          console.error('TODO (in WebsocketAnswer) : case (answer.requestAction === actions.STATE)');
        }
        break;

      case objects.MEETING:
        console.error('TODO (in WebsocketAnswer) : case (objects.MEETING)');
        break;

      default:
        // note: callback for the reception of a propagation message (object == objects.MESSAGE)
        // was caught sooner while checking for message correctness
        break;
    }

    if (!isPropagation) _delPendingQuery(obj.id);
  };

  /** handles a propagate message */
  const _handlePropagateMessage = (queryParams) => {
    /** returns true iff the input string is encoded in base 64 */
    const _checkBase64Strings = (...strings) => {
      // eslint-disable-next-line consistent-return
      strings.forEach((str) => {
        if (typeof str !== 'string') return false;

        try {
          fromString64(str);
        } catch (e) { return false; }
      });
      return true;
    };

    const queryMessage = queryParams.message;
    const decodedMessage = {
      data: '',
      sender: '',
      signature: '',
      message_id: '',
      witness_signatures: [],
    };

    // check message fields correctness
    if (!(Object.keys(queryMessage).length === Object.keys(decodedMessage).length
      && Object.prototype.hasOwnProperty.call(queryMessage, 'data')
      && Object.prototype.hasOwnProperty.call(queryMessage, 'sender')
      && Object.prototype.hasOwnProperty.call(queryMessage, 'signature')
      && Object.prototype.hasOwnProperty.call(queryMessage, 'message_id')
      && Object.prototype.hasOwnProperty.call(queryMessage, 'witness_signatures')
      && _checkBase64Strings(queryMessage.witness_signatures)
    )) return;

    try {
      decodedMessage.data = fromString64(queryMessage.data); // still a json string
      decodedMessage.sender = fromString64(queryMessage.sender);
      decodedMessage.signature = fromString64(queryMessage.signature);
      decodedMessage.message_id = fromString64(queryMessage.message_id);
    } catch (e) {
      return;
    }

    // TODO check that the sender is the server's key
    //

    // check that the signature is correct
    if (
      sign.open(decodedMessage.signature, decodedMessage.sender) !== decodedMessage.data
    ) return;

    // check that the message_id is correct
    if (
      decodedMessage.message_id !== hashStrings(decodedMessage.data, decodedMessage.signature)
    ) return;

    // TODO check that the witness_signatures array is correct
    //

    // check data is correct (partially)
    const dataObject = JSON.parse(decodedMessage.data);
    if (typeof dataObject !== 'object'
      || !Object.prototype.hasOwnProperty.call(dataObject, 'object')
      || typeof dataObject.object !== 'string'
      || !Object.prototype.hasOwnProperty.call(dataObject, 'action')
      || typeof dataObject.action !== 'string'
    ) return;

    const reconstructedQuery = {
      jsonrpc: JSON_RPC_VERSION,
      method: methods.PUBLISH,
      params: queryParams,
    };

    const requestObject = (function _(object) {
      switch (object) {
        case objects.LAO:
          return objects.LAO;
        case objects.MEETING:
          return objects.MEETING;
        default:
          return undefined;
      }
    }(dataObject.object));

    const requestAction = (function _(action) {
      switch (action) {
        case actions.CREATE:
          return actions.CREATE;
        case actions.UPDATE_PROPERTIES:
          return actions.UPDATE_PROPERTIES;
        case actions.STATE:
          return actions.STATE;
        case actions.WITNESS:
          return actions.WITNESS;
        default:
          return undefined;
      }
    }(dataObject.action));

    if (requestObject === undefined || requestAction === undefined) return;

    _handlePositiveServerAnswer(
      new PendingRequest(reconstructedQuery, requestObject, requestAction),
      true,
    );
  };

  const obj = JSON.parse(message.data);
  console.log('handling a new answer : ', obj);

  /* check that the answer is of a valid format (positive or negative) */

  // check that the object has exactly SERVER_ANSWER_FIELD_COUNT fields
  if (Object.keys(obj).length !== SERVER_ANSWER_FIELD_COUNT) return;

  // check that the object has both required field and that the protocol used is correct
  if (!Object.prototype.hasOwnProperty.call(obj, answerProperties.JSONRPC)
      || obj.jsonrpc !== JSON_RPC_VERSION
  ) return;

  // check if we have a propagate message query (no id field and no error/result field)
  if (!Object.prototype.hasOwnProperty.call(obj, answerProperties.ID)) {
    if (Object.prototype.hasOwnProperty.call(obj, 'method')
      && obj.method === methods.MESSAGE
      && Object.prototype.hasOwnProperty.call(obj, 'params')
      && Object.prototype.hasOwnProperty.call(obj.params, 'channel')
      && typeof obj.params.channel === 'string'
      && Object.prototype.hasOwnProperty.call(obj.params, 'message')
      && typeof obj.params.message === 'object'
      && obj.params.message !== null
    ) { _handlePropagateMessage(obj.params); }

    return;
  }

  if (!Object.prototype.hasOwnProperty.call(obj, answerProperties.ID)
    || !Number.isInteger(obj.id)
  ) return;

  // check that the object has exactly one of the two optional fields (XOR)
  if (Object.prototype.hasOwnProperty.call(obj, answerProperties.RESULT)
      === Object.prototype.hasOwnProperty.call(obj, answerProperties.ERROR)
  ) return;

  // if the message answers un unknown request, then drop the message
  if (!_hasPendingQuery(obj.id)) return;

  // processes server answer
  if (Object.prototype.hasOwnProperty.call(obj, answerProperties.RESULT)) {
    // processes positive server answer
    if (obj.result === 0) {
      // general positive answer
      _handlePositiveServerAnswer(obj);
    } else {
      console.error('TODO handle propagate (+ what if none of those two possibilities, silent return?)');
    }
  } else {
    // processes negative server answer (error)
    const { error } = obj;

    if (typeof error !== 'object' || error === null) return;
    if (!Object.prototype.hasOwnProperty.call(error, errorProperties.CODE)
        || !Object.prototype.hasOwnProperty.call(error, errorProperties.DESCRIPTION)
        || Object.keys(error).length !== Object.keys(errorProperties).length
    ) return;
    if (!Number.isInteger(error.code) || error.code < -5 || error.code > -1) return;
    if (typeof error.description !== 'string') return;

    const query = _getPendingQuery(obj.id);
    const { retryCount } = _getPendingQuery(obj.id);
    const newQuery = new PendingRequest(
      query.message,
      query.requestObject,
      query.requestAction,
      retryCount + 1,
    );

    // check if the message should be resent once again
    if (retryCount < MAX_QUERY_RETRIES) {
      _setPendingQuery(obj.id, newQuery);
      WebsocketLink.sendRequestToServer(
        newQuery.message,
        newQuery.requestObject,
        newQuery.requestAction,
        true,
      );
    } else {
      console.error(`max retryCount (${MAX_QUERY_RETRIES}) reached! Failing query : `, query);
      _delPendingQuery(obj.id);
    }
  }
};

export default handleServerAnswer;
