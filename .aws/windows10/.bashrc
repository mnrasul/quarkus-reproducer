function awslogin {
	aws-okta exec -t 12h "$1" -- python.exe ~/.aws/sunrun_aws_okta.py;
}

export -f awslogin
