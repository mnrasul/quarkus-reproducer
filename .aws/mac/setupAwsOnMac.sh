#!/usr/bin/env bash

type brew
if [[ $? -ne 0 ]]; then
  echo Brew is not installed. Installing...
  /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/install.sh)"
else
  echo Brew is installed. Skipping...
fi

python -m pip --version
if [[ $? -ne 0 ]]; then
  echo "pip is not installed. Installing..."
  curl https://bootstrap.pypa.io/get-pip.py -o get-pip.py
  python get-pip.py
else
  echo pip is installed. Skipping...
fi

type aws
if [[ $? -ne 0 ]]; then
  # We only support AWS CLI v1 right now due to the AWFUL choices in V2 https://github.com/aws/aws-cli/pull/4702#issue-344978525
  # TODO check for v1
  echo "AWS CLI is not installed. Installing..."
  brew install awscli@1
brew link --force awscli@1
else
    echo "AWS CLI is installed. Skipping..."
fi

echo "Copying aws configuration files..."
mkdir -p ~/.aws
cp config ~/.aws/
cp sunrun_aws_okta.py ~/.aws/
chmod a+x  ~/.aws/sunrun_aws_okta.py

echo "Installing awslogin..."
# TODO check for existing install
python -m pip install opinel --upgrade --user
brew install aws-okta -s
cp awslogin /usr/local/bin/
chmod a+x /usr/local/bin/awslogin

echo "Adding Sunrun to your aws-okta configuration. You will be prompted for input. Here are Sunrun config values:"
aws-okta add --domain sunrun.okta.com



