"""
This file includes all the authentication logic. No external server logic is
done here.
"""
from __future__ import annotations

import secrets
import time
from urllib import parse

import jwt
from werkzeug.datastructures import MultiDict

valid_nonces: dict[(str, float)]


def get_url(server: str, lao_id: str, client_id: str) -> str:
    """
    Generates the url to contact the authentication server. It contains all
    the information required by the authentication server
    :param server: The current server domain
    :param lao_id: The LAO ID the user wants to connect with
    :param client_id: The unique identifier of this client
    :return: A url to the authentication server
    """
    global valid_nonces
    nonce = secrets.token_urlsafe(64)
    valid_nonces[nonce] = (server, time.time())
    parameters = {
        "response_mode": "query",
        "response_type": "id_token token",
        "client_id": client_id,
        "redirect_uri": f"https%3A%2F%2F{server}%2Fcb",
        "scope": "openid profile",
        "login_hint": lao_id,
        "nonce": nonce,
        }
    return f"https://{server}/authorize?{parse.urlencode(parameters)}"


def validate_args(args: MultiDict[str, str], public_key: str, client_id: str) \
        -> str | None:
    """
    Validate the args as if they were coming from OpenID Authentication request
    :param args: A MultiDict containing the arguments (almost the same as a
    dict).
    :param public_key:
    :param client_id:
    :return: The unique user identifier if logged in None otherwise
    """
    missing_arg: bool = ("access_token" not in args or "id_token" not in args
                         or "token_type" not in args)
    if missing_arg:
        return None
    token: dict = jwt.decode(args.get("id_token", type = str))

    valid_client_id = client_id == token.get("aud") == client_id
    if not valid_client_id:
        return None

    return None


def valid_login_url(args: MultiDict[str, str]) -> str:
    """
    Generates the url the user will be redirected to if they have a
    successful login.
    :param args: The arguments of the successful login request
    :return: The correct
    """
    return args.get()
