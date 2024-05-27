#!/bin/bash

# This script generates a pair of public and private RSA keys that can be used by the scala pop server.
# Without any arguments, the script will generate the keys in the folder it is ran in.
# If "-test" is specified as the first argument, the keys will be generated in a folder named "test"
# openssl must be available on the system to generate the keys

testFolder="test"
testRequested=false

# Moves to test folder if "-test" was given in argument
if [ $# -ge 1 ]; then
  if [ "$1" == "-test" ]; then
    if [ ! -d $testFolder ]; then
      mkdir $testFolder
    fi
    cd $testFolder || exit
    testRequested=true
  else
    echo "Unrecognised argument $1"
    exit
  fi
fi

# Generates public and private keys for scala pop server
# See https://stackoverflow.com/a/39311251 for details
openssl genrsa -out private_key.pem 2048
openssl pkcs8 -topk8 -inform PEM -outform DER -in private_key.pem -out private_key.der -nocrypt
openssl rsa -in private_key.pem -pubout -outform DER -out public_key.der

# Additionally generates a pem version of the public key for easier export
openssl rsa -in private_key.pem -pubout -outform PEM -out public_key.pem

# Moves back to the initial folder if needed
if [ $testRequested ]; then
  cd ..
fi
