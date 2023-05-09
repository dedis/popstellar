# RSA Key pair for the PoPCHA system

## Use

The RSA key pair is used in the PoPCHA protocol for signing and
verifying JWTs (Json Web Tokens using RS256). These JWTs define the format
of the Id Token transmitted from the pop backend to an external client in the
authentication process.

The specifications of the JWT with RS256 can be found at https://datatracker.ietf.org/doc/html/rfc7519#section-8.

## Generation

The following commands have been used to generate the key pair:
* ``openssl genrsa -out popcha.rsa 2048`` 
* ``openssl rsa -in popcha.rsa -pubout > popcha.rsa.pub``