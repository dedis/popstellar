const Ajv = require("ajv")
const main_schema = require("../genericMessage.json")
const answer_schema = require("../answer/answer.json")
const error_schema = require("../answer/negativeAnswer/error.json")
const generic_answer_schema = require("../answer/positiveAnswer/positiveAnswerGeneral.json")
const catchup_answer_schema = require("../answer/positiveAnswer/positiveAnswerCatchup.json")

const query_general = require("../query/method/message/messageGeneral.json")
const query_witness = require("../query/method/message/messageWitnessMessage.json")

const ajv = new Ajv()

ajv.opts.strict = false;
ajv.addSchema([
    main_schema,
    answer_schema,
    error_schema,
    generic_answer_schema,
    catchup_answer_schema,
    query_general,
    query_witness
])

function validate(example) {
    const valid = ajv.validate("https://raw.githubusercontent.com/dedis/student_21_pop/master/protocol/genericMessage.json", example)
    return valid
}

module.exports = validate;