const validate = require('./main');

const answer_general_example = require("../examples/answer/general.json")
const answer_general_wrong_status_example = require("../examples/answer/general_wrong_status.json")
const answer_wrong_payload_example = require("../examples/answer/wrong_payload.json")
const answer_error_example = require("../examples/answer/error.json")
const answer_error_wrong_status_example = require("../examples/answer/error_wrong_status.json")


// ajv.errorsText(ajv.errors),

test('answer general', () => {
    expect(validate(answer_general_example)).toBe(true);
});

test('answer general wrong status', () => {
    expect(validate(answer_general_wrong_status_example)).toBe(false);
});

test('answer wrong payload', () => {
    expect(validate(answer_wrong_payload_example)).toBe(false);
});

test('answer error', () => {
    expect(validate(answer_error_example)).toBe(true);
});

test('answer error wrong status', () => {
    expect(validate(answer_error_wrong_status_example)).toBe(false);
});