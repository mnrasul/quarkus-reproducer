#!/bin/bash

if [ -z "${GITHUB_HEAD_REF}" ]; then
export GITHUB_HEAD_REF=$(git branch | sed -n -e 's/^\* \(.*\)/\1/p')
fi

function cmd_dot_env() {
  generate_dot_env
}

function cmd_deploy_secrets() {
  echo "Storing secrets from .secrets.json in AWS Secrets Manager for your local stack..."
  # https://stackoverflow.com/questions/59895/how-to-get-the-source-directory-of-a-bash-script-from-within-the-script-itself
  SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
  SECRETS_FILE_URI="file://${SCRIPT_DIR}/.secrets.json"
  aws secretsmanager put-secret-value --secret-id "$(get_stack_name)" --secret-string $SECRETS_FILE_URI
}

function cmd_download_secrets() {
  # Secret outside of cloudformation that holds secrets for devs and CI tool
  # https://us-west-2.console.aws.amazon.com/secretsmanager/home?region=us-west-2#/secret
  if [[ ! -f .secrets.json ]]; then
    echo ".secrets.json is missing. Downloading..."
    aws secretsmanager get-secret-value --secret-id ci-pricing-lambda | jq '.SecretString | fromjson' >.secrets.json
  fi
}

# TODO: this could be replaced with a third-party github action really
# the workflow is managing the naming scheme of the files, which it shouldn't really need to worry about
function cmd_upload_artifact() {
    if [[ -z $1 ]]; then
    echo "ERROR: Please input the name of your bucket."
    exit 1
  fi

  if [[ -z $2 ]]; then
    echo "ERROR: Please input the prefix for the name of your artifact."
    exit 1
  fi

  echo "Uploading function.zip to s3://$1/$2-function.zip"

  FUNCTION_ZIP=target/function.zip

  if [[ ! -f $FUNCTION_ZIP ]]; then
    echo "ERROR: function.zip not found!"
    exit 1
  fi

  aws s3 cp $FUNCTION_ZIP s3://$1/$2-function.zip
}

# TODO: this could be replaced with a third-party github action really
function cmd_download_artifact() {
  if [[ -z $1 ]]; then
    echo "ERROR: Please input the name of your bucket."
    exit 1
  fi

  if [[ -z $2 ]]; then
    echo "ERROR: Please input the prefix for the name of your artifact."
    exit 1
  fi

  echo "Downloading function.zip from s3://$1/$2-function.zip."

  FUNCTION_ZIP=target/function.zip

  aws s3 cp s3://$1/$2-function.zip $FUNCTION_ZIP
}

function cmd_build() {
  MVN_FLAGS=""
#  if [[ $(uname) == "Darwin" ]]; then
#    echo "Executing on Mac. Native image will build in a docker container."
  MVN_FLAGS="-Dquarkus.native.container-build=true -Dquarkus.native.builder-image=$1 -Dquarkus.test.profile.tags=local"
#  else
#    echo "Executing on Linux"
  export MAVEN_OPTS="-Xmx4096m"
#  fi
  cmd_download_secrets


  mvn -q --no-transfer-progress clean install --no-transfer-progress -Dnative $MVN_FLAGS
}


function cmd_diff() {
  if [[ -z "${CI}" ]]; then # TODO swap -z with -n
    echo "Deploying a sandbox stack to play in!"
    npx cdk diff --fail
  elif [[ -z "${ENV_NAME}" ]]; then
    echo "ERROR: ENV_NAME env variable must be defined to deploy in CD pipeline."
    exit 1
  else
    npx cdk diff --fail --app "mvn -e -q compile exec:java -Dexec.args=${ENV_NAME}"
  fi
}

function cmd_deploy() {
  if [[ -z "${CI}" ]]; then # TODO swap -z with -n
    echo "Deploying a sandbox stack to play in!"
    npx cdk deploy
    if [[ $? -ne 0 ]]; then
		    echo "CDK deploy failed!"
		    exit 1
    fi
    cmd_dot_env
    #    cmd_download_secrets
    #    cmd_deploy_secrets
  elif [[ -z "${ENV_NAME}" ]]; then
    echo "ERROR: ENV_NAME env variable must be defined to deploy in CD pipeline."
    exit 1
  else
    if [[ "${NO_EXECUTE}" == "true" ]]; then
      CDK_DEPLOY_LOGS=$(npx cdk deploy --require-approval=never --no-execute --app "mvn -e -q compile exec:java -Dexec.args=${ENV_NAME}" 2>&1)
      if [[ $? -ne 0 ]]; then
		    echo "CDK deploy failed!"
		    exit 1
      fi
      echo "${CDK_DEPLOY_LOGS}"
      CHANGE_SET_NAME=$(echo "${CDK_DEPLOY_LOGS}" | grep 'Changeset' | sed -e 's/.*Changeset \(.*\) created.*/\1/')
      echo "${CHANGE_SET_NAME}"
      CHANGE_SET_URL=$(get_change_set_url "$CHANGE_SET_NAME")

      echo "::set-output name=change_set_name::${CHANGE_SET_NAME}"
      echo "::set-output name=change_set_url::${CHANGE_SET_URL}"
    else
      npx cdk deploy --require-approval=never --app "mvn -e -q compile exec:java -Dexec.args=${ENV_NAME}"
      if [[ $? -ne 0 ]]; then
		    echo "CDK deploy failed!"
		    exit 1
      fi
    fi
    STACK_ARN=$(cmd_get_stack_arn "$(get_stack_name)")
    STACK_URL="https://us-west-2.console.aws.amazon.com/cloudformation/home?region=us-west-2#/stacks/stackinfo?filteringText=&filteringStatus=active&viewNested=true&hideStacks=false&stackId=${STACK_ARN}"

    echo "::set-output name=stack_arn::${STACK_ARN}"
    echo "::set-output name=stack_url::${STACK_URL}"
  fi
}

function get_change_set_url() {
  CHANGE_SET_ID=$(aws cloudformation list-change-sets --stack-name $(get_stack_name) --query 'Summaries[?ChangeSetName==`'"${1}"'`].ChangeSetId' --output text)
  STACK_ID=$(aws cloudformation list-change-sets --stack-name $(get_stack_name) --query 'Summaries[?ChangeSetName==`'"${1}"'`].StackId' --output text)
  echo "https://us-west-2.console.aws.amazon.com/cloudformation/home?region=us-west-2#/stacks/changesets/changes?stackId=${STACK_ID}&changeSetId=${CHANGE_SET_ID}"
}

function cmd_test() {
  if [[ -n "${CI}" ]]; then
    cmd_dot_env
    #    cmd_download_secrets
    #    cmd_deploy_secrets
  fi
  #Cloud tests should be run with a different profile than the test profile so they can hit the real lambda after deployment to the pr env
  mvn -DfailIfNoTests=false -Dquarkus.test.profile.tags=cloud test
}

function cmd_integrationTests() {
  if [[ -n "${CI}" ]]; then
    cmd_dot_env
  fi
  #Cloud tests should be run with a different profile than the test profile so they can hit the real lambda after deployment to the pr env
  mvn -DfailIfNoTests=false -Dquarkus.test.profile.tags=integration test
}


function cmd_clean() {
  mvn clean
}

function cmd_destroy() {
  empty_bucket
  aws cloudformation delete-stack --stack-name="$(get_stack_name)"
}

function cmd_get_stack_arn() {
  aws cloudformation describe-stacks \
    --stack-name $1 \
    --query "Stacks[0].StackId" \
    --output text
}

function cmd_get_latest_version() {
  aws cloudformation list-stack-resources --stack-name="$(get_stack_name)" \
                --query "StackResourceSummaries[?ResourceType=='AWS::Lambda::Version'].PhysicalResourceId" \
                | jq -j '.[0] | split(":") | .[-1]'
}

function get_physical_resource_id() {
  aws cloudformation list-stack-resources --stack-name=$1 \
    --query "StackResourceSummaries[?starts_with(LogicalResourceId,'$2')].PhysicalResourceId" --output text
}

function get_physical_resource_id_for_resource_type() {
  aws cloudformation list-stack-resources --stack-name=$1 \
    --query "StackResourceSummaries[?starts_with(LogicalResourceId,'$2') && ResourceType=='$3'].PhysicalResourceId" --output text
}

function empty_bucket() {
  echo "Deleting data from bucket in stack $(get_stack_name)..."
  aws s3 rm s3://$(get_physical_resource_id $(get_stack_name) PricingLambdaBucket) --recursive
}

function generate_dot_env() {
  echo "Generating .env file"

  export GET_PRICE_FUNCTION_NAME="$(get_physical_resource_id_for_resource_type "$(get_stack_name)" HandlerName AWS::Lambda::Function)"
  envsubst <.env.template | sed '1d' >.env
  if [ -f ".secrets.json" ]; then
    echo ".secrets.json exists! Adding .secrets.json to .env file"
    jq -r 'to_entries[] | "\(.key)=\(.value)"' .secrets.json >>.env
  fi
}

function get_stack_name() {
  SERVICE_NAME="test2"
  WHOAMI=$(whoami | sed "s/\./\-/")

  if [[ -z ${CI} ]]; then
    echo "${WHOAMI}-${SERVICE_NAME}"
  else
    if [[ -z ${ENV_NAME} ]]; then
      echo "ERROR: Running in CI mode but ENV_NAME is not defined."
      exit 1
    fi
    echo "${ENV_NAME}-${SERVICE_NAME}"
  fi
}

VERSION=1

if [ -z ${1} ]; then

  echo "Available commands:"
  echo "  dot-env - Generate .env file from existing cloudformation stack"
  echo "  deploy-secrets - Push your .secrets.json into the secretsmanager"
  echo "  deploy - Deploys the application using AWS CDK"
  echo "  get-latest-version - gets the latest version of the function"

  echo "\nNOTE: All commands use whoami and the hardcoded SERVICE_NAME to create the cloudformation stack name. See the code for details.\n"
else
  # https://unix.stackexchange.com/questions/168221/are-there-problems-with-hyphens-in-functions-aliases-and-executables
  # https://stackoverflow.com/questions/13210880/replace-one-substring-for-another-string-in-shell-script/13210909
  eval cmd_${1/-/_} ${2} ${3}
fi
