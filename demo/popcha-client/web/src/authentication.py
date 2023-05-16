"""
This file includes all the authentication logic. No external server logic is
done here.
"""
from __future__ import annotations

import secrets
import time
from urllib import parse

import jwt
from jwt import PyJWTError
from werkzeug.datastructures import MultiDict


class Authentication:
    """
    Class that allows users to authenticate
    """

    def __init__(self):
        self.providers: list = []
        self.login_states: dict[(str, str, float)] = {}

    def get_url(self, server: str, lao_id: str, client_id: str) -> str:
        """
        Generates the url to contact the authentication server. It contains all
        the information required by the authentication server
        :param server: The current server domain
        :param lao_id: The LAO ID the user wants to connect with
        :param client_id: The unique identifier of this client
        :return: A url to the authentication server
        """
        nonce = secrets.token_urlsafe(64)
        state = secrets.token_urlsafe(64)
        self.login_states[state] = (nonce, server, time.time())
        parameters = {
            "response_mode": "query",
            "response_type": "id_token",
            "client_id": client_id,
            "redirect_uri": f"https://{server}/cb",
            "scope": "openid profile",
            "login_hint": lao_id,
            "nonce": nonce,
            "state": state
            }
        return f"https://{server}/authorize?{parse.urlencode(parameters)}"

    def validate_args(self, args: MultiDict[str, str], client_id: str) \
            -> str | None:
        """
        Validate the args as if they were coming from OpenID Authentication request
        :param args: A MultiDict containing the arguments (almost the same as a
        dict).
        :param client_id: The ID of this client
        :return: The unique user identifier if logged in None otherwise
        """

        missing_arg: bool = ("id_token" not in args or "token_type" not in
                             args or "state" not in args)
        if missing_arg:
            return None

        if args.get("state") not in self.login_states.keys():
            return
        nonce_data = self.login_states.pop(args.get("state"))
        server_pub_key = self.public_key_from_iss(nonce_data[1])
        try:
            token: dict = jwt.decode(
                jwt = args.get("id_token", type = str),
                key = server_pub_key,
                algorithms = ['RS256'],
                audience = client_id
                )
        except PyJWTError:
            return None

        valid_provided_nonce = (token.get("nonce") == nonce_data[0])
        if not valid_provided_nonce:
            return None

        valid_issuer = nonce_data[1] == token.get("iss")
        if not valid_issuer:
            return None
        user_id: str = token.get("sub")
        return f"{user_id}@{nonce_data[1]}"

    def public_key_from_iss(self, iss: str) -> str:
        """
        Returns the public key of a registered server.
        :param iss: The registered server
        :return: The public key
        """
        return ([p for p in self.providers if p.get("domain") == iss][0]).get(
                "public_key"
                )
