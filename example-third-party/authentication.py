import urllib.parse
import secrets


def get_url(server: str, lao_id: str, client_id: str) -> str :
    baseUrl = "https://{}/authorize?response_mode=query&response_type=id_token%20token&client_id={}" \
              "&redirect_uri=https%3A%2F%2F{}%2Fcb&scope=openid%20profile&login_hint={}&nonce={}"
    url = baseUrl.format(server, client_id, server, lao_id, secrets.token_urlsafe(32))
    return url