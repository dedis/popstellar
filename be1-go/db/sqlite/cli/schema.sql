-- This schema maps the content of an organizer.
--
-- lao_channel ◄── message_info ◄── message_witness
--   ▲  ▲  
--   │  └── lao_witness
--   └── lao_attendee
--

CREATE TABLE lao_channel (
    lao_channel_id TEXT NOT NULL PRIMARY KEY
);

CREATE TABLE lao_attendee (
    attendee_id INTEGER PRIMARY KEY,

    attendee_key TEXT NOT NULL,

    lao_channel_id TEXT NOT NULL
        REFERENCES lao_channel(lao_channel_id)
        ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE lao_witness (
    witness_id INTEGER PRIMARY KEY,

    pub_key TEXT NOT NULL,

    lao_channel_id TEXT NOT NULL
        REFERENCES lao_channel(lao_channel_id)
        ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE message_info (
    message_id TEXT NOT NULL PRIMARY KEY,
    sender TEXT NOT NULL,
    message_signature TEXT NOT NULL,
    raw_data TEXT,
    message_timestamp INTEGER,

    lao_channel_id TEXT NOT NULL
        REFERENCES lao_channel(lao_channel_id)
        ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE message_witness (
    witness_id INTEGER PRIMARY KEY,

    pub_key TEXT NOT NULL,
    witness_signature TEXT NOT NULL,

    message_id TEXT NOT NULL
        REFERENCES message_info(message_id)
        ON UPDATE CASCADE ON DELETE CASCADE
);

--
-- Election channel
--

CREATE TABLE election_channel (
    election_channel_id TEXT NOT NULL PRIMARY KEY,

    start_timestamp INTEGER,
    end_timestamp INTEGER,
    terminated INTEGER
);

CREATE TABLE election_attendee (
    attendee_key TEXT NOT NULL,

    election_channel_id TEXT NOT NULL
        REFERENCES election_channel(election_channel_id)
        ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE election_question (
    question_id TEXT NOT NULL PRIMARY KEY,

    method TEXT NOT NULL,

    election_channel_id TEXT NOT NULL
        REFERENCES election_channel(election_channel_id)
        ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE election_question_ballot_option (
    option_text TEXT NOT NULL,

    question_id TEXT NOT NULL
        REFERENCES election_question(question_id)
        ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE election_valid_vote (
    voter_id TEXT NOT NULL,

    vote_timestamp INTEGER,

    question_id TEXT NOT NULL
        REFERENCES election_question(question_id)
        ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE vote_index (
    vote_index INTEGER

    voter_id TEXT NOT NULL
        REFERENCES election_valid_vote(voter_id)
        ON UPDATE CASCADE ON DELETE CASCADE
);