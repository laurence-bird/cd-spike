#!/usr/bin/env bash
#
# Based on https://github.com/circleci/go-ecs-ecr/blob/master/deploy.sh
#
# Assumes the following environment variables are set:
# - AWS creds
# - AWS_ACCOUNT_ID

set -e # Exit if any function returns an error code

environment=${1?"Environment must be specified"}
service_name=${2:-$CIRCLE_PROJECT_REPONAME}
task_family=$service_name
cluster_name="ecs-cluster-$environment"
git_sha1="${CIRCLE_SHA1:-$(git rev-parse HEAD)}"

echo "Deploying version $git_sha1 to ECS in $environment environment."

# more bash-friendly output for jq
JQ="jq --raw-output --exit-status"

configure_aws_cli(){
	aws --version
	aws configure set default.region eu-west-1
	aws configure set default.output json
}

make_container_definitions(){
	template='[
		{
			"name": "%s",
			"image": "%s.dkr.ecr.eu-west-1.amazonaws.com/%s:%s",
			"essential": true,
			"memory": 200,
			"cpu": 10,
			"portMappings": [
				{
					"containerPort": 8080,
					"hostPort": 80
				}
			]
		}
	]'

	container_definitions=$(printf "$template" $service_name $AWS_ACCOUNT_ID $service_name $git_sha1)
}

register_task_definition() {
  if revisionArn=$(aws ecs register-task-definition --container-definitions "$container_definitions" --family "$task_family" | $JQ '.taskDefinition.taskDefinitionArn'); then
    echo "Revision: $revisionArn"
  else
    echo "Failed to register task definition"
    return 1
  fi
}

update_service() {
  if [[ $(aws ecs update-service --cluster "$cluster_name" --service "$service_name" --task-definition "$revisionArn" | \
    $JQ '.service.taskDefinition') != "$revisionArn" ]]
  then
    echo "Error updating service."
    return 1
  fi
}

await_stabilization() {
  # wait for older revisions to disappear
  for attempt in {1..30}; do
    if stale=$(aws ecs describe-services --cluster "$cluster_name" --services "$service_name" | \
      $JQ ".services[0].deployments | .[] | select(.taskDefinition != \"$revisionArn\") | .taskDefinition")
    then
      echo "Waiting for stale deployments:"
      echo "$stale"
      sleep 5
    else
      echo "Deployed!"
      return 0
    fi
  done
  echo "Service update took too long."
  return 1
}

deploy_to_ecs() {
    make_container_definitions
    register_task_definition
    update_service
    await_stabilization
}

configure_aws_cli
deploy_to_ecs
