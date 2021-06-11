package validation

import (
	"embed"
	"fmt"
	"io/fs"
	"path/filepath"
	"strings"
	"student20_pop/message"

	"github.com/xeipuuv/gojsonschema"
	"golang.org/x/xerrors"
)

type SchemaValidator struct {
	genericMessageSchema *gojsonschema.Schema
	dataSchema           *gojsonschema.Schema
}

type SchemaType int

const (
	GenericMessage SchemaType = 0
	Data           SchemaType = 1
)

//go:embed protocol
var protocolFS embed.FS

// VerifyJson verifies that the `msg` follow the schema protocol of name 'schemaName',
// it returns an error if it is not the case.
func (s *SchemaValidator) VerifyJson(msg []byte, st SchemaType) error {
	messageLoader := gojsonschema.NewBytesLoader(msg)
	var schema *gojsonschema.Schema

	switch st {
	case GenericMessage:
		schema = s.genericMessageSchema
	case Data:
		schema = s.dataSchema
	default:
		return &message.Error{
			Code:        -6,
			Description: fmt.Sprintf("unsupported schema type: %v", st),
		}
	}

	result, err := schema.Validate(messageLoader)
	if err != nil {
		return &message.Error{
			Code:        -4,
			Description: fmt.Sprintf("failed to validate schema: %s", err.Error()),
		}
	}

	errorsList := result.Errors()
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
	gmLoader := gojsonschema.NewSchemaLoader()
	dataLoader := gojsonschema.NewSchemaLoader()

	// recurse over the protocol directory and load all the files
	err := fs.WalkDir(protocolFS, "protocol", func(path string, d fs.DirEntry, err error) error {
		if err != nil {
			return err
		}

		if d.IsDir() {
			return nil
		}

		if filepath.Ext(path) != "json" {
			return nil
		}

		file, err := protocolFS.Open(path)
		if err != nil {
			return xerrors.Errorf("failed to open schema: %v", err)
		}
		defer file.Close()

		jsonLoader, _ := gojsonschema.NewReaderLoader(file)

		if strings.HasPrefix(path, "protocol/query/method/message/data") {
			dataLoader.AddSchema(path, jsonLoader)
		} else {
			gmLoader.AddSchema(path, jsonLoader)
		}

		return nil
	})

	if err != nil {
		return nil, xerrors.Errorf("failed to load schema: %v", err)
	}

	// read genericMessage.json
	gmBuf, err := protocolFS.ReadFile("protocol/genericMessage.json")
	if err != nil {
		return nil, xerrors.Errorf("failed to read root schema: %v", err)
	}

	gmSchemaLoader := gojsonschema.NewBytesLoader(gmBuf)
	gmSchema, err := gmLoader.Compile(gmSchemaLoader)
	if err != nil {
		return nil, xerrors.Errorf("failed to compile validator: %v", err)
	}

	// read data.json
	dataBuf, err := protocolFS.ReadFile("protocol/query/method/message/data/data.json")
	if err != nil {
		return nil, xerrors.Errorf("failed to read root schema: %v", err)
	}

	dataSchemaLoader := gojsonschema.NewBytesLoader(dataBuf)
	dataSchema, err := dataLoader.Compile(dataSchemaLoader)
	if err != nil {
		return nil, xerrors.Errorf("failed to compile validator: %v", err)
	}

	return &SchemaValidator{
		genericMessageSchema: gmSchema,
		dataSchema:           dataSchema,
	}, nil
}
