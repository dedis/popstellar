package util

import (
	"github.com/pkg/errors"
	"github.com/rs/zerolog"
	"github.com/rs/zerolog/log"
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

func GetSchemaValidatorInstance() (*validation.SchemaValidator, bool) {
	if instance == nil || instance.schemaValidator == nil {
		return nil, false
	}

	return instance.schemaValidator, true
}

func VerifyJSON(msg []byte, st validation.SchemaType) error {
	if instance == nil || instance.schemaValidator == nil {
		return errors.Errorf("schema validator doesn't exist")
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
