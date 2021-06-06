package validation

import (
	"embed"
	"fmt"
	"net/http"

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

//go:embed protocol
var protocolFS embed.FS

const (
	GenericMsgSchema string = "genericMsgSchema"
	DataSchema       string = "dataSchema"
)

// RegisterSchema adds a JSON schema specified by the path
func (s *SchemaValidator) RegisterSchema(fs http.FileSystem, schema schema) error {
	msgLoader := gojsonschema.NewReferenceLoaderFileSystem(schema.path, fs)
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
	genericMsgSchema := schema{
		name: GenericMsgSchema,
		path: "file:///protocol/genericMessage.json",
	}
	dataSchema := schema{
		name: DataSchema,
		path: "file:///protocol/query/method/message/data/data.json"}

	return NewSchemaValidatorWithSchemas(http.FS(protocolFS), genericMsgSchema, dataSchema)
}

// NewSchemaValidatorWithSchemas returns a Schema Validator for the schemas 'schemas'
func NewSchemaValidatorWithSchemas(fs http.FileSystem, schemas ...schema) (*SchemaValidator, error) {
	// Instantiate schema
	schemaValidator := &SchemaValidator{
		schemas: make(map[string]*gojsonschema.Schema),
	}

	// Register the paths
	for _, schema := range schemas {
		err := schemaValidator.RegisterSchema(fs, schema)
		if err != nil {
			return nil, xerrors.Errorf("failed to register a json schema: %v", err)
		}
	}

	return schemaValidator, nil
}
