package sqlite

const (
	DefaultPath    = "sqlite.db"
	serverKeysPath = "server_keys"
)

const (
	RootType         = "root"
	LaoType          = "lao"
	ElectionType     = "election"
	ChirpType        = "chirp"
	ReactionType     = "reaction"
	ConsensusType    = "consensus"
	CoinType         = "coin"
	AuthType         = "auth"
	PopChaType       = "popcha"
	GeneralChirpType = "generalChirp"
)

var channelTypeToID = map[string]string{
	RootType:         "1",
	LaoType:          "2",
	ElectionType:     "3",
	ChirpType:        "4",
	ReactionType:     "5",
	ConsensusType:    "6",
	PopChaType:       "7",
	CoinType:         "8",
	AuthType:         "9",
	GeneralChirpType: "10",
}

var channelTypes = []string{
	RootType,
	LaoType,
	ElectionType,
	ChirpType,
	ReactionType,
	ConsensusType,
	PopChaType,
	CoinType,
	AuthType,
	GeneralChirpType,
}

const (
	createMessage = `
	CREATE TABLE IF NOT EXISTS message (
	    		messageID TEXT,
	    		message TEXT,
	    		messageData TEXT NULL,
	    		storedTime BIGINT,
	    		PRIMARY KEY (messageID)
	            )`

	createChannelType = `
	CREATE TABLE IF NOT EXISTS channelType (
	    		ID INTEGER,
	    		type TEXT,
	    		PRIMARY KEY (ID)
	    		)`

	createChannel = `
	CREATE TABLE IF NOT EXISTS channel (
	    		channelPath TEXT,
	    		typeID TEXT,
	    		laoPath TEXT NULL,
	    		FOREIGN KEY (laoPath) REFERENCES channel(channelPath),
	    		FOREIGN KEY (typeID) REFERENCES channelType(ID),
	    		PRIMARY KEY (channelPath)
	            )`

	createKey = `
	CREATE TABLE IF NOT EXISTS key (
	    		channelPath TEXT,
	    		publicKey TEXT,
	    		secretKey TEXT NULL,
	    		FOREIGN KEY (channelPath) REFERENCES channel(channelPath),
	    		PRIMARY KEY (channelPath)
	            )`

	createChannelMessage = `
	CREATE TABLE IF NOT EXISTS channelMessage (
	    		channelPath TEXT,
	    		messageID TEXT,
	    		isBaseChannel BOOLEAN,
	    		FOREIGN KEY (messageID) REFERENCES message(messageID),
	    		FOREIGN KEY (channelPath) REFERENCES channel(channelPath),
	    		PRIMARY KEY (channelPath, messageID)
	            )`

	createPendingSignatures = `
	CREATE TABLE IF NOT EXISTS pendingSignatures (
	    		messageID TEXT,
	    		witness TEXT,
	    		signature TEXT UNIQUE,
	    		PRIMARY KEY (messageID, witness)
	            )`

	createRumor = `
	CREATE TABLE IF NOT EXISTS rumor ( 
    			ID INTEGER, 
    			sender TEXT, 
    			PRIMARY KEY (ID, sender) 
                )`

	createMessageRumor = `
	CREATE TABLE IF NOT EXISTS messageRumor (
				messageID TEXT,
				rumorID INTEGER,
				sender TEXT,
				FOREIGN KEY (messageID) REFERENCES message(messageID),
				FOREIGN KEY (rumorID, sender) REFERENCES rumor(ID, sender),
				PRIMARY KEY (messageID, rumorID, sender)
	            )`

	createUnprocessedMessage = `
	CREATE TABLE IF NOT EXISTS unprocessedMessage (
	    				messageID TEXT,
	    				channelPath TEXT,
	    				message TEXT,
	    				PRIMARY KEY (messageID)
	)`

	createUnprocessedMessageRumor = `
	CREATE TABLE IF NOT EXISTS unprocessedMessageRumor (
	    				messageID TEXT,
	    				rumorID INTEGER,
	    				sender TEXT,
	    				FOREIGN KEY (messageID) REFERENCES unprocessedMessage(messageID),
	    				FOREIGN KEY (rumorID, sender) REFERENCES rumor(ID, sender),
	    				PRIMARY KEY (messageID, rumorID, sender)
	)`
)

const (
	insertChannelMessage           = `INSERT INTO channelMessage (channelPath, messageID, isBaseChannel) VALUES (?, ?, ?)`
	insertMessage                  = `INSERT INTO message (messageID, message, messageData, storedTime) VALUES (?, ?, ?, ?)`
	insertChannel                  = `INSERT INTO channel (channelPath, typeID, laoPath) VALUES (?, ?, ?)`
	insertOrIgnoreChannel          = `INSERT OR IGNORE INTO channel (channelPath, typeID, laoPath) VALUES (?, ?, ?)`
	insertKeys                     = `INSERT INTO key (channelPath, publicKey, secretKey) VALUES (?, ?, ?)`
	insertPublicKey                = `INSERT INTO key (channelPath, publicKey) VALUES (?, ?)`
	insertPendingSignatures        = `INSERT INTO pendingSignatures (messageID, witness, signature) VALUES (?, ?, ?)`
	insertRumor                    = `INSERT INTO rumor (ID, sender) VALUES (?, ?)`
	insertUnprocessedMessage       = `INSERT INTO unprocessedMessage (messageID, channelPath, message) VALUES (?, ?, ?)`
	insertUnprocessedMessageRumor  = `INSERT INTO unprocessedMessageRumor (messageID, rumorID, sender) VALUES (?, ?, ?)`
	insertMessageRumor             = `INSERT INTO messageRumor (messageID, rumorID, sender) VALUES (?, ?, ?)`
	tranferUnprocessedMessageRumor = `INSERT INTO messageRumor (messageID, rumorID, sender) SELECT messageID, rumorID, sender FROM unprocessedMessageRumor WHERE messageID = ?`
	deleteUnprocessedMessage       = `DELETE FROM unprocessedMessage WHERE messageID = ?`
	deleteUnprocessedMessageRumor  = `DELETE FROM unprocessedMessageRumor WHERE messageID = ?`
)

const (
	selectKeys = `SELECT publicKey, secretKey FROM key WHERE channelPath = ?`

	selectPublicKey = `SELECT publicKey FROM key WHERE channelPath = ?`

	selectSecretKey = `SELECT secretKey FROM key WHERE channelPath = ?`

	selectPendingSignatures = `SELECT witness, signature FROM pendingSignatures WHERE messageID = ?`

	selectMessage = `SELECT message FROM message WHERE messageID = ?`

	selectAllChannels = `SELECT channelPath FROM channel`

	selectChannelType = `SELECT type FROM channelType JOIN channel on channel.typeID = channelType.ID WHERE channelPath = ?`

	selectAllMessagesFromChannel = `
    SELECT message.message
    FROM message 
    JOIN channelMessage ON message.messageID = channelMessage.messageID
    WHERE channelMessage.channelPath = ?
    ORDER BY message.storedTime DESC`

	selectBaseChannelMessages = `SELECT messageID, channelPath FROM channelMessage WHERE isBaseChannel = ?`

	selectChannelPath = `SELECT channelPath FROM channel WHERE channelPath = ?`

	selectMessageID = `SELECT messageID FROM message WHERE messageID = ?`

	selectLastRollCallMessage = `
    SELECT json_extract(messageData, '$.action') 
    FROM message 
    WHERE storedTime = (
        SELECT MAX(storedTime) 
        FROM (
            SELECT * 
            FROM message 
            JOIN channelMessage ON message.messageID = channelMessage.messageID
        ) 
        WHERE json_extract(messageData, '$.object') = ? AND channelPath = ?
    )`

	selectLastRollCallMessageInList = `
    SELECT message.messageData, json_extract(message.messageData, '$.action')
    FROM message
    JOIN channelMessage ON message.messageID = channelMessage.messageID
    WHERE channelMessage.channelPath = ?
      AND json_extract(message.messageData, '$.object') = ?
      AND json_extract(message.messageData, '$.action') IN (?, ?)
    ORDER BY message.storedTime DESC
    LIMIT 1`

	selectLaoWitnesses = `
    SELECT json_extract(messageData, '$.witnesses')
    FROM (
        SELECT * 
        FROM message 
        JOIN channelMessage ON message.messageID = channelMessage.messageID
    )
    WHERE channelPath = ?
      AND json_extract(messageData, '$.object') = ?
      AND json_extract(messageData, '$.action') = ?`

	selectLaoOrganizerKey = `
    SELECT publicKey 
    FROM key 
    WHERE channelPath = (
        SELECT laoPath 
        FROM channel 
        WHERE channelPath = ?
    )
`

	selectLastElectionMessage = `
    SELECT json_extract(messageData, '$.action')
    FROM message
    WHERE storedTime = (
        SELECT MAX(storedTime)
        FROM (
            SELECT *
            FROM message
            JOIN channelMessage ON message.messageID = channelMessage.messageID
        )
        WHERE channelPath = ?
          AND json_extract(messageData, '$.object') = ?
          AND json_extract(messageData, '$.action') != ?
    )`

	selectElectionCreationTime = `
    SELECT json_extract(messageData, '$.created_at')
    FROM (
        SELECT *
        FROM message
        JOIN channelMessage ON message.messageID = channelMessage.messageID
    )
    WHERE channelPath = ?
      AND json_extract(messageData, '$.object') = ?
      AND json_extract(messageData, '$.action') = ?`

	selectElectionType = `
    SELECT json_extract(messageData, '$.version')
    FROM (
        SELECT *
        FROM message
        JOIN channelMessage ON message.messageID = channelMessage.messageID
    )
    WHERE channelPath = ?
      AND json_extract(messageData, '$.object') = ?
      AND json_extract(messageData, '$.action') = ?`

	selectElectionAttendees = `
    SELECT joined.messageData
    FROM (
        SELECT *
        FROM message
        JOIN channelMessage ON message.messageID = channelMessage.messageID
    ) joined
    JOIN channel c ON joined.channelPath = c.laoPath
    WHERE c.channelPath = ?
      AND json_extract(joined.messageData, '$.object') = ?
      AND json_extract(joined.messageData, '$.action') = ?
      AND joined.storedTime = (
        SELECT MAX(storedTime)
        FROM (
            SELECT *
            FROM message
            JOIN channelMessage ON message.messageID = channelMessage.messageID
        )
        WHERE channelPath = c.laoPath 
          AND json_extract(messageData, '$.object') = ?
          AND json_extract(messageData, '$.action') = ?
    )`

	selectElectionSetup = `
    SELECT messageData
    FROM (
        SELECT *
        FROM message
        JOIN channelMessage ON message.messageID = channelMessage.messageID
    )
    WHERE channelPath = ?
      AND json_extract(messageData, '$.object') = ?
      AND json_extract(messageData, '$.action') = ?`

	selectCastVotes = `
    SELECT messageData, messageID, json_extract(message, '$.sender')
    FROM (
        SELECT *
        FROM message
        JOIN channelMessage ON message.messageID = channelMessage.messageID
    )
    WHERE channelPath = ?
      AND json_extract(messageData, '$.object') = ?
      AND json_extract(messageData, '$.action') = ?`

	selectLastRollCallClose = `
    SELECT messageData
    FROM message
    WHERE storedTime = (
        SELECT MAX(storedTime)
        FROM (
            SELECT *
            FROM message
            JOIN channelMessage ON message.messageID = channelMessage.messageID
        )
        WHERE channelPath = ?
          AND json_extract(messageData, '$.object') = ?
          AND json_extract(messageData, '$.action') = ?
    )`

	selectSender = `
    SELECT json_extract(message, '$.sender'), 
           json_extract(messageData, '$.object'), 
           json_extract(messageData, '$.action') 
    FROM message 
    WHERE messageID = ?`

	selectRumor = `SELECT ID FROM rumor WHERE ID = ? AND sender = ?`

	selectAllUnprocessedMessages = `SELECT channelPath, message FROM unprocessedMessage`
)

const (
	deletePendingSignatures = `DELETE FROM pendingSignatures WHERE messageID = ?`
)

const (
	updateMsg = `UPDATE OR IGNORE message SET message = json_insert(message,'$.witness_signatures[#]', json(?)) WHERE messageID = ?`
)
