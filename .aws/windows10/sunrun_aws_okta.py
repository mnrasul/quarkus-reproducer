#!C:\Users\userName\AppData\Local\Programs\Python\Python39\Python

import os
from opinel.utils.credentials import *

profile = os.environ['AWS_OKTA_PROFILE']

credentials = read_creds_from_environment_variables()
write_creds_to_aws_credentials_file(profile, credentials)

if "dev" in profile:
    print("This is a 'dev' profile. Replicating credentials to 'default' profile for use with AWS SDKs.")
    write_creds_to_aws_credentials_file('default', credentials)

if "prd" in profile:
    print("################################################")
    print("WARNING SETTING PROD CREDENTIALS")
    print("################################################")
