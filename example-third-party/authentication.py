"""
This file includes all the authentication logic. No external server logic is
done here.
"""
import secrets
import time
from urllib import parse

valid_nonces: list[(str, float)]


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
    nonce = (secrets.token_urlsafe(64), time.time())
    valid_nonces.append(nonce)
    parameters = {
        "response_mode": "query",
        "response_type": "id_token token",
        "client_id": client_id,
        "redirect_uri": f"https%3A%2F%2F{server}%2Fcb",
        "scope": "openid profile",
        "login_hint": lao_id,
        "nonce": nonce[0],
        }
    return f"https://{server}/authorize?{parse.urlencode(parameters)}"
