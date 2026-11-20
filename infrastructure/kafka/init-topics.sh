#!/usr/bin/env bash
# ==============================================================================
# APIShield — Apache Kafka Topic Initialization Script
# Pre-provisions all canonical domain topics with appropriate partitions
# ==============================================================================

set -e

BOOTSTRAP_SERVER="${KAFKA_BOOTSTRAP_SERVER:-localhost:9092}"
REPLICATION_FACTOR="${KAFKA_REPLICATION_FACTOR:-1}"

echo "Waiting for Kafka broker to be ready at ${BOOTSTRAP_SERVER}..."
until kafka-topics.sh --bootstrap-server "${BOOTSTRAP_SERVER}" --list > /dev/null 2>&1; do
  echo "Kafka is not ready yet - sleeping 2 seconds..."
  sleep 2
done

echo "Kafka is ready. Creating APIShield topics..."

create_topic() {
  local TOPIC_NAME=$1
  local PARTITIONS=$2

  if kafka-topics.sh --bootstrap-server "${BOOTSTRAP_SERVER}" --list | grep -q "^${TOPIC_NAME}$"; then
    echo "Topic '${TOPIC_NAME}' already exists."
  else
    echo "Creating topic '${TOPIC_NAME}' with ${PARTITIONS} partition(s)..."
    kafka-topics.sh --bootstrap-server "${BOOTSTRAP_SERVER}" \
      --create \
      --topic "${TOPIC_NAME}" \
      --partitions "${PARTITIONS}" \
      --replication-factor "${REPLICATION_FACTOR}" \
      --config retention.ms=604800000
  fi
}

# Control Plane domain event topics (3 partitions each)
create_topic "apishield.user.events" 3
create_topic "apishield.registry.events" 3
create_topic "apishield.subscription.events" 3
create_topic "apishield.payment.events" 3
create_topic "apishield.apikey.events" 3
create_topic "apishield.notification.events" 3

# Data Plane telemetry stream (6 partitions for high throughput)
create_topic "apishield.gateway.analytics" 6

echo "All APIShield Kafka topics successfully verified and initialized."
kafka-topics.sh --bootstrap-server "${BOOTSTRAP_SERVER}" --list
