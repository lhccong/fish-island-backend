#!/usr/bin/env bash
# Linux with Docker Engine and Docker Compose v2.
# Optional on first run: REME_DIR, REME_PUBLISHED_PORT, DEEPSEEK_API_KEY.
set -euo pipefail
umask 077

fail() {
    printf '%s\n' "$*" >&2
    exit 1
}

command -v docker >/dev/null || fail "Install Docker Engine first."
docker compose version >/dev/null || fail "Install the Docker Compose v2 plugin first."
docker_os=$(docker info --format '{{.OSType}}') || fail "Cannot reach the Docker daemon."
[[ "$docker_os" == linux ]] || fail "ReMe requires a Linux Docker daemon."

deploy_dir=${REME_DIR:-"$HOME/reme-server"}
mkdir -p -- "$deploy_dir"
cd -- "$deploy_dir"

run_uid=$(id -u)
run_gid=$(id -g)
if [[ "$run_uid" == 0 ]]; then
    run_uid=1000
    run_gid=1000
fi

if [[ ! -e compose.yaml ]]; then
    published_port=${REME_PUBLISHED_PORT:-2333}
    if [[ ! "$published_port" =~ ^[1-9][0-9]{0,4}$ ]] || (( published_port > 65535 )); then
        fail "REME_PUBLISHED_PORT must be between 1 and 65535."
    fi

    if [[ ! -e data ]]; then
        mkdir -- data
        if [[ $(id -u) == 0 ]]; then
            chown "$run_uid:$run_gid" data
        fi
    fi
    [[ -d data ]] || fail "The ReMe data path must be a directory."

    cat > compose.yaml <<EOF
services:
  reme:
    image: ghcr.io/agentscope-ai/reme:0.4.2.3
    user: "$run_uid:$run_gid"
    restart: unless-stopped
    stop_grace_period: 60s
    ports:
      - "127.0.0.1:$published_port:2333"
    env_file:
      - model.env
    environment:
      REME_TIMEZONE: Asia/Shanghai
    volumes:
      - type: bind
        source: ./data
        target: /data
        bind:
          create_host_path: false
EOF
fi

if [[ ! -e model.env ]]; then
    deepseek_key=${DEEPSEEK_API_KEY:-}
    if [[ -z "$deepseek_key" ]]; then
        if ! read -r -s -p 'DeepSeek API Key: ' deepseek_key </dev/tty; then
            fail "Run this script in a terminal or set DEEPSEEK_API_KEY first."
        fi
        printf '\n'
    fi
    [[ "$deepseek_key" =~ ^[A-Za-z0-9._-]+$ ]] || fail "Invalid DeepSeek API key format."
    printf '%s\n' \
        'LLM_BACKEND=deepseek' \
        'LLM_BASE_URL=https://api.deepseek.com' \
        'LLM_MODEL_NAME=deepseek-flash' \
        "LLM_API_KEY=$deepseek_key" > model.env
    unset deepseek_key
fi

compose=(docker compose --file compose.yaml)
"${compose[@]}" config --quiet
if ! "${compose[@]}" up -d --wait --wait-timeout 180 reme; then
    fail "ReMe did not become healthy. Inspect logs with: docker compose logs reme (from $PWD)"
fi

endpoint=$("${compose[@]}" port reme 2333)
printf 'ReMe is healthy at http://%s\n' "$endpoint"
printf 'Memory workspace: %s/data\n' "$PWD"
