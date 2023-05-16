echo "Creating Virtual environment"
python -m venv venv;
echo "Entering Virtual environment"
source venv/bin/activate;
echo "Installing dependencies"
pip install -r requirements.txt;
echo "Starting server"
gunicorn -w 4 --chdir src "main:app" | tee -a log.out;
