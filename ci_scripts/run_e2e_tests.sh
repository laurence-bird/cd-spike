#!/usr/bin/env bash

echo "yo I'm running the end-to-end tests"

git clone git@github.com:ovotech/comms-e2e-tests-example.git e2e

cd e2e

./run.sh
