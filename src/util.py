#!/usr/bin/env python3

"""
util.py provides classes that are used by multiple applications
"""

"""
Oauth2 wrapper
"""
class oauth():
    """
    provides a wrapper around oauth, so that we can validate users/tokens, and
    relay relevant information to Postgre.
    """
    def __init__(token):
        """
        initialize class
        :param token: the OIDC/oauth2 token of the user
        """