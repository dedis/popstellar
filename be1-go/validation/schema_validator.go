package validation

import (
	"embed"
	"fmt"
	"io/fs"
	"net/http"
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

const baseUrl = "https://raw.githubusercontent.com/dedis/student_21_pop/master/"

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
			return fs.SkipDir
		}

		if filepath.Ext(path) != ".json" {
			return nil
		}

		file, err := protocolFS.Open(path)
		if err != nil {
			return xerrors.Errorf("failed to open schema: %v", err)
		}
		defer file.Close()

		// gojsonschema computes relative urls in "$ref" using the "$id" field.
		// We pre-populate the loader with files from the local filesystem
		// instead.
		jsonLoader, _ := gojsonschema.NewReaderLoader(file)
		url := baseUrl + path
		if strings.HasPrefix(path, "protocol/query/method/message/data") {
			dataLoader.AddSchema(url, jsonLoader)
		} else {
			gmLoader.AddSchema(url, jsonLoader)
		}

		return nil
	})

	if err != nil {
		return nil, xerrors.Errorf("failed to load schema: %v", err)
	}

	// read genericMessage.json
	gmSchemaLoader := gojsonschema.NewReferenceLoaderFileSystem("file:///protocol/genericMessage.json", http.FS(protocolFS))
	gmSchema, err := gmLoader.Compile(gmSchemaLoader)
	if err != nil {
		return nil, xerrors.Errorf("failed to compile validator: %v", err)
	}

	// read data.json
	dataSchemaLoader := gojsonschema.NewReferenceLoaderFileSystem("file:///protocol/query/method/message/data/data.json", http.FS(protocolFS))
	dataSchema, err := dataLoader.Compile(dataSchemaLoader)
	if err != nil {
		return nil, xerrors.Errorf("failed to compile validator: %v", err)
	}

	return &SchemaValidator{
		genericMessageSchema: gmSchema,
		dataSchema:           dataSchema,
	}, nil
}
