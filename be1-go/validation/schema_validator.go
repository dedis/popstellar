package validation

import (
	"fmt"
	"path/filepath"

	"student20_pop/message"

	"github.com/xeipuuv/gojsonschema"
	"golang.org/x/xerrors"
)

type SchemaValidator struct {
	schemas map[string]*gojsonschema.Schema
}

type schema struct {
	name string
	path string
}

const (
	GenericMsgSchema string = "genericMsgSchema"
	DataSchema       string = "dataSchema"
)

// RegisterSchema adds a JSON schema specified by the path
func (s *SchemaValidator) RegisterSchema(schema schema) error {
	msgLoader := gojsonschema.NewReferenceLoader(schema.path)
	msgSchema, err := gojsonschema.NewSchema(msgLoader)
	if err != nil {
		return xerrors.Errorf("failed to load the json schema with path `%s`: %v", schema.path, err)
	}

	s.schemas[schema.name] = msgSchema
	return nil
}

func (s *SchemaValidator) VerifyJson(byteMessage []byte, schemaName string) error {
	// Validate the Json "byteMessage" with a schema
	messageLoader := gojsonschema.NewBytesLoader(byteMessage)
	resultErrors, err := s.schemas[schemaName].Validate(messageLoader)
	if err != nil {
		return &message.Error{
			Code:        -1,
			Description: err.Error(),
		}
	}

	errorsList := resultErrors.Errors()
	descriptionErrors := ""
	// Concatenate all error descriptions
	for index, e := range errorsList {
		descriptionErrors += fmt.Sprintf(" (%d) %s", index+1, e.Description())
	}

	if len(errorsList) > 0 {
		return &message.Error{
			Code:        -1,
			Description: descriptionErrors,
		}
	}

	return nil
}

func NewSchemaValidator() (*SchemaValidator, error) {
	// Import the Json schemas defined in the protocol section
	protocolPath, err := filepath.Abs("../protocol")
	if err != nil {
		return nil, xerrors.Errorf("failed to load the path for the json schemas: %v", err)
	}
	protocolPath = "file://" + protocolPath

	genericMsgSchema := schema{GenericMsgSchema, protocolPath + "/genericMessage.json"}
	dataSchema := schema{DataSchema, protocolPath + "/query/method/message/data/data.json"}

	return NewSchemaValidatorWithSchemas(genericMsgSchema, dataSchema)
}

func NewSchemaValidatorWithSchemas(schemas ...schema) (*SchemaValidator, error) {
	// Instantiate schema
	schemaValidator := &SchemaValidator{
		schemas: make(map[string]*gojsonschema.Schema),
	}

	// Register the paths
	for _, schema := range schemas {
		schemaValidator.RegisterSchema(schema)
	}

	return schemaValidator, nil
}
