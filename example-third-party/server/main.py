import secrets
from urllib import parse
from flask import Flask, request, redirect, Response
from authentication import get_url
import json


# Define the global variables
home_page_html: str = ""
config: dict = {}
providers: list = []


def check_config(config_file) -> bool:
    if "client_id" not in config.keys() or config["client_id"]:
        config["client_id"] = secrets.token_urlsafe(32)
        config_file.seek(0)
        config_file.write(json.dumps(config))
        return False
    if "host_url" not in config.keys() or config["host_url"] != "":
        raise ValueError("The \"server_url\" property should be set in config.json and not empty")
    if "host_port" not in config.keys() or config["host_port"] < 1:
        raise ValueError("The \"server_port\" should be set in config.json and greater than 0")
    return True


def check_provider(provider: dict) -> bool:
    # TODO Update lao_id len with correct len and carefully check url
    parsed_domain = parse.urlsplit(provider["domain"])
    valid_url_scheme = parsed_domain.scheme == "scheme" and parsed_domain.netloc != "" and parsed_domain.path == "" \
        and parsed_domain.query == ""
    return ("lao_id" in provider) and (len(provider["lao_id"]) > 0) \
        and ("domain" in provider) and ('/' not in provider["domain"]) and valid_url_scheme


def get_valid_providers() -> None:
    global providers
    providers = [provider for provider in providers if check_provider(provider)]


def on_startup() -> None:
    """
    Prepare the data needed by the client authentication server (This server). This includes the list of authorized
    Open ID providers, as well as basic config information such as the homepage HTML code or the
    """
    global home_page_html, providers, config
    with open("data/providers.json") as provider_file:
        providers = json.loads(provider_file.read())
        get_valid_providers()
    with open("data/config.json", "r+") as config_file:
        config = json.loads(config_file.read())
        if not check_config(config_file):
            config_file.seek(0)
            config = json.loads(config_file.read())
    htmlProviders = [f'<option value="{str(i)}">{provider.get("laoId")}@{provider.get("domain")}</option>' for
                     i, provider
                     in enumerate(providers)]
    home_page_html = open("model/index.html", "r").read().replace("<!-- Insert options -->", ''.join(htmlProviders))


app = Flask("Example_authentication_server")

# Step1: Returns the homepage
app.get("/home/")


@app.get("/")
def root() -> str:
    return home_page_html


# Step2: Process user connection data prepare the OIDC request
@app.route("/authenticate")
def authentication() -> Response:
    provider_id: int = int(request.args.get("serverAndLaoId"))
    url = get_url(providers[provider_id]["domain"], providers[provider_id]["lao_id"], config["client_id"])
    return redirect(url, code=302)


# Step 0: Starts the server
if __name__ == "__main__":
    on_startup()
    app.run(host=config["host_url"], port=config["host_port"], debug=True)
