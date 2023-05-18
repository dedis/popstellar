#!/bin/sh
echo "Creating Virtual environment"
python -m venv venv;
echo "Entering Virtual environment"
source venv/bin/activate;
echo "Installing dependencies"
pip install -r requirements.txt;
if [ $# -eq 2 ] ; then
    echo "Creating config file (This will reset your client id)"
    printf "{\"client_id\":\"\", \"host_url\": \"$1\", \"host_port\": $2}" > data/config.json
fi
echo "Starting server with $# arguments"
gunicorn -w 4 --chdir src "main:app" | tee -a log.out;