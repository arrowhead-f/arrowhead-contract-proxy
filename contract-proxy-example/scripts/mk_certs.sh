#!/bin/bash

cd "$(dirname "$0")" || exit
source "lib_certs.sh"
cd ..

create_root_keystore \
  "cloud/crypto/master.p12" "arrowhead.eu"

create_cloud_keystore \
  "cloud/crypto/master.p12" "arrowhead.eu" \
  "cloud/crypto/cloud.p12" "cp-example.ltu.arrowhead.eu"

create_consumer_system_keystore() {
  local SYSTEM_NAME=$1
  local SYSTEM_SANS=$2

  create_system_keystore \
    "cloud/crypto/master.p12" "arrowhead.eu" \
    "cloud/crypto/cloud.p12" "cp-example.ltu.arrowhead.eu" \
    "cloud/crypto/${SYSTEM_NAME}.p12" "${SYSTEM_NAME}.cp-example.ltu.arrowhead.eu" \
    "${SYSTEM_SANS}"
}

create_consumer_system_keystore "authorization"            "ip:172.23.2.13"
create_consumer_system_keystore "contract-initiator"       "ip:172.23.2.14"
create_consumer_system_keystore "contract-proxy-initiator" "ip:172.23.2.15"
create_consumer_system_keystore "contract-proxy-reactor"   "ip:172.23.2.16"
create_consumer_system_keystore "contract-reactor"         "ip:172.23.2.17"
create_consumer_system_keystore "event_handler"            "ip:172.23.2.18"
create_consumer_system_keystore "orchestrator"             "ip:172.23.2.19"
create_consumer_system_keystore "service_registry"         "ip:172.23.2.20"

create_sysop_keystore \
  "cloud/crypto/master.p12" "arrowhead.eu" \
  "cloud/crypto/cloud.p12" "cp-example.ltu.arrowhead.eu" \
  "cloud/crypto/sysop.p12" "sysop.cp-example.ltu.arrowhead.eu"

create_truststore \
  "cloud/crypto/truststore.p12" \
  "cloud/crypto/cloud.crt" "cp-example.ltu.arrowhead.eu"
