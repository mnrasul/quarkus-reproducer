# Automation to Set Up AWS CLI Tooling

The tooling in this directory will install AWS CLI tooling and configuration for Sunrun's Okta login and three accounts (dev / stage / prod). 


A few things to know:
1. The `awslogin` script will create credentials that are valid for 12 hours by logging you into AWS through Okta. The credentials will be found in `~.aws/credentials`. 
2. When logging in to the Sunrun dev AWS account (i.e. `sunrundev` AWS profile), the credentials will also be written to the `default` AWS profile. This removes the need to set `--profile` on CLI commands or set `AWS_PROFILE` in your IDE or shell when developing. 
3. When logging into the Sunrun stage or prod accounts (i.e. `sunrunstage` and `sunrunprd`), the credentials WILL NOT be written to `default` as a guardrail. This will require `--profile` or `AWS_PROFILE` for AWS API operations.

## Prerequisites

1. Sunrun AWS Okta apps for dev, stage and prod accounts.

## Install on Mac

Be advised that the script will prompt you for user input to configure Okta integration for your user account.
```
cd .aws/mac
./setupAwsOnMac.sh
```

## Install on Windows 10

There is currently no install automation for Windows. Contributions welcome!

Prerequisites: 
- [Git Bash](https://git-scm.com/download/win) should be used as your shell.
- [AWS Okta CLI](https://github.com/segmentio/aws-okta/blob/master/docs/windows.md)
- [Python](https://docs.python.org/3/using/windows.html). The current awslogin script assumes python 3.9. If you install
a version different than 3.9, you will need to modify `sunrun_aws_okta.py`. 

The following steps should be run manually in Git Bash:
1. Copy `.aws\windows10\sunrun_aws_okta.py` to `C:\Users\<userName>\.aws\sunrun_aws_okta.py`.  
2. Copy `.aws\windows10\.bashrc` to `C:\Users\<userName>\.bashrc`. If you have a `.bashrc` file already, append the contents.
   

## Verify The Installation on Mac & Windows

If these steps work, your installation is correct and ready for use:

1. Open a new shell because the installation modifies your shell profile (`.bashrc` or `.zshrc`).
2. Should prompt you for Okta two factor and list out s3 buckets in the dev account. The `aws-okta` plugin stores your credentials in the Mac key chain, so this first run should prompt you for permission to the keychain.
    ```  
   awslogin sunrundev
   aws s3 ls
    ```
3. Should login to stage account and list out stage s3 buckets. You should not be prompted for two factor key because your previous two factor key is still valid.
    ```  
   awslogin sunrunstage
   aws s3 ls --profile sunrunstage
    ```
4. Should login to prd account and list out stage s3 buckets. You should not be prompted for two factor key because your previous two factor key is still valid.
    ```  
   awslogin sunrunprd
   aws s3 ls --profile sunrunprd
    ```   
If the above work correctly, then you have a valid installation. If you get an error that you do not have access to a 
particular AWS account, talk to your manager and have them add you via Okta.
