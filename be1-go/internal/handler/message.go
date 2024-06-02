package handler

import (
	"encoding/base64"
	"go.dedis.ch/kyber/v3/sign/schnorr"
	"popstellar/internal/crypto"
	"popstellar/internal/message/answer"
	"popstellar/internal/message/messagedata"
	"popstellar/internal/message/query/method/message"
	"popstellar/internal/singleton/database"
)

func HandleMessage(channelPath string, msg message.Message, fromRumor bool) *answer.Error {
	errAnswer := verifyMessage(msg)
	if errAnswer != nil {
		return errAnswer.Wrap("HandleMessage")
	}

	db, errAnswer := database.GetChannelRepositoryInstance()
	if errAnswer != nil {
		return errAnswer.Wrap("HandleMessage")
	}

	msgAlreadyExists, err := db.HasMessage(msg.MessageID)
	if err != nil {
		errAnswer := answer.NewQueryDatabaseError("if message exists: %v", err)
		return errAnswer.Wrap("HandleMessage")
	}
	if msgAlreadyExists && fromRumor {
		return nil
	}
	if msgAlreadyExists {
		errAnswer := answer.NewInvalidActionError("message %s was already received", msg.MessageID)
		return errAnswer.Wrap("HandleMessage")
	}

	return HandleChannel(channelPath, msg)
}

func verifyMessage(msg message.Message) *answer.Error {
	dataBytes, err := base64.URLEncoding.DecodeString(msg.Data)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to decode data: %v", err)
		return errAnswer.Wrap("verifyMessage")
	}

	publicKeySender, err := base64.URLEncoding.DecodeString(msg.Sender)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to decode public key: %v", err)
		return errAnswer.Wrap("verifyMessage")
	}

	signatureBytes, err := base64.URLEncoding.DecodeString(msg.Signature)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to decode signature: %v", err)
		return errAnswer.Wrap("verifyMessage")
	}

	err = schnorr.VerifyWithChecks(crypto.Suite, publicKeySender, dataBytes, signatureBytes)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to verify signature : %v", err)
		return errAnswer.Wrap("verifyMessage")
	}

	expectedMessageID := messagedata.Hash(msg.Data, msg.Signature)
	if expectedMessageID != msg.MessageID {
		errAnswer := answer.NewInvalidActionError("messageID is wrong: expected %s found %s",
			expectedMessageID, msg.MessageID)
		return errAnswer.Wrap("verifyMessage")
	}
	return nil
}
