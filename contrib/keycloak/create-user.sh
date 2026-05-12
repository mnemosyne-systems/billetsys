# Usage:
# ./create-user.sh <username> <email> <password> <role> <firstName> <lastName>
#
# Example:
# ./create-user.sh user1 user1@mnemosyne-systems.ai User@123 user John Doe

USERNAME=$1
EMAIL=$2
PASSWORD=$3
ROLE=$4
FIRST_NAME=$5
LAST_NAME=$6

# get keycloak admin token
KEYCLOAK_TOKEN=$(curl -s -X POST \
  "http://localhost:8180/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=admin-cli" \
  -d "username=admin" \
  -d "password=admin" | jq -r .access_token)


# create user
curl -s -X POST \
  "http://localhost:8180/admin/realms/billetsys/users" \
  -H "Authorization: Bearer $KEYCLOAK_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"username\": \"$USERNAME\",
    \"email\": \"$EMAIL\",
    \"firstName\": \"$FIRST_NAME\",
    \"lastName\": \"$LAST_NAME\",
    \"enabled\": true,
    \"emailVerified\": true,
    \"requiredActions\": [],
    \"credentials\": [{
      \"type\": \"password\",
      \"value\": \"$PASSWORD\",
      \"temporary\": false
    }]
  }"


# get user id
USER_ID=$(curl -s \
  "http://localhost:8180/admin/realms/billetsys/users?username=$USERNAME" \
  -H "Authorization: Bearer $KEYCLOAK_TOKEN" | jq -r '.[0].id')

# get role id
ROLE_ID=$(curl -s \
  "http://localhost:8180/admin/realms/billetsys/roles/$ROLE" \
  -H "Authorization: Bearer $KEYCLOAK_TOKEN" | jq -r '.id')

# assign role
curl -s -X POST \
  "http://localhost:8180/admin/realms/billetsys/users/$USER_ID/role-mappings/realm" \
  -H "Authorization: Bearer $KEYCLOAK_TOKEN" \
  -H "Content-Type: application/json" \
  -d "[{\"id\": \"$ROLE_ID\", \"name\": \"$ROLE\"}]"


echo ""
echo "Created user:"
echo "  Username:   $USERNAME"
echo "  Email:      $EMAIL"
echo "  First Name: $FIRST_NAME"
echo "  Last Name:  $LAST_NAME"
echo "  Role:       $ROLE"
echo "  User ID:    $USER_ID"