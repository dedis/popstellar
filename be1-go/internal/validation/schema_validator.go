package validation

import (
	"bytes"
	"embed"
	"encoding/base64"
	"github.com/santhosh-tekuri/jsonschema/v3"
	"golang.org/x/xerrors"
	"io"
	"io/fs"
	"path/filepath"
	"popstellar/internal/errors"
	"strings"
)

// SchemaValidator is used to validate JSON-RPC schemas.
type SchemaValidator struct {
	genericMessageSchema *jsonschema.Schema
	dataSchema           *jsonschema.Schema
}

// SchemaType denotes the type of schema.
type SchemaType int

const (
	// GenericMessage denotes the Generic Message schema.
	GenericMessage SchemaType = 0

	// Data denotes the Data schema.
	Data SchemaType = 1
)

// baseUrl is the baseUrl for all schemas.
const baseURL = "https://raw.githubusercontent.com/dedis/popstellar/master/"

// protocolFS is an embedded file system which allows us to bake the schemas
// into the binary during compilation. Since Go doesn't allow embedded files
// outside the module we copy the schemas over right before invoking the build
// or test commands in the Makefile. As a result, users may safely ignore the
// warnings on this line.
//
//go:embed protocol
var protocolFS embed.FS

func init() {
	// Override the defaults for loading files and decoding base64 encoded data
	jsonschema.Loaders["file"] = loadFileURL
	jsonschema.Decoders["base64"] = base64.URLEncoding.DecodeString
	jsonschema.Loaders["https"] = loadHttpURL
}

// VerifyJSON verifies that the `msg` follow the schema protocol of name
// 'schemaName', it returns an error otherwise.
func (s SchemaValidator) VerifyJSON(msg []byte, st SchemaType) error {
	reader := bytes.NewBuffer(msg[:])
	var schema *jsonschema.Schema

	switch st {
	case GenericMessage:
		schema = s.genericMessageSchema
	case Data:
		schema = s.dataSchema
	default:
		return errors.NewInternalServerError("unsupported schema type: %v", st)
	}

	err := schema.Validate(reader)
	if err != nil {
		return errors.NewInvalidMessageFieldError("failed to validate schema: %v", err)
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

		if d.IsDir() || filepath.Ext(path) != ".json" {
			return nil
		}

		file, err := protocolFS.Open(path)
		if err != nil {
			return xerrors.Errorf("failed to open schema: %v", err)
		}
		defer file.Close()

		url := baseURL + path
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

	// read jsonRPC.json
	gmSchema, err := gmCompiler.Compile("file://protocol/jsonRPC.json")
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

func loadHttpURL(s string) (io.ReadCloser, error) {
	// If for example a message data use a $ref to ../message.json,
	// it will try to load using the baseURL => replace path,
	// so that only local file is loaded.
	return loadFileURL(strings.ReplaceAll(s, baseURL, "file://"))
}
