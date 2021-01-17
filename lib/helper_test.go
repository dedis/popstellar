package lib

import (
	"testing"
)

func TestEscapeAndQuote(t *testing.T) {
	s1 := "hel\"lo1"
	exp := "\"hel\\\"lo1\""
	escaped := EscapeAndQuote(s1)
	if exp != escaped {
		t.Errorf("incorrect output for %s,  got %s ,but excpected :%s", s1, escaped, exp)
	}
	s2 := "hel\\lo2"
	exp = "\"hel\\\\lo2\""
	if exp != EscapeAndQuote(s2) {
		t.Errorf("incorrect output for %s excpected : %s", s2, exp)
	}
	s3 := ""
	exp = "\"\""
	if exp != EscapeAndQuote(s3) {
		t.Errorf("incorrect output for %s excpected : %s", s3, exp)
	}
}

func TestComputeAsJsonArray(t *testing.T) {
	s1 := "hel\"lo1"
	s2 := "hel\\lo2"
	s3 := ""
	s4 := "la vida"
	tab := []string{}
	tab = append(tab, s1, s2, s3, s4)
	expected := "[\"hel\\\"lo1\",\"hel\\\\lo2\",\"\",\"la vida\"]"
	if expected != ArrayRepresentation(tab) {
		t.Errorf("incorrect output for %s excpected : %s", s3, expected)
	}
}
