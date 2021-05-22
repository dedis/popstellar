package validation

import (
	"testing"

	"github.com/stretchr/testify/require"
)

func TestOrganizer_SchemaLoading(t *testing.T) {
	_, err := NewSchemaValidator()
	require.NoError(t, err)
}
