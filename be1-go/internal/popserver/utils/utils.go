package utils

import (
	"github.com/rs/zerolog"
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

func GetLogInstance() (*zerolog.Logger, bool) {
	if instance == nil || instance.log == nil {
		return nil, false
	}

	return instance.log, true
}

func GetSchemaValidatorInstance() (*validation.SchemaValidator, bool) {
	if instance == nil || instance.schemaValidator == nil {
		return nil, false
	}

	return instance.schemaValidator, true
}
