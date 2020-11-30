import WebsocketLink from './WebsocketLink';
import { JSON_RPC_VERSION, PendingRequest, SERVER_ANSWER_FIELD_COUNT } from './WebsocketUtils';

const MAX_QUERY_RETRIES = 3;
const answerProperties = Object.freeze({
  JSONRPC: 'jsonrpc',
  RESULT: 'result',
  ERROR: 'error',
  ID: 'id',
});

const errorProperties = Object.freeze({
  CODE: 'code',
  DESCRIPTION: 'description',
});

export const handleServerAnswer = (message) => {
  const obj = JSON.parse(message.data);

  console.log('handling a new answer : ', obj);

  /* check that the answer is of a valid format (positive or negative) */

  // check that the object has exactly SERVER_ANSWER_FIELD_COUNT fields
  if (Object.keys(obj).length !== SERVER_ANSWER_FIELD_COUNT) return;
  // check that the object has both required field and that the protocol used is correct
  if (!obj.hasOwnProperty(answerProperties.JSONRPC) || obj.jsonrpc !== JSON_RPC_VERSION) return;
  if (!obj.hasOwnProperty(answerProperties.ID) || !Number.isInteger(obj.id)) return;
  // check that the object has exactly one of the two optional fields (XOR)
  if (obj.hasOwnProperty(answerProperties.RESULT) === obj.hasOwnProperty(answerProperties.ERROR)) return;

  // if the message answers un unknown request, then drop the message
  if (!WebsocketLink.getPendingProperties().has(obj.id)) return;

  // processes server answer
  if (obj.hasOwnProperty(answerProperties.RESULT)) {
    // processes positive server answer
    if (obj.result === 0) {
      // general positive answer
      WebsocketLink.getPendingProperties().delete(obj.id);
    } else {
      // TODO handle propagate (+ what if none of those two possibilities, silent return?)

    }
  } else {
    // processes negative server answer (error)
    const { error } = obj;

    if (typeof error !== 'object' || error === null) return;
    if (!error.hasOwnProperty(errorProperties.CODE) || !error.hasOwnProperty(errorProperties.DESCRIPTION)) return;
    if (!Number.isInteger(error.code) || error.code < -5 || error.code > -1) return;
    if (typeof error.description !== 'string') return;

    const query = WebsocketLink.getPendingProperties().get(obj.id).message;
    const { retryCount } = WebsocketLink.getPendingProperties().get(obj.id);
    if (retryCount < MAX_QUERY_RETRIES) {
      WebsocketLink.getPendingProperties().set(obj.id, new PendingRequest(query, retryCount + 1));
      WebsocketLink.sendRequestToServer(query, true);
    } else {
      console.error(`max retryCount (${MAX_QUERY_RETRIES}) reached! Failing query : `, query);
      WebsocketLink.getPendingProperties().delete(obj.id);
    }
  }

  // ------------------- ALG ----------------------------------

  // Idea : check that the generated id is not already in the map

  // ---------------- TODO --------------------------------------
  // Builder for data object (building json objects iteratively?)
  // Wrapper for Jsonrpc, method, params, id
};
