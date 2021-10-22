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

    lao_state = require("../examples/messageData/lao_state/lao_state.json")
    expect(lao_state).toBeValid(messageDataSchema);

    lao_update = require("../examples/messageData/lao_update/lao_update.json")
    expect(lao_update).toBeValid(messageDataSchema);
})

test("message data: vote", () => {
    vote_cast_vote = require("../examples/messageData/vote_cast_vote.json")
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
    election_end = require("../examples/messageData/election_end.json")
    expect(election_end).toBeValid(messageDataSchema)

    election_result = require("../examples/messageData/election_result.json")
    expect(election_result).toBeValid(messageDataSchema)

    election_setup = require("../examples/messageData/election_setup.json")
    expect(election_setup).toBeValid(messageDataSchema)
})

test("message data: message", () => {
    message_witness = require("../examples/messageData/message_witness.json")
    expect(message_witness).toBeValid(messageDataSchema)
})

test("message data: chirp", () => {
    chirp_add = require("../examples/messageData/chirp_add_publish.json")
    expect(chirp_add).toBeValid(messageDataSchema)

    chirp_add_broadcast = require("../examples/messageData/chirp_add_broadcast.json")
    expect(chirp_add_broadcast).toBeValid(messageDataSchema)

})

function printError(errors) {
    return errors
        .map((e) => JSON.stringify(e, null, "  "))
        .reduce((text, msg) => text + "\n" + msg);
}