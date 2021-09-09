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