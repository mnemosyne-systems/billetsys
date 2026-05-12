#!/bin/bash

# Usage:
# ./login.sh <username> <password>
#
# Example:
# ./login.sh omar User@123

USERNAME=$1
PASSWORD=$2

KEYCLOAK_URL="http://localhost:8180"
REALM="billetsys"
CLIENT_ID="billetsys-backend"

TOKEN=$(curl -s -X POST \
  "$KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=$CLIENT_ID" \
  -d "client_secret=123456" \
  -d "username=$USERNAME" \
  -d "password=$PASSWORD" | jq -r .access_token)

echo "$TOKEN"