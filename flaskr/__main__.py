#!/usr/bin/env python3

"""
CEREBRALSTRATUM Flask Based Backend
"""

from flaskr.versions.v1 import v1
from flaskr.versions.v2 import v2

from flask import Flask

def main():
    """
    Main application loop
    """
    app = Flask(__name__)
    app.register_blueprint(v1)
    app.register_blueprint(v2)

if __name__ == '__main__':
    main()
