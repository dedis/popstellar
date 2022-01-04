const Ajv = require("ajv");
const path = require("path");
const fs = require("fs");

const ajv = require("./main");

const rootSchema = "https://raw.githubusercontent.com/dedis/student_21_pop/master/protocol/jsonRPC.json"
const messageDataSchema = "https://raw.githubusercontent.com/dedis/student_21_pop/master/protocol/query/method/message/data/data.json"

// custom validator to display better error message.
expect.extend({
    toBeValid(filename, schemaID) {
        const valid = ajv.validate(
            schemaID,
            filename
        );

        if (valid) {
            return {
                message: () => `Schema should NOT be valid`,
                pass: true
            };
        } else {
            return {
                message: () =>
                    `Schema should be valid: ${printError(ajv.errors)}`,
                pass: false
            };
        }
    }
});

// Tests all files in specified directories.
describe("Check files", () => {
    let data = [];

    const directories = [
        ".",
        "answer",
        "query",
        "query/broadcast",
        "query/subscribe",
        "query/unsubscribe",
        "query/publish",
        "query/catchup"
    ];

    directories.forEach((directory) => {
        let absolutePath = path.join(__dirname, "..", "examples", directory);

        // add all files from directory
        var files = fs.readdirSync(absolutePath);
        files.forEach((file) => {
            const filePath = path.join(absolutePath, file);
            var stats = fs.statSync(filePath);
            if (
                stats.isFile() &&
                !file.startsWith(".") &&
                file.endsWith(".json")
            ) {
                data.push(filePath);
            }
        });
    });

    // Iterate over files. Automatically makes an assert false with files
    // starting with `wrong_`.
    test.each(data)("validate %s", (filePath) => {
        const file = require(filePath);

        const filename = path.basename(filePath);

        if (filename.startsWith("wrong_")) {
            expect(file).not.toBeValid(rootSchema);
        } else {
            expect(file).toBeValid(rootSchema);
        }
    });
});

test("message data: lao", () => {
    lao_create = require("../examples/messageData/lao_create/lao_create.json")
    expect(lao_create).toBeValid(messageDataSchema);

    lao_create = require("../examples/messageData/lao_create/wrong_lao_create_additional_params.json")
    expect(lao_create).not.toBeValid(messageDataSchema);

    lao_create = require("../examples/messageData/lao_create/wrong_lao_create_missing_params.json")
    expect(lao_create).not.toBeValid(messageDataSchema);

    lao_state = require("../examples/messageData/lao_state/lao_state.json")
    expect(lao_state).toBeValid(messageDataSchema);

    lao_state = require("../examples/messageData/lao_state/wrong_lao_state_additional_params.json")
    expect(lao_state).not.toBeValid(messageDataSchema);

    lao_state = require("../examples/messageData/lao_state/wrong_lao_state_missing_params.json")
    expect(lao_state).not.toBeValid(messageDataSchema);

    lao_update = require("../examples/messageData/lao_update/lao_update.json")
    expect(lao_update).toBeValid(messageDataSchema);

    lao_update = require("../examples/messageData/lao_update/wrong_lao_update_additional_params.json")
    expect(lao_update).not.toBeValid(messageDataSchema);

    lao_update = require("../examples/messageData/lao_update/wrong_lao_update_missing_params.json")
    expect(lao_update).not.toBeValid(messageDataSchema);
})

test("message data: vote", () => {
    vote_cast_vote = require("../examples/messageData/vote_cast_vote/vote_cast_vote.json")
    expect(vote_cast_vote).toBeValid(messageDataSchema)

    vote_cast_write_in = require("../examples/messageData/vote_cast_write_in.json")
    expect(vote_cast_write_in).toBeValid(messageDataSchema)
})

test("message data: roll call", () => {
    roll_call_close = require("../examples/messageData/roll_call_close.json")
    expect(roll_call_close).toBeValid(messageDataSchema)

    roll_call_create = require("../examples/messageData/roll_call_create.json")
    expect(roll_call_create).toBeValid(messageDataSchema)

    roll_call_open = require("../examples/messageData/roll_call_open.json")
    expect(roll_call_open).toBeValid(messageDataSchema)

    roll_call_reopen = require("../examples/messageData/roll_call_reopen.json")
    expect(roll_call_reopen).toBeValid(messageDataSchema)
})

test("message data: meeting", () => {
    meeting_create = require("../examples/messageData/meeting_create.json")
    expect(meeting_create).toBeValid(messageDataSchema)

    meeting_state = require("../examples/messageData/meeting_state.json")
    expect(meeting_state).toBeValid(messageDataSchema)
})

test("message data: election", () => {
    election_end = require("../examples/messageData/election_end/election_end.json")
    expect(election_end).toBeValid(messageDataSchema)

    election_result = require("../examples/messageData/election_result.json")
    expect(election_result).toBeValid(messageDataSchema)

    election_setup = require("../examples/messageData/election_setup/election_setup.json")
    expect(election_setup).toBeValid(messageDataSchema)
})

test("message data: message", () => {
    message_witness = require("../examples/messageData/message_witness.json")
    expect(message_witness).toBeValid(messageDataSchema)
})

test("message data: chirp", () => {
    chirp_add = require("../examples/messageData/chirp_add_publish/chirp_add_publish.json")
    expect(chirp_add).toBeValid(messageDataSchema)

    chirp_add_notify = require("../examples/messageData/chirp_notify/chirp_add_notify.json")
    expect(chirp_add_notify).toBeValid(messageDataSchema)

    chirp_delete = require("../examples/messageData/chirp_delete_publish/chirp_delete_publish.json")
    expect(chirp_delete).toBeValid(messageDataSchema)

    chirp_delete_broadcast = require("../examples/messageData/chirp_delete_broadcast/chirp_delete_broadcast.json")
    expect(chirp_delete_broadcast).toBeValid(messageDataSchema)

    reaction_add = require("../examples/messageData/reaction_add/reaction_add.json")
    expect(reaction_add).toBeValid(messageDataSchema)

    reaction_delete = require("../examples/messageData/reaction_delete/reaction_delete.json")
    expect(reaction_delete).toBeValid(messageDataSchema)
})

test("message data: consensus", () => {
    elect = require("../examples/messageData/consensus_elect/elect.json");
    expect(elect).toBeValid(messageDataSchema)

    elect_accept = require("../examples/messageData/consensus_elect_accept/elect_accept.json");
    expect(elect_accept).toBeValid(messageDataSchema)

    // Prepare
    prepare = require("../examples/messageData/consensus_prepare/prepare.json");
    expect(prepare).toBeValid(messageDataSchema)

    prepare = require("../examples/messageData/consensus_prepare/wrong_prepare_negative_created_at.json");
    expect(prepare).not.toBeValid(messageDataSchema)

    prepare = require("../examples/messageData/consensus_prepare/wrong_prepare_negative_proposed_try.json");
    expect(prepare).not.toBeValid(messageDataSchema)

    // Promise
    promise = require("../examples/messageData/consensus_promise/promise.json");
    expect(promise).toBeValid(messageDataSchema)

    promise = require("../examples/messageData/consensus_promise/wrong_promise_negative_created_at.json");
    expect(promise).not.toBeValid(messageDataSchema)

    promise = require("../examples/messageData/consensus_promise/wrong_promise_negative_accepted_try.json");
    expect(promise).not.toBeValid(messageDataSchema)

    // Propose
    propose = require("../examples/messageData/consensus_propose/propose.json");
    expect(propose).toBeValid(messageDataSchema)

    propose = require("../examples/messageData/consensus_propose/wrong_propose_negative_created_at.json");
    expect(propose).not.toBeValid(messageDataSchema)

    propose = require("../examples/messageData/consensus_propose/wrong_propose_negative_proposed_try.json");
    expect(propose).not.toBeValid(messageDataSchema)

    // Accept
    accept = require("../examples/messageData/consensus_accept/wrong_accept_negative_created_at.json");
    expect(accept).not.toBeValid(messageDataSchema)

    accept = require("../examples/messageData/consensus_accept/wrong_accept_negative_accepted_try.json");
    expect(accept).not.toBeValid(messageDataSchema)

    // Learn
    learn = require("../examples/messageData/consensus_learn/learn.json");
    expect(learn).toBeValid(messageDataSchema)

    // Failure
    failure = require("../examples/messageData/consensus_failure/failure.json");
    expect(failure).toBeValid(messageDataSchema)

    failure = require("../examples/messageData/consensus_failure/wrong_failure_negative_created_at.json");
    expect(failure).not.toBeValid(messageDataSchema)
})

function printError(errors) {
    return errors
        .map((e) => JSON.stringify(e, null, "  "))
        .reduce((text, msg) => text + "\n" + msg);
}
