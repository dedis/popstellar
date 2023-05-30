#!/bin/sh
echo "Creating Virtual environment";
python -m venv venv;
echo "Entering Virtual environment";
. venv/bin/activate;
echo "Installing dependencies";
pip install -r requirements.txt;
numWorkers=4
if [ $# -lt 1 ] ; then
  echo "Please specify the config_file as the first argument (./run.sh config.json)";
  exit 1;
elif [ $# -eq 3 ] ; then
  numWorkers=$2;
fi
export FLASK_CONFIG_FILE=$1
echo "Starting server with $numWorkers workers";
gunicorn -w $numWorkers --chdir src "main:app";
