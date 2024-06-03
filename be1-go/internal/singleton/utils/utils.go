package utils

import (
	"github.com/rs/zerolog"
	"github.com/rs/zerolog/log"
	"popstellar/internal/errors"
	"popstellar/internal/validation"
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

func VerifyJSON(msg []byte, st validation.SchemaType) error {
	if instance == nil || instance.schemaValidator == nil {
		return errors.NewInternalServerError("schema validator was not instantiated")
	}

	return instance.schemaValidator.VerifyJSON(msg, st)
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
