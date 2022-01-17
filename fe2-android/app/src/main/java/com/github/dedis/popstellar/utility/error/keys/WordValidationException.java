package com.github.dedis.popstellar.utility.error.keys;

public class WordValidationException extends KeyException {

  public WordValidationException(String words, Exception cause) {
    super("Unable to validate given words : " + words, cause);
  }
}
