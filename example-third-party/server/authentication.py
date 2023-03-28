import urllib.parse


def getUrl(server: str, userToken: str, clientId: str) -> str :
    baseUrl = "https://{}/authorize?response_type=id_token%20token&client_id={}&redirect_uri={}cb&" +\
              "scope=openid%20profile&state={}nonce={}"

    url = .format()

    return url