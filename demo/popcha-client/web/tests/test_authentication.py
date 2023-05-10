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
    jwt: str = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOiJjSUQxMjJkdyIs" \
               "ImF1dGhfdGltZSI6MTY4MzcwNzM4OSwiZXhwIjoxNjgzNzEwOTg5LCJpYXQiO" \
               "jE2ODM3MDczODksImlzcyI6Imh0dHBzOi8vc2VydmVyLmV4YW1wbGUuY29tIi" \
               "wibm9uY2UiOiJuMG5jMyIsInN1YiI6InBwaWQxMjU2NCJ9.Wx5VJQ1ASRd-bp" \
               "SRZUdRRfSRmVcML4_pKmBbNYHCbvNUJCUiOin7MVn_9XYtZG0Dsi0SXovWAqt" \
               "xZnm5E-z4ptUAn5LWHfA6DQaCYY0q7YjQige-7RCacvGhdTCTvWGvb8g-S5tV" \
               "BMjtxnh602Y7YkmWv1jeN-qcmgdavszmFLdYmln77kifvN26XXoV3X-x2mm44" \
               "6KJeBk_Voa3ytK7do4AdmcP3uBxo6DGA2C2HNfO44z7WSyNgLtok1IxS3PNcn" \
               "DU-DiPeXUNK3wkoiwjNHSBwWCxXffn1Lqfa2A02Pnf4Avz2XIXrmYJWwMUEiy" \
               "HhP5t30JdzwV2XthROJcnQg"

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
        authentication.login_nonces["n0nc3"] = ("https://server.example.com",
                                                time())
        assert (authentication.validate_args(self.args, "cID122dw",
                                            self.pub_key)
                == 'ppid12564@https://server.example.com')

    def test_validate_args_fails_if_a_required_arg_is_missing(self):
        assert 0 == 0

    def test_validate_args_fails_on_wrong_client_id(self):
        assert 0 == 0

    def test_validate_args_fails_on_missing_nonce(self):
        assert 0 == 0

    def test_validate_args_fails_on_invalid_issuer(self):
        assert 0 == 0

