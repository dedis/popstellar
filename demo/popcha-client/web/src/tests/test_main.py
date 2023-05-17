"""
Test file for main.py
"""
import pytest

from src import main

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
        "lao_id": "5ie-bfxMwsqCtZRY1WuQzhturCoQ3MIvCyX5yAhsyvQ=",
        "public_key": "ddd"
      }]

    config = {
        "client_id": "rILOsh19WmpMrZKy2MTWy8vcwitQR0gGEmYQL1d-nis",
        "host_url": "127.0.0.1",
        "host_port": 8000
        }
    
    @pytest.fixture
    def client(self):
        """
        Fixture that allows to do get request to the server
        """
        main.config = self.config.copy()
        main.authenticationProvider.providers = self.providers.copy()
        main.app.config.update({
            "TESTING": True,
        })
        return main.app.test_client()
    
    def test_root_page_without_error(self, client):
        with main.app.app_context():
            response = client.get("/")
            assert (b'<option value="1">5ie-bfxMwsqCtZRY1WuQzhturCoQ3MIvCyX5yAh'
                    b'syvQ=@valid2.server.example:8000</option>' in response.data)
    
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