#!/usr/bin/env bash
set -euo pipefail

awslocal s3 mb s3://sleep-recordings || true
awslocal sqs create-queue --queue-name sleep-recording-analysis >/dev/null
