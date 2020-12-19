// eslint-disable-next-line import/no-cycle
import WebsocketLink from './WebsocketLink';
import {
  actions, JSON_RPC_VERSION, objects, PendingRequest, SERVER_ANSWER_FIELD_COUNT, fromString64,
  getCurrentLao,
} from './WebsocketUtils';
import { getStore } from '../Store/configureStore';

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
  const obj = JSON.parse(message.data);

  console.log('handling a new answer : ', obj);

  /* check that the answer is of a valid format (positive or negative) */

  // check that the object has exactly SERVER_ANSWER_FIELD_COUNT fields
  if (Object.keys(obj).length !== SERVER_ANSWER_FIELD_COUNT) return;

  // check that the object has both required field and that the protocol used is correct
  if (!Object.prototype.hasOwnProperty.call(obj, answerProperties.JSONRPC)
      || obj.jsonrpc !== JSON_RPC_VERSION
  ) return;
  if (!Object.prototype.hasOwnProperty.call(obj, answerProperties.ID)
      || !Number.isInteger(obj.id)
  ) return;

  // check that the object has exactly one of the two optional fields (XOR)
  if (Object.prototype.hasOwnProperty.call(obj, answerProperties.RESULT)
      === Object.prototype.hasOwnProperty.call(obj, answerProperties.ERROR)
  ) return;

  // if the message answers un unknown request, then drop the message
  if (!WebsocketLink.getPendingProperties().has(obj.id)) return;

  // processes server answer
  if (Object.prototype.hasOwnProperty.call(obj, answerProperties.RESULT)) {
    // processes positive server answer
    if (obj.result === 0) {
      // general positive answer
      const answer = WebsocketLink.getPendingProperties().get(obj.id);

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
            const updatedLao = JSON.parse(JSON.stringify(getCurrentLao()));

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

        case objects.MESSAGE:
          console.error('TODO (in WebsocketAnswer) : case (objects.MESSAGE)');
          break;

        case objects.MEETING:
          console.error('TODO (in WebsocketAnswer) : case (objects.MEETING)');
          break;

        default:
          break;
      }

      WebsocketLink.getPendingProperties().delete(obj.id);
    } else {
      console.error('TODO handle propagate (+ what if none of those two possibilities, silent return?)');
    }
  } else {
    // processes negative server answer (error)
    const { error } = obj;

    if (typeof error !== 'object' || error === null) return;
    if (!Object.prototype.hasOwnProperty.call(error, errorProperties.CODE)
        || !Object.prototype.hasOwnProperty.call(error, errorProperties.DESCRIPTION)
    ) return;
    if (!Number.isInteger(error.code) || error.code < -5 || error.code > -1) return;
    if (typeof error.description !== 'string') return;

    const query = WebsocketLink.getPendingProperties().get(obj.id);
    const { retryCount } = WebsocketLink.getPendingProperties().get(obj.id);
    if (retryCount < MAX_QUERY_RETRIES) {
      WebsocketLink.getPendingProperties().set(
        obj.id,
        new PendingRequest(query.message, query.requestObject, query.requestAction, retryCount + 1),
      );
      WebsocketLink.sendRequestToServer(query.message, true);
    } else {
      console.error(`max retryCount (${MAX_QUERY_RETRIES}) reached! Failing query : `, query);
      WebsocketLink.getPendingProperties().delete(obj.id);
    }
  }
};

export default handleServerAnswer;
