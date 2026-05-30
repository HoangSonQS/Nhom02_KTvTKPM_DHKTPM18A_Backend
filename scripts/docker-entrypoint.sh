#!/bin/sh
set -e

mkdir -p /logs/app /app/config/keys
chown -R spring:spring /logs/app /app/config/keys 2>/dev/null || true

if su-exec spring sh -c 'touch /logs/app/.write-test && rm -f /logs/app/.write-test' 2>/dev/null; then
    exec su-exec spring sh -c 'java $JAVA_OPTS -jar /app/app.jar'
fi

echo "WARN: /logs/app is not writable by spring; running application as root for this bind mount."
exec sh -c 'java $JAVA_OPTS -jar /app/app.jar'
