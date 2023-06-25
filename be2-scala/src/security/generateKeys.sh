#!/bin/sh

# Generates public and private keys for scala pop server
# See https://stackoverflow.com/a/39311251 for details
openssl genrsa -out private_key.pem 2048
openssl pkcs8 -topk8 -inform PEM -outform DER -in private_key.pem -out private_key.der -nocrypt
openssl rsa -in private_key.pem -pubout -outform DER -out public_key.der

# Additionally generates a pem version of the public key for easier export
openssl rsa -in private_key.pem -pubout -outform PEM -out public_key.pem
