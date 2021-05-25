package validation

import (
	"fmt"
	"path/filepath"
	"strings"

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

// VerifyJson verifies that the 'byteMessage' follow the schema protocol of name 'schemaName',
// it returns an error if it is not the case.
func (s *SchemaValidator) VerifyJson(byteMessage []byte, schemaName string) error {
	// Validate the Json "byteMessage" with a schema
	messageLoader := gojsonschema.NewBytesLoader(byteMessage)
	resultErrors, err := s.schemas[schemaName].Validate(messageLoader)
	if err != nil {
		return &message.Error{
			Code:        -4,
			Description: fmt.Sprintf("failed to validate schema: %s", err.Error()),
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
			Code:        -4,
			Description: descriptionErrors,
		}
	}

	return nil
}

// NewSchemaValidator returns a Schema Validator
func NewSchemaValidator() (*SchemaValidator, error) {
	// Import the Json schemas defined in the protocol section
	protocolPath, err := filepath.Abs("../protocol")
	if err != nil {
		return nil, xerrors.Errorf("failed to load the path for the json schemas: %v", err)
	}

	// Replace the '\\' from windows path with '/'
	protocolPath = strings.ReplaceAll(protocolPath, "\\", "/")

	protocolPath = "file://" + protocolPath

	genericMsgSchema := schema{
		name: GenericMsgSchema,
		path: protocolPath + "/genericMessage.json",
	}
	dataSchema := schema{
		name: DataSchema,
		path: protocolPath + "/query/method/message/data/data.json"}

	return NewSchemaValidatorWithSchemas(genericMsgSchema, dataSchema)
}

// NewSchemaValidatorWithSchemas returns a Schema Validator for the schemas 'schemas'
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
