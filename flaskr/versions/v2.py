"""
v2 API endpoints
"""

from flask import Blueprint, request

v2 = Blueprint('v2', __name__)

@v2.route("/api/v2/healthz", methods=['GET'])
def health():
    """
    endpoint for backend health status
    """
    if request.method == "GET":
        return {
            "status": "ok",
            "api_version": "v2"
        }