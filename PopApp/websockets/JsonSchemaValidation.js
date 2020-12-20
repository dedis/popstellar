const Ajv = require('ajv').default;

const ajv = new Ajv();

// Temporary json schema. Waiting for the pull request to wire it all up
// For now, EMPTY_SCHEMA validates every JS object input
const EMPTY_SCHEMA = Object.freeze({});

/** returns true iff data (JS object) is a dataCreateLao */
export const validateDataCreateLao = (data) => {
  const validate = ajv.compile(EMPTY_SCHEMA);
  return validate(data);
};

/** returns true iff data (JS object) is a dataUpdateLao */
export const validateDataUpdateLao = (data) => {
  const validate = ajv.compile(EMPTY_SCHEMA);
  return validate(data);
};

/** returns true iff data (JS object) is a dataStateLao */
const validateDataStateLao = (data) => {
  const validate = ajv.compile(EMPTY_SCHEMA);
  return validate(data);
};

/** returns true iff data (JS object) is a dataWitnessMessage */
const validateDataWitnessMessage = (data) => {
  const validate = ajv.compile(EMPTY_SCHEMA);
  return validate(data);
};

/** returns true iff data (JS object) is a dataCreateMeeting */
const validateDataCreateMeeting = (data) => {
  const validate = ajv.compile(EMPTY_SCHEMA);
  return validate(data);
};

/** returns true iff data (JS object) is a dataStateMeeting */
const validateDataStateMeeting = (data) => {
  const validate = ajv.compile(EMPTY_SCHEMA);
  return validate(data);
};

/** returns true iff data (JS object) is a valid data object */
export const validateData = (data) => (validateDataCreateLao(data)
  || validateDataUpdateLao(data)
  || validateDataStateLao(data)
  || validateDataWitnessMessage(data)
  || validateDataCreateMeeting(data)
  || validateDataStateMeeting(data)
);

/** returns true iff data (JS object) is a valid server answer */
export const validateServerAnswer = (data) => {
  const validate = ajv.compile(EMPTY_SCHEMA);
  return validate(data);
};

/** returns true iff data (JS object) is a valid propagation message */
export const validatePropagateMessage = (data) => {
  const validate = ajv.compile(EMPTY_SCHEMA);
  return validate(data);
};

/** returns true iff data (JS object) is a valid input the client could receive */
export const validateWebsocketInput = (data) => (
  validateServerAnswer(data) || validatePropagateMessage(data)
);
