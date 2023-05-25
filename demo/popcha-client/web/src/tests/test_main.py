"""
Test file for main.py
"""
import pytest

from src import main
from src.authentication import Authentication

class TestMain:
    """
    Test class for the requests to the Flask app in main.py
    """
    providers = [{
        "domain": "valid.example",
        "lao_id": "1SZa21kpmmlkjuIJ2WqB9-C3v7GfU3dkga1yz1xuhdo=",
        "public_key": "d"
      }, 
      {
        "domain": "valid2.server.example:8000",
        "lao_id": "invalid.laod.id",
        "public_key": "ddd"
      },
        {
        "invalid": "provider"
        }]

    config = {
        "client_id": "rILOsh19WmpMrZKy2MTWy8vcwitQR0gGEmYQL1d-nis",
        "public_domain": "127.0.0.1",
        "public_port": 8000,
        "local_port": 8080
        }
    
    @pytest.fixture
    def client(self):
        """
        Fixture that allows to do get request to the server
        """
        main.config = self.config.copy()
        main.authenticationProvider= Authentication(self.providers.copy())
        main.app.config.update({
            "TESTING": True,
        })
        return main.app.test_client()
    
    def test_root_page_without_error(self, client):
        with main.app.app_context():
            response = client.get("/")
            assert (b'<option value="0">1SZa21kpmmlkjuIJ2WqB9-C3v7GfU3dkga1yz1x'
                    b'uhdo=@valid.example</option>' in response.data)
    
    def test_root_page_with_error(self, client):
        response = client.get("/", query_string={"error": "custom error"})
        assert (b'<p class="error">Error: custom error</p>' in response.data)
        
    def test_authentication_without_error(self, client):
        response = client.get("/authenticate",
                              query_string={"serverAndLaoId": 0})
        assert response.status_code == 302
        assert "response_mode=query" in response.location
        assert "https://valid.example/authorize?" in response.location
        assert "response_type=id_token" in response.location
        assert "scope=openid+profile" in response.location
        assert ("redirect_uri=https%3A%2F%2F127.0.0.1%3A8000%2Fcb" in
               response.location)
        assert ("login_hint=1SZa21kpmmlkjuIJ2WqB9-C3v7GfU3dkga1yz1xuhdo%3D" 
                in response.location)

    def test_validate_config_on_correct_one(self):
        config_modifications = main.validate_config(self.config)
        assert len(config_modifications) == 0

    def test_validate_config_on_missing_client_id(self):
        input_config = self.config.copy()
        input_config.pop("client_id")
        config_modifications = main.validate_config(input_config)
        assert len(config_modifications) == 1
        assert "client_id" in config_modifications

    def test_validate_config_on_missing_public_port(self):
        with pytest.raises(ValueError):
            input_config = self.config.copy()
            input_config.pop("public_port")
            main.validate_config(input_config)

    def test_validate_config_on_missing_public_domain(self):
        with pytest.raises(ValueError):
            input_config = self.config.copy()
            input_config.pop("public_domain")
            main.validate_config(input_config)

    def test_validate_config_on_missing_local_port(self):
        with pytest.raises(ValueError):
            input_config = self.config.copy()
            input_config.pop("local_port")
            main.validate_config(input_config)

    def test_filter_providers_correct_length(self):
        assert len(main.filter_providers(self.providers)) == 1