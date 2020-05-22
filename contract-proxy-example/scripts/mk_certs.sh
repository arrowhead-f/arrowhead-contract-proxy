#!/bin/bash

cd "$(dirname "$0")" || exit
source "lib_certs.sh"
cd ..

create_root_keystore \
  "configuration/crypto/master.p12" "arrowhead.eu"

create_cloud_keystore \
  "configuration/crypto/master.p12" "arrowhead.eu" \
  "configuration/crypto/cloud.p12" "cp-example.ltu.arrowhead.eu"

create_consumer_system_keystore() {
  local SYSTEM_NAME=$1
  local SYSTEM_SANS=$2

  create_system_keystore \
    "configuration/crypto/master.p12" "arrowhead.eu" \
    "configuration/crypto/cloud.p12" "cp-example.ltu.arrowhead.eu" \
    "configuration/crypto/${SYSTEM_NAME}.p12" "${SYSTEM_NAME}.cp-example.ltu.arrowhead.eu" \
    "${SYSTEM_SANS}"
}

create_consumer_system_keystore "authorization"            "ip:172.23.1.10"
create_consumer_system_keystore "orchestrator"             "ip:172.23.1.11"
create_consumer_system_keystore "service_registry"         "ip:172.23.1.12"
create_consumer_system_keystore "event_handler"            "ip:172.23.1.13"

create_consumer_system_keystore "contract-initiator"       "ip:172.23.2.10"
create_consumer_system_keystore "contract-proxy-initiator" "ip:172.23.2.11"

create_consumer_system_keystore "contract-reactor"         "ip:172.23.3.10"
create_consumer_system_keystore "contract-proxy-reactor"   "ip:172.23.3.11"

create_sysop_keystore \
  "configuration/crypto/master.p12" "arrowhead.eu" \
  "configuration/crypto/cloud.p12" "cp-example.ltu.arrowhead.eu" \
  "configuration/crypto/sysop.p12" "sysop.cp-example.ltu.arrowhead.eu"

create_truststore \
  "configuration/crypto/truststore.p12" \
  "configuration/crypto/cloud.crt" "cp-example.ltu.arrowhead.eu"
