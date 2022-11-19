
"""
v1 API endpoints
"""

from flask import Blueprint, request

v1 = Blueprint('v1', __name__)

@v1.route("/api/v1/healthz", methods=['GET'])
def health():
    """
    endpoint for backend health status
    """
    if request.method == "GET":
        return {
            "status": "ok"
        }