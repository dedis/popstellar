"""
Test file for counterApp.py
"""
import pytest
from werkzeug.datastructures import MultiDict

from src.counterApp import CounterApp
from src.main import app as flask_app


class TestCounterApp:
    """
    Test class for the Counter App
    """

    custom_user_id = "custom user id"

    @pytest.fixture
    def app(self):
        """
        Allows tests to get a fresh Counter app as a parameter
        """
        return CounterApp()

    def test_login_param_first_time(self, app):
        params = app.get_new_login_params(self.custom_user_id)

        assert "nonce" in params
        assert len(app.app_nonces) == 1
        assert len(app.app_data) == 1

    def test_login_param_already_registered(self, app):
        app.get_new_login_params(self.custom_user_id)
        params = app.get_new_login_params(self.custom_user_id)

        assert "nonce" in params
        assert len(app.app_nonces) == 2
        assert len(app.app_data) == 1

    def test_invalid_nonce(self, app):
        args = MultiDict({"nonce": "invalid nonce"})
        with pytest.raises(ValueError):
            app.process(args)

    def test_valid_nonce_incrementation(self, app):
        with flask_app.app_context():
            args = app.get_new_login_params("user id")
            args["action"] = "increment"
            html = app.process(MultiDict(args))

            assert 'Counter:<span class="bold"> 1 </span>' in html

    def test_valid_nonce_decrementation(self, app):
        with flask_app.app_context():
            args = app.get_new_login_params("user id")
            args["action"] = "decrement"
            html = app.process(MultiDict(args))

            assert 'Counter:<span class="bold"> -1 </span>' in html
