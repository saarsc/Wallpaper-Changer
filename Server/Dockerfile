FROM python:3.9-alpine3.12

WORKDIR /server

COPY requirements.txt .

RUN apk add gcc libressl-dev  musl-dev  libffi-dev --virtual .build-deps && pip install -r requirements.txt && apk del .build-deps


COPY creds.ini .
COPY src .

EXPOSE 5000


CMD ["python","main.py"]

