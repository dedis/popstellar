from typing import Optional, Any
from flask import Flask, request
import json

def on_startup():
    global homePageHTML, providers
    providers = json.loads(open("data/providers.json").read())
    htmlProviders = [f'<option value="{str(i)}">{provider.get("laoId")}@{provider.get("url")}</option>' for i, provider in enumerate(providers)]
    homePageHTML = open("model/index.html", "r").read().replace("<!-- Insert options -->", ''.join(htmlProviders))

#Creates a subclass to be able to load some data on startup
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
    return homePageHTML

# Step2: Process user connection data prepare the OIDC request
@app.route("/authenticate")
def authentication():
    id = request.args.get("serverAndLaoId")
    token = request.args.get("token").strip()




 # Step 0: Starts the server
if __name__ == "__main__":
    app.run(debug = True, ssl_context= "adhoc")