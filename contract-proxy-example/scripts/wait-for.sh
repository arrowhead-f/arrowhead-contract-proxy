#!/bin/bash

set -e

until (echo > "/dev/tcp/$1/$2") >/dev/null 2>&1; do
  sleep 1
done
