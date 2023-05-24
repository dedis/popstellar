#!/bin/sh
echo "Creating Virtual environment"
python -m venv venv;
echo "Entering Virtual environment"
source venv/bin/activate;
echo "Installing dependencies"
pip install -r requirements.txt;
numWorkers=4
if [ $# -eq 2 ] || [ $# -eq 3 ] ; then
  echo "Creating config file (This will reset your client id)"
  printf "{\"client_id\":\"\", \"host_url\": \"%s\", \"host_port\": %s}" "$1" "$2" > data/config.json
fi
if [ $# -eq 1 ] ; then
  numWorkers=$1
elif [ $# -eq 3 ]; then
  numWorkers=$3
fi
echo "Starting server with $numWorkers workers"
gunicorn -w $numWorkers --chdir src "main:app" | tee -a log.out;