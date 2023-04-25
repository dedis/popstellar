"""
This files includes the basis of the example server. Here are managed all
HTTP/S requests.
"""
import json
import secrets
from typing import IO

from flask import Flask, redirect, request, Response

from authentication import get_url, validate_args

# Define the global variables
home_page_html: str = ""
config: dict = {}
providers: list = []

def replace_in_model( model: str, **kwargs) -> str:
    """
    Replace the comment "Insert arg here" in the model by its value
    """
    for key in kwargs:
        model = model.replace(f"<!-- Insert {key} here -->", kwargs[key])
    return model


def check_config(config_file: IO) -> bool:
    """
    Checks that the global config file is valid. Updates the updatable
    parameters in the file.
    :param config_file:  the file where the configuration is written
    :return: True if the configurations is valid
    """
    if "client_id" not in config.keys() or config["client_id"] == '':
        config["client_id"] = secrets.token_urlsafe(32)
        config_file.seek(0)
        config_file.write(json.dumps(config))
        return False
    if "host_url" not in config.keys() or config["host_url"] == "":
        raise ValueError(
            "The \"server_url\" property should be set in "
            "config.json and not empty"
        )
    if "host_port" not in config.keys() or config["host_port"] < 1:
        raise ValueError(
            "The \"server_port\" should be set in config.json "
            "and greater than 0"
        )
    return True


def check_provider(provider: dict) -> bool:
    """
    Check that a single provider is valid. It only performs static checks and
    no connectivity tests.
    :param provider: The provider to check
    :return: True if the provider is valid
    """
    valid_lao_id = (("lao_id" in provider) and (len(provider["lao_id"]) == 44)
                    and provider["lao_id"].endswith("="))
    valid_domain = (("domain" in provider) and ('/' not in provider["domain"])
                    and not provider["domain"].startswith("http"))
    valid_public_key = len(provider["public_key"]) > 0
    return valid_lao_id and valid_domain and valid_public_key


def filter_providers() -> None:
    """
    Keeps only the providers that are valid
    """
    global providers
    providers = [provider for provider in providers if check_provider(provider)]


def on_startup() -> None:
    """
    Prepare the data needed by the client authentication server (This
    server). This includes the list of authorized
    Open ID providers, as well as basic config information such as the
    homepage HTML code or the
    """
    global home_page_html, providers, config
    with open("data/providers.json") as provider_file:
        providers = json.loads(provider_file.read())
        filter_providers()
    with open("data/config.json", "r+") as config_file:
        config = json.loads(config_file.read())
        if not check_config(config_file):
            config_file.seek(0)
            config = json.loads(config_file.read())
    home_page_html = open("model/index.html", "r").read()


app = Flask("Example_authentication_server")
on_startup()


# Step1: Returns the homepage
@app.get("/")
def root() -> str:
    """
    Get the homepage.
    :return: The homepage HTML
    """
    providers_html = [
        f'<option value="{str(i)}">{provider.get("lao_id")}@'
        f'{provider.get("domain")}</option>'
        for
        i, provider
        in enumerate(providers)]
    ''.join(providers_html)
    return replace_in_model(home_page_html, providers=providers_html,
                            errors="test")


# Step2: Process user connection data prepare the OIDC request
@app.route("/authenticate")
def authentication() -> Response:
    """
    Redirect the user to the PoPCHA based authentication server
    :return: A response which includes a redirect to the original website
    """
    provider_id: int = request.args.get("serverAndLaoId", -1, type=int)
    if provider_id < 0:  # if serverAndLaoId is not an int
        return redirect("/")
    url = get_url(
        providers[provider_id]["domain"],
        providers[provider_id]["lao_id"],
        config["client_id"]
    )
    return redirect(url)


# Step3: Process the callback/authentication response
@app.route("/cb")
def authentication_callback() -> Response:
    """
    Redirects the user after the authentication callback has been verified
    :return: A response which redirects the user to the homePage if the login
    is not valid or to a new "app" page if the login answer is valid
    """
    valid_answer: bool = validate_args(request.args)
    if valid_answer:
        return redirect(valid_login_url(request.args))
    else:
        return redirect("/")

# Step 0: Starts the server
if __name__ == "__main__":
    app.run(host=config["host_url"], port=config["host_port"], debug=True)
