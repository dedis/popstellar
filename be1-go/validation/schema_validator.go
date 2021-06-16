package validation

import (
	"bytes"
	"embed"
	"encoding/base64"
	"fmt"
	"io"
	"io/fs"
	"log"
	"path/filepath"
	"strings"
	"student20_pop/message"

	"github.com/santhosh-tekuri/jsonschema/v3"
	"golang.org/x/xerrors"
)

type SchemaValidator struct {
	genericMessageSchema *jsonschema.Schema
	dataSchema           *jsonschema.Schema
}

type SchemaType int

const (
	GenericMessage SchemaType = 0
	Data           SchemaType = 1
)

const baseUrl = "https://raw.githubusercontent.com/dedis/student_21_pop/master/"

//go:embed protocol
var protocolFS embed.FS

func init() {
	// Override the defaults for loading files and decoding base64 encoded
	// data
	jsonschema.Loaders["file"] = loadFileURL
	jsonschema.Decoders["base64"] = base64.URLEncoding.DecodeString
}

// VerifyJson verifies that the `msg` follow the schema protocol of name 'schemaName',
// it returns an error if it is not the case.
func (s *SchemaValidator) VerifyJson(msg []byte, st SchemaType) error {
	reader := bytes.NewBuffer(msg[:])
	var schema *jsonschema.Schema

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

	err := schema.Validate(reader)
	if err != nil {
		log.Printf("failed to validate schema: %v", err)
		return &message.Error{
			Code:        -4,
			Description: "failed to validate schema",
		}
	}

	return nil
}

// NewSchemaValidator returns a Schema Validator
func NewSchemaValidator() (*SchemaValidator, error) {
	gmCompiler := jsonschema.NewCompiler()
	dataCompiler := jsonschema.NewCompiler()

	// recurse over the protocol directory and load all the files
	err := fs.WalkDir(protocolFS, "protocol", func(path string, d fs.DirEntry, err error) error {
		if err != nil {
			return err
		}

		if filepath.Ext(path) != ".json" {
			return nil
		}

		file, err := protocolFS.Open(path)
		if err != nil {
			return xerrors.Errorf("failed to open schema: %v", err)
		}
		defer file.Close()

		url := baseUrl + path
		if strings.HasPrefix(path, "protocol/query/method/message/data") {
			dataCompiler.AddResource(url, file)
		} else {
			gmCompiler.AddResource(url, file)
		}

		return nil
	})

	if err != nil {
		return nil, xerrors.Errorf("failed to load schema: %v", err)
	}

	// read genericMessage.json
	gmSchema, err := gmCompiler.Compile("file://protocol/genericMessage.json")
	if err != nil {
		return nil, xerrors.Errorf("failed to compile validator: %v", err)
	}

	// read data.json
	dataSchema, err := dataCompiler.Compile("file://protocol/query/method/message/data/data.json")
	if err != nil {
		return nil, xerrors.Errorf("failed to compile validator: %v", err)
	}

	return &SchemaValidator{
		genericMessageSchema: gmSchema,
		dataSchema:           dataSchema,
	}, nil
}

func loadFileURL(s string) (io.ReadCloser, error) {
	path := strings.TrimPrefix(s, "file://")

	return protocolFS.Open(path)
}
