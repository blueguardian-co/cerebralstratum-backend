#!/usr/bin/env python3

"""
CEREBRALSTRATUM Flask Based Backend
"""

from src.versions.v1 import v1
from src.versions.v2 import v2

from flask import Flask

app = Flask(__name__)
app.register_blueprint(v1)
app.register_blueprint(v2)
