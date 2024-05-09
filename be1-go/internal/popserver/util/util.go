package util

import (
	"github.com/rs/zerolog"
	"github.com/rs/zerolog/log"
	"popstellar/message/answer"
	"popstellar/validation"
	"sync"
)

var once sync.Once
var instance *utils

type utils struct {
	log             *zerolog.Logger
	schemaValidator *validation.SchemaValidator
}

func InitUtils(log *zerolog.Logger, schemaValidator *validation.SchemaValidator) {
	once.Do(func() {
		instance = &utils{
			log:             log,
			schemaValidator: schemaValidator,
		}
	})
}

func VerifyJSON(msg []byte, st validation.SchemaType) *answer.Error {
	if instance == nil || instance.schemaValidator == nil {
		return answer.NewInternalServerError("schema validator was not instantiated").Wrap("VerifyJSON")
	}

	err := instance.schemaValidator.VerifyJSON(msg, st)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("invalid json: %v", err).Wrap("VerifyJSON")
		return errAnswer
	}

	return nil
}

func LogInfo(msg string) {
	if instance == nil || instance.log == nil {
		return
	}

	instance.log.Info().Msg(msg)
}

func LogError(err error) {
	if instance == nil || instance.log == nil {
		return
	}

	log.Error().Msg(err.Error())
}
