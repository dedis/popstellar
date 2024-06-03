package utils

import (
	"popstellar/internal/errors"
	"popstellar/internal/validation"
	"sync"
)

var once sync.Once
var instance *utils

type utils struct {
	schemaValidator *validation.SchemaValidator
}

func InitUtils(schemaValidator *validation.SchemaValidator) {
	once.Do(func() {
		instance = &utils{
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
