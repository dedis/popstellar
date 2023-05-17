"""
Test file for authentication.py
"""
import jwt
import pytest
from werkzeug.datastructures import MultiDict
from time import time
from freezegun import freeze_time

from src.authentication import Authentication


class TestQuery:
    """
    Simple test class for the authentication query related functions
    """

    def test_get_url_returns_valid_one(self):
        authentication = Authentication()
        url: str = authentication.get_url(
                "server.example", "custom_lao_id",
                "127.0.0.1",
                8000,
                "custom_client_id"
                )
        generated_state = list(authentication.login_states.keys())[0]
        generated_nonce = authentication.login_states.get(generated_state)[0]
        reference = "https://server.example/authorize?response_mode=query" \
                    "&response_type=id_token&client_id=custom_client_id&" \
                    "redirect_uri=https%3A%2F%2F127.0.0.1%3A8000%2Fcb&scope=" \
                    "openid+profile&login_hint=custom_lao_id&nonce=" \
                    f"{generated_nonce}&state={generated_state}"
        assert reference == url


@freeze_time("2023-05-10 09:00:00")
class TestAuthenticationResponse:
    """
    Simple test class for the authentication response related functions
    """
    # This JWT has been provided by be1 for cross-validation
    be1_go_jwt: str = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9" \
               ".eyJhdWQiOiJjSUQxMjJkdyIs" \
               "ImF1dGhfdGltZSI6MTY4MzcwNzM4OSwiZXhwIjoxNjgzNzEwOTg5LCJpYXQiO" \
               "jE2ODM3MDczODksImlzcyI6Imh0dHBzOi8vc2VydmVyLmV4YW1wbGUuY29tIi" \
               "wibm9uY2UiOiJuMG5jMyIsInN1YiI6InBwaWQxMjU2NCJ9.Wx5VJQ1ASRd-bp" \
               "SRZUdRRfSRmVcML4_pKmBbNYHCbvNUJCUiOin7MVn_9XYtZG0Dsi0SXovWAqt" \
               "xZnm5E-z4ptUAn5LWHfA6DQaCYY0q7YjQige-7RCacvGhdTCTvWGvb8g-S5tV" \
               "BMjtxnh602Y7YkmWv1jeN-qcmgdavszmFLdYmln77kifvN26XXoV3X-x2mm44" \
               "6KJeBk_Voa3ytK7do4AdmcP3uBxo6DGA2C2HNfO44z7WSyNgLtok1IxS3PNcn" \
               "DU-DiPeXUNK3wkoiwjNHSBwWCxXffn1Lqfa2A02Pnf4Avz2XIXrmYJWwMUEiy" \
               "HhP5t30JdzwV2XthROJcnQg"

    priv_key: str = b"-----BEGIN PRIVATE " \
                    b"KEY-----\nMIIEvgIBADANBgkqhkiG9w0BAQE" \
                    b"FAASCBKgwggSkAgEAAoIBAQCxtH0inzi1Hi9zUgR58El0cyVABCbVSW" \
                    b"1gTe+zzvl5jwBJniXOUsbqJVF7ssfhU3qPsYHhOOPYS2BQVQBCpsWzm" \
                    b"aftwU7sNH2uJAOAsZ6QBDmD+YvP1CG4AaNTZqbSY8E5IEwTTwST281F" \
                    b"8A4ZVaT18NSF1Fx5F0o5eefP5i+ph97e/PiPH1dckdkeYbbA0Hp6AKe" \
                    b"xtk64SqJZUyPn7fAq3WmtQYoQAe0+oL6iUNovBnDV2J1ii0+BpkPB52" \
                    b"RwBVJY0e86SRje4yq8oj4KmjE/66q13L20zniw1COhZ2rgshpYX+ctH" \
                    b"Ll8zYkBVsCrA9brPbdgyLgcix/C6P53xRm1AgMBAAECggEAAdcYpKR5" \
                    b"ddwGKcTjqaTvXcwDdWeVmgfUoMwDJh2H6oEB7mvmKv4jHobxvRIw4l3" \
                    b"MRciqIPxHKmo9aReNlSMc+6slA1/zwpvDNsEbYth0B+cYoWE9gr1zoU" \
                    b"WDEiNcqY5sOyc2d8y4WL+h9I4egeynyf7gdIeqctE8QjQc+RjXzYLyQ" \
                    b"VBLagczxLUtVD2UH6Irdgy4VI1GkevAQU8Eso6cCAfmhOiHepi5nx7h" \
                    b"hZTL6fxBz9BkFSNhdu40Kibwr3tDGGVms3c31hqyjUNtA+6Nhe/beYa" \
                    b"/FZ5QB35VbuculbytXUOnxIXQ2+kgHNl+X15/YVuooJNE4XZHJT2u3r" \
                    b"ts0QKBgQDEt9rNW6cnz7nD5GrsR3Z1bieKzICacfnPS6dINIGiNGThq" \
                    b"bWiHpuZVkMu+SKPbVAxdh4sMrqmiyuFW/wYbv8W0O5kRI3hkaRxhExn" \
                    b"GU6iG5m0GbBvDg3fDG9B569npGzoYGhBDT2CVEUd54razGovXveqB2G" \
                    b"eRDJfRU/6kQluwwKBgQDnQdVyPw5kZMEvWQ5KxW/86pXTfP8210t6Ip" \
                    b"Dyvr/SwC2i3i83vQQruukjWll8AIQHhR8SDlHxBK/k/aTR4VvEdhkyH" \
                    b"T4+2Z4JNBGVzT7wh2IHMDUhP5/KP+tSbAhtArWw/Bv5i+lBkmi5UYVG" \
                    b"nOOOdH6jpGGFs/LtbzPhi1Y+JwKBgApxVB0oq2PypALhIkfutzwen9y" \
                    b"/ZGhOeptlgbjUiLkqnNxZ3PmBNHNcX+6jbRE+FU663XktLDlhE+tdab" \
                    b"GGWuZEKxOJjBqYV6lrA39JmaIDYxJrdrE+hr/7cgCGowoWcW2YiJBDe" \
                    b"qtre8vNmdJpnY1sNiuBfs4fAqmKDWfYwS5vAoGBAJW3lHWjhzDN3hhG" \
                    b"Qq97xXXrddZ23U/m8LGAwXC2t7+8tY7044Lld1bManV93+Mc/l1T/Pq" \
                    b"WlMxCKZJJ+DP8/4lgoA1Gy26rOtpggGYIfBACxh87QZpl85Bf83zn/k" \
                    b"h88Z5EieP0ha3zGKOpuGwv1E788qQFHzINf1/il6cUq2APAoGBAKntL" \
                    b"R+/m/rwfGUXj3obZua6zW7jv31/3v/Beefr+swMX4PyKP2UNhsZpk9w" \
                    b"/lFpssGWTJu8R7AwjBJLxdRl6ugWAxojAtVWd/lQEq2IUCejLLjX4s1" \
                    b"5syVAd46rnEyc62SCvPEWnjhQy6RzP/+qzSHxRl1I8Q+19G/PziUDCi" \
                    b"cK\n-----END PRIVATE KEY-----"

    pub_key: str = b"-----BEGIN PUBLIC KEY-----\nMIIBIjANBgkqhkiG9w0BAQEFAAOC" \
                   b"AQ8AMIIBCgKCAQEAsbR9Ip84tR4vc1IEefBJdHMlQAQm1UltYE3vs875" \
                   b"eY8ASZ4lzlLG6iVRe7LH4VN6j7GB4Tjj2EtgUFUAQqbFs5mn7cFO7DR9" \
                   b"riQDgLGekAQ5g/mLz9QhuAGjU2am0mPBOSBME08Ek9vNRfAOGVWk9fDU" \
                   b"hdRceRdKOXnnz+YvqYfe3vz4jx9XXJHZHmG2wNB6egCnsbZOuEqiWVMj" \
                   b"5+3wKt1prUGKEAHtPqC+olDaLwZw1didYotPgaZDwedkcAVSWNHvOkkY" \
                   b"3uMqvKI+CpoxP+uqtdy9tM54sNQjoWdq4LIaWF/nLRy5fM2JAVbAqwPW" \
                   b"6z23YMi4HIsfwuj+d8UZtQIDAQAB\n-----END PUBLIC KEY-----"
    domain = "https://server.example.com"
    client_id = "cID122dw"

    claimset = {
        "aud": client_id,
        "auth_time": 1683707389,
        "exp": 1683710989,
        "iat": 1683707389,
        "iss": domain,
        "nonce": "n0nc3",
        "sub": "ppid12564"
        }

    def get_args(self, jwt):
        """
        No doc
        """
        return MultiDict(
                {
                    "id_token": jwt,
                    "token_type": "bearer",
                    "state": "stat3"
                    }
                )

    @pytest.fixture
    def authentication(self):
        """
        Simple fixture
        """
        authentication = Authentication()
        authentication.providers = [{
            "domain": self.domain,
            "lao_id": "mmm",
            "public_key": self.pub_key
            }]
        authentication.login_states["stat3"] = ("n0nc3",
                                                self.domain,
                                                time())
        return authentication

    def test_validate_args_validates_a_correct_one(self, authentication):
        encoded = jwt.encode(
                self.claimset, self.priv_key,
                algorithm = "RS256"
                )
        assert (authentication.validate_args(
                self.get_args(encoded),
                self.client_id
                )
                == 'ppid12564@https://server.example.com')

    def test_validate_other_systems_token(self, authentication):
        assert (authentication.validate_args(
                self.get_args(self.be1_go_jwt),
                self.client_id
                )
                == 'ppid12564@https://server.example.com')

    def test_validate_fails_on_missing_required_arg(self, authentication):
        encoded = jwt.encode(
                self.claimset, self.priv_key,
                algorithm = "RS256"
                )
        args = self.get_args(encoded).pop("token_type")
        assert (authentication.validate_args(args, self.client_id) is None)

    def test_validate_args_fails_on_wrong_state(self, authentication):
        encoded = jwt.encode(self.claimset, self.priv_key, algorithm = "RS256")
        args = self.get_args(encoded)
        args["state"] = "wrong state"
        assert (authentication.validate_args(
                args,
                self.client_id
                )
                is None)

    def test_validate_args_fails_on_altered_audience(self, authentication):
        claimset = self.claimset.copy()
        claimset["aud"] = "altered audience"
        encoded = jwt.encode(claimset, self.priv_key, algorithm = "RS256")
        assert (authentication.validate_args(
                self.get_args(encoded),
                self.client_id
                )
                is None)

    def test_validate_args_fails_on_altered_nonce(self, authentication):
        claimset = self.claimset.copy()
        claimset["nonce"] = "wrong nonce"
        encoded = jwt.encode(claimset, self.priv_key, algorithm = "RS256")
        assert (authentication.validate_args(
                self.get_args(encoded),
                self.client_id
                )
                is None)

    def test_validate_args_fails_on_invalid_issuer(self, authentication):
        claimset = self.claimset.copy()
        claimset["iss"] = "invalid issuer"
        encoded = jwt.encode(claimset, self.priv_key, algorithm = "RS256")
        assert (authentication.validate_args(
                self.get_args(encoded),
                self.client_id
                )
                is None)
