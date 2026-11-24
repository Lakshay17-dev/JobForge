
#!/usr/bin/env bash
# Usage: KEY=jf_xxx scripts/load-test.sh [number-of-jobs]
# Needs GNU date (Linux or WSL).
set -euo pipefail

N="${1:-200}"
BASE="${BASE:-http://localhost:8080}"
: "${KEY:?Set KEY to your API key, e.g. export KEY=jf_...}"

pending() {
  curl -s "$BASE/actuator/prometheus" | awk '
    /^jobforge_jobs\{/ && (/status="QUEUED"/ || /status="RUNNING"/) {
      sum += $NF
    }
    END { print int(sum) }'
}

echo "Submitting $N jobs..."
start=$(date +%s.%N)

for i in $(seq 1 "$N"); do
  curl -s -o /dev/null -X POST "$BASE/jobs" \
    -H "X-API-Key: $KEY" \
    -H 'Content-Type: application/json' \
    -d "{\"type\":\"echo\",\"payload\":{\"n\":$i}}"
done

submitted=$(date +%s.%N)

echo "Waiting for the queue to drain..."
while [ "$(pending)" -gt 0 ]; do
  sleep 0.2
done

finished=$(date +%s.%N)

awk -v n="$N" -v s="$start" -v m="$submitted" -v f="$finished" 'BEGIN {
  printf "Submit phase : %.2f s (%.1f jobs/s)\n", m - s, n / (m - s)
  printf "End to end   : %.2f s (%.1f jobs/s)\n", f - s, n / (f - s)
}'
