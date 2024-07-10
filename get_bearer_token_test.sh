#!/usr/bin/env bash
set -euo pipefail

export access_token=$(\
    curl --insecure -X POST https://sso.bastion.blueguardian.co/realms/silent-auction-demo/protocol/openid-connect/token \
    --user 'backend:JdRAtGrsGuhj6Hy6biwBV9CvJsM8amVU' \
    -H 'content-type: application/x-www-form-urlencoded' \
    -d 'username=test.user@example.com&password=Password123&grant_type=password' | jq --raw-output '.access_token' \
)

curl -v -H "Authorization: Bearer $access_token" \
  http://localhost:6443/api/v1/users/me