#!/usr/bin/env bash
# Runs a claim through the deployed stack with real Keycloak tokens:
# alice submits, bob approves, dave (auditor) verifies the audit chain. Used by CI after deploying to kind.
set -euo pipefail

NS=expense-claims
kubectl -n "$NS" port-forward svc/keycloak 18180:8080 >/dev/null 2>&1 &
kubectl -n "$NS" port-forward svc/expense-claims-api 18080:80 >/dev/null 2>&1 &
trap 'kill $(jobs -p) 2>/dev/null' EXIT
sleep 3

token() {
  curl -sf localhost:18180/realms/expenses/protocol/openid-connect/token \
    -d grant_type=password -d client_id=expenses-cli -d username="$1" -d password="$1" | jq -r .access_token
}

claim_id=$(curl -sf -X POST localhost:18080/claims -H "Authorization: Bearer $(token alice)" \
  -H 'Content-Type: application/json' -d '{"description":"Taxi to client site","amount":42.50}' | jq -r .id)
echo "alice submitted claim $claim_id"

status=$(curl -sf -X POST "localhost:18080/claims/$claim_id/approve" -H "Authorization: Bearer $(token bob)" | jq -r .status)
echo "bob's decision: $status"
[ "$status" = "APPROVED" ]

verify=$(curl -sf localhost:18080/audit/verify -H "Authorization: Bearer $(token dave)")
echo "audit check: $verify"
[ "$(jq -r .valid <<<"$verify")" = "true" ]
