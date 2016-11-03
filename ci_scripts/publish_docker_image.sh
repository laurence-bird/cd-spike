#!/usr/bin/env bash

git_sha1=${CIRCLE_SHA1:$(git rev-parse HEAD)}

echo Publishing docker image to ECR with version $git_sha1

# Assume sbt is in charge of logging in to ECR
sbt "; set version := \"$git_sha1\"; docker:publish"


