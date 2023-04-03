from typing import Optional, Any
from flask import Flask, request, redirect
from authentication import get_url
import json


def on_startup():
    global home_page_html, providers, config
    providers = json.loads(open("data/providers.json").read())
    config = json.loads(open("data/config.json").read())
    htmlProviders = [f'<option value="{str(i)}">{provider.get("laoId")}@{provider.get("url")}</option>' for i, provider
                     in enumerate(providers)]
    home_page_html = open("model/index.html", "r").read().replace("<!-- Insert options -->", ''.join(htmlProviders))


# The only usage of this class is to call the on_
class ExampleFlask(Flask):
    def run(
            self,
            host: Optional[str] = None,
            port: Optional[int] = None,
            debug: Optional[bool] = None,
            load_dotenv: bool = True,
            **options: Any,
    ) -> None:
        with self.app_context():
            on_startup()
        super(ExampleFlask, self).run(host=host, port=port, debug=debug, load_dotenv=load_dotenv, **options)


app = ExampleFlask(__name__)

# Step1: Returns the homepage
app.get("/home/")


@app.get("/")
def root():
    return home_page_html


# Step2: Process user connection data prepare the OIDC request
@app.route("/authenticate")
def authentication():
    id = request.args.get("serverAndLaoId")
    token = request.args.get("token").strip()
    url = get_url()
    return redirect(url, code = 302)


# Step 0: Starts the server
if __name__ == "__main__":
    app.run(debug=True, ssl_context="adhoc")
