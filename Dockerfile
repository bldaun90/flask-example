# Dockerfile
# Copies the current folder (".") into container folder /app
# Sets /app folder as the working directory
# Installs requirements with pip install
# Runs the file using python app.py
#
FROM python:3.7

COPY . /app

WORKDIR /app

RUN pip install -r requirements.txt

ENTRYPOINT ["python3"]

CMD ["d-task-api.py"]
