"""
This files includes the basis of the example server. Here are managed all
HTTP/S requests.
"""
from __future__ import annotations

import json
from os import path
import secrets
import urllib.parse

from flask import Flask, redirect, request, Response, render_template
from flask_wtf import CSRFProtect

from src.counterApp import CounterApp
from src.authentication import Authentication

# Define the global variables
config: dict[str, str | int] = {}
core_app: CounterApp
authenticationProvider: Authentication
app = Flask("Example_authentication_server",
            template_folder = path.join(path.dirname(__file__), "templates")
            )


def validate_config(configuration: dict[str, str | int]) -> dict[str, str]:
    """
    Checks that the global config file is valid. Returns the missing /
    updatable parameters
    :param configuration:  the file where the configuration is written
    :return: A dictionary containing the change in the configuration. This
    function might throw error if needed.
    """
    is_client_id_valid = ("client_id" not in configuration or
                          configuration["client_id"] == "")
    if is_client_id_valid:
        return {"client_id": secrets.token_urlsafe(32)}

    is_public_domain_valid = ("public_domain" not in configuration or
                           configuration["public_domain"] == "")
    if is_public_domain_valid:
        raise ValueError(
                "The \"public_domain\" property should be set in "
                "config.json and not empty"
                )

    is_public_port_valid = ("public_port" not in configuration or
                            configuration["public_port"] < 1)
    if is_public_port_valid:
        raise ValueError(
                "The \"public_port\" should be set in config.json "
                "and greater than 0"
                )

    is_local_port_valid = ("local_port" not in configuration or
                           configuration["local_port"] < 1)
    if is_local_port_valid:
        raise ValueError(
            "The \"local_port\" should be set in config.json "
            "and greater than 0"
            )

    return {}


def check_provider(provider: dict) -> bool:
    """
    Check that a single provider is valid. It only performs static checks and
    no connectivity tests.
    :param provider: The provider to check
    :return: True if the provider is valid
    """
    # 44 is the length of the string representing a lao id
    valid_lao_id = (("lao_id" in provider) and (len(provider["lao_id"]) == 44)
                    and provider["lao_id"].endswith("="))

    valid_domain = (("domain" in provider) and ('/' not in provider["domain"])
                    and not provider["domain"].startswith("http"))

    valid_public_key = ("public_key" in provider and
                        len(provider["public_key"]) > 0)

    return valid_lao_id and valid_domain and valid_public_key


def filter_providers(providers: list) -> list:
    """
    Keeps only the providers that are valid from the original list
    :param providers: The original list of providers
    :return: The valid providers of the original list
    """
    return [provider for provider in providers if check_provider(provider)]


def load_providers() -> list:
    """
    Loads the providers from the configuration file and returns them
    :return: The providers present in the configuration files
    """
    provider_path = path.normpath(
        path.join(
            path.dirname(__file__),
            "../data/providers.json"
            )
        )

    with open(provider_path) as provider_file:
        return json.loads(provider_file.read())


def on_startup() -> None:
    """
    Prepare the data needed by the client authentication server (This
    server). This includes the list of authorized
    Open ID providers.html, as well as basic config information such as the
    homepage HTML code or the
    """
    global config, core_app, authenticationProvider

    app.config.from_prefixed_env()

    csrf = CSRFProtect()
    csrf.init_app(app)

    providers = load_providers()

    core_app = CounterApp()
    authenticationProvider = Authentication(filter_providers(providers))
    config_path = path.normpath(
        path.join(
            path.dirname(__file__),
            f"../{app.config['CONFIG_FILE']}"
            )
        )

    with open(config_path, "r+") as config_file:
        config = json.loads(config_file.read())

        config_modifications = validate_config(config)
        if len(config_modifications) != 0:
            config.update(config_modifications)
            config_file.seek(0)
            config_file.write(json.dumps(config))
            print(f"Your app configuration has been updated in "
                  f"{app.config['CONFIG_FILE']}")

# Step1: Returns the homepage
@app.get("/")
def root() -> str:
    """
    Get the homepage.
    :return: The homepage HTML
    """
    providers_strings = [f'{provider.get("lao_id")}@{provider.get("domain")}'
        for provider in authenticationProvider.providers]

    error: str = ""

    if "error" in request.args:
        error = f'Error: {request.args.get("error", default = "", type = str)}'

    return render_template(
            'index.html',
            providers = providers_strings,
            error = error
            )


# Step2: Process user connection data prepare the OIDC request
@app.route("/authenticate")
def authentication() -> Response:
    """
    Redirect the user to the PoPCHA based authentication server
    :return: A response which includes a redirect to the original website
    """
    provider_id: int = request.args.get("serverAndLaoId", default = -1,
                                        type = int)
    if provider_id < 0:  # if serverAndLaoId is not an int
        return redirect("/")

    url = authenticationProvider.get_url(
            authenticationProvider.providers[provider_id]["domain"],
            authenticationProvider.providers[provider_id]["lao_id"],
            config["public_domain"],
            config["public_port"],
            config["client_id"])

    return redirect(url)


# Step3: Process the callback/authentication response
@app.route("/cb")
def authentication_callback() -> Response:
    """
    Redirects the user after the authentication callback has been verified
    :return: A response which redirects the user to the homePage if the login
    is not valid or to a new "app" page if the login answer is valid
    """
    user_id: str = authenticationProvider.validate_args(
            request.args, config[
                "client_id"], )

    if user_id is not None:
        params = urllib.parse.urlencode(core_app.get_new_login_params(user_id))
        return redirect(f'/app?{params}')
    else:
        return redirect("/")


@app.route("/app")
def app_route():
    """
    The main part of the app (very light since it is only an example)
    :return: The HTML data returned based on the app module logic or a
    redirection if this path is called without valid query arguments
    """
    try:
        html: str = core_app.process(request.args)

    except ValueError:
        return redirect("/")

    return html


# WARNING: This code is used since it is an example app, but it is UNSAFE

@app.route("/add_provider")
def add_provider():
    """
    !!! DANGER ZONE !!!
    WARNING: This call is unsafe and allows to easily add new providers.html. It
    is intended for example / showcase servers that do not provide security.
    """
    args = request.args

    if not check_provider(args):
        return redirect("/?error=Invalid%20provider")

    provider = {
        "domain": args["domain"],
        "lao_id": args["lao_id"],
        "public_key": args["public_key"]
    }
    authenticationProvider.providers.append(provider)
    return redirect("/")

on_startup()
