"""
This class contains the function related to the application data persitance
and  management
"""
import secrets
import time

from werkzeug.datastructures import MultiDict

from util import replace_in_model

base_app_html: str = ""
app_nonces : dict = {"valid":"user"} # Maps nonce to user
app_data : dict = {"user":0} # Maps users to counter values


def get_new_login_params(user_id: str) -> str:
    """
    Returns a valid URL to use for the app.
    :param user_id: The id of the newly logged-in user
    :returns: The url where the user should be redirected
    """
    if user_id not in app_data.keys():
            app_data[user_id] = 0
    nonce = secrets.token_urlsafe(64)
    app_nonces[nonce] = (user_id, time.time())
    return f"?nonce={nonce}"


def html_on_param(args: MultiDict):
    """
     Computes the HTML code of the app given the provided args
    :param args: The arguments passed to the app
    :returns: An empty strings if the request is invalid, the correct HTML
    code otherwise
    """
    # Validates nonce
    if "nonce" not in args or args.get("nonce", type=str) not in app_nonces:
        return ""
    user_id = app_nonces.pop(args.get("nonce", type=str))
    # Updates the app data if needed
    if "action" in args and args.get("action", type=str) == "increment":
        app_data[user_id] += 1
    if "action" in args and args.get("action", type=str) == "decrement":
        app_data[user_id] -= 1
    #Computes the returned html code
    new_nonce = secrets.token_urlsafe(64)
    app_nonces[new_nonce] = user_id
    return replace_in_model(base_app_html, nonce=new_nonce,
                            counter=str(app_data[user_id]))


def init_app() -> None:
    """
    Initializes the required variables to have a working app
    """
    global base_app_html
    with open("model/app.html") as app_file:
        base_app_html = app_file.read()
