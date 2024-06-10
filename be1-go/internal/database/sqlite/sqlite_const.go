package sqlite

import (
	"popstellar/internal/handler/channel"
)

const (
	serverKeysPath = "server_keys"
)

var channelTypeToID = map[string]string{
	messagedata.RootObject:       "1",
	messagedata.LAOObject:        "2",
	messagedata.ElectionObject:   "3",
	messagedata.ChirpObject:      "4",
	messagedata.ReactionObject:   "5",
	messagedata.ConsensusObject:  "6",
	messagedata.CoinObject:       "7",
	messagedata.AuthObject:       "8",
	messagedata.FederationObject: "9",
}

var channelTypes = []string{
	messagedata.RootObject,
	messagedata.LAOObject,
	messagedata.ElectionObject,
	messagedata.ChirpObject,
	messagedata.ReactionObject,
	messagedata.ConsensusObject,
	messagedata.CoinObject,
	messagedata.AuthObject,
	messagedata.FederationObject,
}

const foreignKeyOff = `PRAGMA foreign_keys = OFF;`

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
	insertChannelType              = `INSERT INTO channelType (type) VALUES (?)`
	insertKeys                     = `INSERT INTO key (channelPath, publicKey, secretKey) VALUES (?, ?, ?)`
	insertPublicKey                = `INSERT INTO key (channelPath, publicKey) VALUES (?, ?)`
	insertRumor                    = `INSERT INTO rumor (ID, sender) VALUES (?, ?)`
	insertUnprocessedMessage       = `INSERT INTO unprocessedMessage (messageID, channelPath, message) VALUES (?, ?, ?)`
	insertUnprocessedMessageRumor  = `INSERT INTO unprocessedMessageRumor (messageID, rumorID, sender) VALUES (?, ?, ?)`
	insertMessageRumor             = `INSERT INTO messageRumor (messageID, rumorID, sender) VALUES (?, ?, ?)`
	tranferUnprocessedMessageRumor = `INSERT INTO messageRumor (messageID, rumorID, sender) SELECT messageID, rumorID, sender FROM unprocessedMessageRumor WHERE messageID = ?`
	insertMessageToMyRumor         = `
    INSERT INTO messageRumor (messageID, rumorID, sender) 
    SELECT ?, max(ID), sender 
    FROM rumor 
    WHERE sender = (
                    SELECT publicKey 
                 	FROM key 
                    WHERE channelPath = ?
            )
    LIMIT 1`

	insertFirstRumor = `INSERT OR IGNORE INTO rumor (ID, sender) SELECT ?, publicKey FROM key WHERE channelPath = ?`
)

const (
	selectKeys = `SELECT publicKey, secretKey FROM key WHERE channelPath = ?`

	selectPublicKey = `SELECT publicKey FROM key WHERE channelPath = ?`

	selectSecretKey = `SELECT secretKey FROM key WHERE channelPath = ?`

	selectMessage = `SELECT message FROM message WHERE messageID = ?`

	selectAllChannels = `SELECT channelPath FROM channel`

	selectChannelType = `SELECT type FROM channelType JOIN channel on channel.typeID = channelType.ID WHERE channelPath = ?`

	selectAllMessagesFromChannel = `
    SELECT message.message
    FROM message 
    JOIN channelMessage ON message.messageID = channelMessage.messageID
    WHERE channelMessage.channelPath = ?
    ORDER BY message.storedTime DESC`

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

	selectAnyRumor = `SELECT ID FROM rumor WHERE sender = ?`

	selectAllUnprocessedMessages = `SELECT channelPath, message FROM unprocessedMessage`

	selectCountMyRumor = `SELECT count(*) FROM messageRumor WHERE rumorID = (SELECT max(ID) FROM rumor WHERE sender = (SELECT publicKey FROM key WHERE channelPath = ?))`

	selectMyRumorMessages = `
	select message, channelPath
	FROM message JOIN channelMessage ON message.messageID = channelMessage.messageID
		WHERE isBaseChannel = ? 
		AND message.messageID IN 
		      (SELECT messageID 
		       FROM messageRumor 
		       WHERE sender = (SELECT publicKey FROM key WHERE channelPath = ?) AND rumorID = (SELECT max(ID) FROM rumor 
		                                       WHERE sender = (SELECT publicKey FROM key WHERE channelPath = ?)))`

	selectMyRumorInfos = `SELECT max(ID), sender FROM rumor WHERE sender = (SELECT publicKey FROM key WHERE channelPath = ?)`
	selectLastRumor    = `SELECT max(ID) FROM rumor WHERE sender = ?`

	selectValidFederationChallenges = `
	SELECT messageData
	FROM (
	    SELECT *
		FROM message
		JOIN channelMessage ON message.messageID = channelMessage.messageID
	)
	WHERE channelPath = ?
		AND json_extract(message, '$.sender') = ?
		AND json_extract(messageData, '$.object') = ?
		AND json_extract(messageData, '$.action') = ?
		AND json_extract(messageData, '$.value') = ?
		AND json_extract(messageData, '$.valid_until') = ?
	ORDER BY storedTime DESC
	`

	deleteFederationChallenge = `
	DELETE
	FROM message
	WHERE json_extract(messageData, '$.object') = ?
		AND json_extract(messageData, '$.action') = ?
		AND json_extract(messageData, '$.value') = ?
		AND json_extract(messageData, '$.valid_until') = ?
	`

	selectFederationExpects = `
	SELECT messageData
	FROM (
	    SELECT *
		FROM message
		JOIN channelMessage ON message.messageID = channelMessage.messageID
	)
	WHERE channelPath = ?
	    AND json_extract(message, '$.sender') = ?
		AND json_extract(messageData, '$.object') = ?
		AND json_extract(messageData, '$.action') = ?
		AND json_extract(messageData, '$.public_key') = ?
	ORDER BY storedTime DESC
	`
)

const (
	deleteUnprocessedMessage      = `DELETE FROM unprocessedMessage WHERE messageID = ?`
	deleteUnprocessedMessageRumor = `DELETE FROM unprocessedMessageRumor WHERE messageID = ?`
)
