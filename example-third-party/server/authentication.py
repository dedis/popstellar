import urllib.parse
import secrets


def get_url(server: str, lao_id: str, client_id: str) -> str :
    baseUrl = "https://{server_url}/authorize?response_type=id_token%20token&client_id={id}" \
              "&redirect_uri=https%3A%2F%2F{serverURL}%2Fcb&scope=openid%20profile&state={state}nonce={nonce}"

    url = baseUrl.format(server_url = server, id = client_id, state = lao_id, nonce = secrets.token_urlsafe(32))

    return url