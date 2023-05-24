"""
This class contains the function related to the application data persitance
and  management
"""
import secrets
import time

from flask import render_template
from werkzeug.datastructures import MultiDict


class CounterApp:
    """
    An app that allows a user to count
    """

    def __init__(self):
        self.app_nonces: dict[str, (str, float)] = {}  # Maps nonce to user
        self.app_data: dict[str, int] = {}  # Maps users to counter values

    def get_new_login_params(self, user_id: str) -> dict[str, str]:
        """
        Returns a dict specifying the parameter to use for the app.
        :param user_id: The id of the newly logged-in user
        :returns: The arguments to use to connect to the app.
        """
        if user_id not in self.app_data.keys():
            self.app_data[user_id] = 0

        nonce: str = secrets.token_urlsafe(64)

        self.app_nonces[nonce] = (user_id, time.time())

        return {"nonce": nonce}

    def process(self, args: MultiDict) -> str:
        # Validates nonce
        """
         Computes the HTML code of the app given the provided args
        :param args: The arguments passed to the app
        :returns: The correct HTML code if the request is valid,
        it will raise an exception otherwise
        """
        valid_nonce: bool = (
                    "nonce" not in args or args.get("nonce", type = str)
                    not in self.app_nonces)

        if valid_nonce:
            raise ValueError("The nonce is not valid")
        user_id, _ = self.app_nonces.pop(args.get("nonce", type = str))

        # Updates the app data if needed
        if "action" in args and args.get("action", type = str) == "increment":
            self.app_data[user_id] += 1
        if "action" in args and args.get("action", type = str) == "decrement":
            self.app_data[user_id] -= 1

        # Computes the returned html code
        new_nonce = secrets.token_urlsafe(64)
        self.app_nonces[new_nonce] = (user_id, time.time())

        return render_template(
            'counterApp.html', nonce = new_nonce,
            counter = str(self.app_data[user_id])
            )
