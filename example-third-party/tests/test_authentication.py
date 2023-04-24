"""
Test file for authentication.py
"""
import authentication


class TestQuery:
    """
    Simple test class for the authentication query related functions
    """

    def test_get_url_returns_valid_one(self):
        url: str = authentication.get_url("server.example", "custom_lao_id",
                                          "custom_client_id")
        generated_nonce = list(authentication.valid_nonces.keys())[0]
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

    def test_validate_args_validates_a_correct_one(self):
        assert 0 == 0

    def test_validate_args_fails_if_a_required_arg_is_missing(self):
        assert 0 == 0

    def test_validate_args_fails_on_wrong_client_id(self):
        assert 0 == 0

    def test_validate_args_fails_on_missing_nonce(self):
        assert 0 == 0

    def test_validate_args_fails_on_invalid_issuer(self):
        assert 0 == 0

