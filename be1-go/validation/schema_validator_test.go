package validation

import (
	"testing"

	"github.com/stretchr/testify/require"
)

func TestOrganizer_SchemaLoading(t *testing.T) {
	validator, err := NewSchemaValidator()
	require.NoError(t, err)
	require.Equal(t, 2, len(validator.schemas))
	require.NotNil(t, validator.schemas[GenericMsgSchema])
	require.NotNil(t, validator.schemas[DataSchema])
}
