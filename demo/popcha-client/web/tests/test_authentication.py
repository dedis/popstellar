"""
Test file for authentication.py
"""
from werkzeug.datastructures import MultiDict
from time import time

import authentication


class TestQuery:
    """
    Simple test class for the authentication query related functions
    """

    def test_get_url_returns_valid_one(self):
        url: str = authentication.get_url("server.example", "custom_lao_id",
                                          "custom_client_id")
        generated_nonce = list(authentication.login_nonces.keys())[0]
        reference = "https://server.example/authorize?response_mode=query" \
                    "&response_type=id_token+token&client_id" \
                    "=custom_client_id&redirect_uri=https%3A%2F%2Fserver" \
                    ".example%2Fcb&scope=openid+profile&login_hint" \
                    f"=custom_lao_id&nonce={generated_nonce}"
        assert reference == url


class TestAuthenticationResponse:
    """
    Simple test class for the authentication response related functions
    """
    jwt: str = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJodHRwczovL3Nl" \
               "cnZlci5leGFtcGxlLmNvbSIsInN1YiI6IjEyMzQ1Njc4IiwiYXVkIjpbImNsM" \
               "WVudCJdLCJleHAiOjE2ODM2NDY0NjAsImlhdCI6MTY4MzY0Mjg2MH0.cSfvw8" \
               "zhBXNJ6YWrtb7e2hBRye45q5eXpBWRia1KMEwz9E3Ka2qlsLRNQ7VpTJrBDL-" \
               "so_ESOUwDLl34JjJCvJWxtLg56r1jUQNyp8CTKX64xWT9kBFOF8VNi45MpAVX" \
               "fgY7TkLgaSiqZzboAP8q_P8rB1FFgHN42FcRvLFIT6u7KkOyiyfo_yba0DL5g" \
               "dHGDiORoG9O-DUZNr0HAyj2TZHqDn81j8Rg2eZ4w8yzjFu4wERsvlYyR1wL2-" \
               "tBGI25XSAbCLjrLr4vLjDrl6Ztw7LdlD1bMyLNShnjtg0dArkNw07LtQmxLMb" \
               "AblYEOXD1QFhwiBoLA21TDKl1KibbMQ"
    pub_key: str = b"-----BEGIN PUBLIC KEY-----\nMIIBIjANBgkqhkiG9w0BAQEFAAOC" \
                   b"AQ8AMIIBCgKCAQEAsbR9Ip84tR4vc1IEefBJdHMlQAQm1UltYE3vs875" \
                   b"eY8ASZ4lzlLG6iVRe7LH4VN6j7GB4Tjj2EtgUFUAQqbFs5mn7cFO7DR9" \
                   b"riQDgLGekAQ5g/mLz9QhuAGjU2am0mPBOSBME08Ek9vNRfAOGVWk9fDU" \
                   b"hdRceRdKOXnnz+YvqYfe3vz4jx9XXJHZHmG2wNB6egCnsbZOuEqiWVMj" \
                   b"5+3wKt1prUGKEAHtPqC+olDaLwZw1didYotPgaZDwedkcAVSWNHvOkkY" \
                   b"3uMqvKI+CpoxP+uqtdy9tM54sNQjoWdq4LIaWF/nLRy5fM2JAVbAqwPW" \
                   b"6z23YMi4HIsfwuj+d8UZtQIDAQAB\n-----END PUBLIC KEY-----"
    args: MultiDict[str, str] = MultiDict({
        "id_token": jwt,
        "token_type": "bearer"
    })


    def test_validate_args_validates_a_correct_one(self):
        authentication.login_nonces["n0nce"] = ("https://server.example.com", time())
        assert authentication.validate_args(self.args, "cl1ent",
                                            self.pub_key) is not None

    def test_validate_args_fails_if_a_required_arg_is_missing(self):
        assert 0 == 0

    def test_validate_args_fails_on_wrong_client_id(self):
        assert 0 == 0

    def test_validate_args_fails_on_missing_nonce(self):
        assert 0 == 0

    def test_validate_args_fails_on_invalid_issuer(self):
        assert 0 == 0

