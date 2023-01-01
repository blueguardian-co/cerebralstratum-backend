#!/usr/bin/env python3

"""
CEREBRALSTRATUM Flask Based Backend
"""
from os import environ

from authlib.integrations.flask_client import OAuth
from flask import Flask
from flask_socketio import SocketIO

from src.versions.v1 import v1
from src.versions.v2 import v2

app = Flask(__name__)
app.config['SECRET_KEY'] = environ.get('OAUTH_CLIENT_KEY', 'SECRET1!')
app.register_blueprint(v1)
app.register_blueprint(v2)
socketio = SocketIO(app)

oauth = OAuth(app)

def main():
    """
    Main application loop
    """
    socketio.run(app, host="127.0.0.1", port=6443)

if __name__ == '__main__':
    main()
