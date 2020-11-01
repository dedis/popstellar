import WebsocketLink from './WebsocketLink';

const types = Object.freeze({
  LAO: 'lao',
  EVENT: 'event',
  VOTE: 'vote',
});

const actions = Object.freeze({
  GET: 'get',
  CREATE: 'create',
  JOIN: 'join',
});

const _sign = (...args) => 'signature'; // TODO modify

const _handleAnswerError = (error) => console.error('(TODO)', error);

const _handleStandardAnswer = (result, caller, resolve, reject) => {
  if (result.hasOwnProperty('success') && result.hasOwnProperty('error')) {
    if (result.success === 'true') resolve();
    else reject(result.error);
    
  } else {
    let errorMessage = '(TODO) Unexpected server answer';
    if (caller) errorMessage += (` for ${caller}`);

    errorMessage += ".\nProperty 'success' or 'error' does not exist in server answer : ";
    errorMessage += JSON.stringify(result);

    reject(errorMessage);
  }
};

export const requestCreateLao = (name, resolve, reject) => {
  const date = Date.now();
  const organizerKey = 'organizerKey'; // TODO modify
  const witnessesKeys = []; // TODO modify

  WebsocketLink.sendRequestToServer({
    type: types.LAO,
    action: actions.CREATE,
    name,
    date,
    organizer: organizerKey,
    witnesses: witnessesKeys,
    attestation: _sign(name, date, organizerKey, witnessesKeys),
  });

  const promise = new Promise((resolveServerAnswer, rejectServerAnswer) => {
    WebsocketLink.waitServerAnswer(resolveServerAnswer, rejectServerAnswer);
  });

  promise.then(
    (result) => _handleStandardAnswer(result, 'requestCreateLao', resolve, reject),
    (error) => _handleAnswerError(error),
  );
};

export const requestJoinLao = (laoId, resolve, reject) => {
  WebsocketLink.sendRequestToServer({
    type: types.LAO,
    action: actions.JOIN,
    lao: laoId,
    person: 'personPublicKey', // TODO modify
    attestation: _sign(laoId),
  });

  const promise = new Promise((resolveServerAnswer, rejectServerAnswer) => {
    WebsocketLink.waitServerAnswer(resolveServerAnswer, rejectServerAnswer);
  });

  promise.then(
    (result) => {
      console.log(
        "(TODO) requestJoinLao sent and server answered! WebsocketUtils.js doesn't know what to do next\n"
        + 'Server answer :', result,
      );
      resolve();
    },
    (error) => _handleAnswerError(error),
  );
};

export const requestCreateEvent = (name, location, resolve, reject) => {
  const laoId = 'hash de la LAO'; // TODO get current lao id

  WebsocketLink.sendRequestToServer({
    type: types.EVENT,
    action: actions.CREATE,
    lao: laoId,
    name,
    location,
    attestation: _sign(laoId, name, location), // TODO creation date? Inconsistency in online doc
  });

  const promise = new Promise((resolveServerAnswer, rejectServerAnswer) => {
    WebsocketLink.waitServerAnswer(resolveServerAnswer, rejectServerAnswer);
  });

  promise.then(
    (result) => _handleStandardAnswer(result, 'requestCreateEvent', resolve, reject),
    (error) => _handleAnswerError(error),
  );
};

export const requestCastVote = (vote, resolve, reject) => {
  const clientPublicKey = "client's public key"; // TODO get client public key
  const electionId = "hash de l'élection"; // TODO get current election id
  const encryptedVote = vote;

  WebsocketLink.sendRequestToServer({
    type: types.VOTE,
    action: actions.CREATE,
    person: clientPublicKey,
    election: electionId,
    vote: encryptedVote,
    attestation: _sign(clientPublicKey, electionId, encryptedVote),
  });

  const promise = new Promise((resolveServerAnswer, rejectServerAnswer) => {
    WebsocketLink.waitServerAnswer(resolveServerAnswer, rejectServerAnswer);
  });

  promise.then(
    (result) => _handleStandardAnswer(result, 'requestCastVote', resolve, reject),
    (error) => _handleAnswerError(error),
  );
};

export const requestCreateChannel = (channelName, contract, resolve, reject) => {
  WebsocketLink.sendRequestToServer({
    publish: {
      channel: channelName,
      contract,
    },
  });

  const promise = new Promise((resolveServerAnswer, rejectServerAnswer) => {
    WebsocketLink.waitServerAnswer(resolveServerAnswer, rejectServerAnswer);
  });

  promise.then(
    (result) => _handleStandardAnswer(result, 'requestCreateChannel', resolve, reject),
    (error) => _handleAnswerError(error),
  );
};

export const requestPublishChannel = (channelName, eventContent, resolve, reject) => {
  WebsocketLink.sendRequestToServer({
    publish: {
      channel: channelName,
      event: eventContent,
    },
  });

  const promise = new Promise((resolveServerAnswer, rejectServerAnswer) => {
    WebsocketLink.waitServerAnswer(resolveServerAnswer, rejectServerAnswer);
  });

  promise.then(
    (result) => _handleStandardAnswer(result, 'requestPublishChannel', resolve, reject),
    (error) => _handleAnswerError(error),
  );
};

export const requestSubscribeChannel = (channelName, resolve, reject) => {
  WebsocketLink.sendRequestToServer({
    subscribe: {
      channel: channelName,
    },
  });

  const promise = new Promise((resolveServerAnswer, rejectServerAnswer) => {
    WebsocketLink.waitServerAnswer(resolveServerAnswer, rejectServerAnswer);
  });

  promise.then(
    (result) => _handleStandardAnswer(result, 'requestSubscribeChannel', resolve, reject),
    (error) => _handleAnswerError(error),
  );
};

export const requestFetchChannel = (channelName, eventId, resolve, reject) => {
  WebsocketLink.sendRequestToServer({
    fetch: {
      channel: channelName,
      event_id: eventId,
    },
  });

  const promise = new Promise((resolveServerAnswer, rejectServerAnswer) => {
    WebsocketLink.waitServerAnswer(resolveServerAnswer, rejectServerAnswer);
  });

  promise.then(
    (result) => {
      console.log(
        "(TODO) requestFetchChannel sent and server answered! WebsocketUtils.js doesn't know what to do next\n"
        + 'Server answer :', result,
      );
      resolve();
    },
    (error) => _handleAnswerError(error),
  );
};
