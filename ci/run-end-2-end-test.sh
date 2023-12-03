#!/bin/bash
set -e # Set to fail on error

echo "Running End To End Test Script"

echo $@

while getopts :ci:s:u:p:bc:prn:bn: flag
do
    case "${flag}" in
        ci) isCI=${OPTARG};;
        s) server=${OPTARG};; 
        u) user=${OPTARG};; 
        p) password=${OPTARG};; 
        bc) buildCounter=${OPTARG};;
        prn) pullRequestNumber=${OPTARG};;
        bn) buildNumber=${OPTARG};;
        \?) echo "Invalid option: -$OPTARG" >&2;; 
    esac
done

mkdir -p .batect/sqlvolume
sudo chown 10001:0 .batect/sqlvolume
prNumber="NOT_SET"
if [ -n "$pullRequestNumber" ]; then
    prNumber=$pullRequestNumber
fi

echo "isCI $isCI";
echo "Server $server";
echo "User $user";
echo "Password $password"
echo "BuildCounter $buildCounter"
echo "PullRequestNumber $pullRequestNumber"
echo "BuildNumber $buildNumber"
echo "PRNumber $prNumber"

#make

# ./batect \
# --config-var BUILD_NUMBER=%build.number% \
# --config-var TC_SONAR_QUBE_USE="1" \
# --config-var TC_SONAR_QUBE_SERVER=""%env.sonar_server%"" \
# --config-var TC_SONAR_QUBE_USER=""%env.sonar_user%"" \
# --config-var TC_SONAR_QUBE_PASSWORD=""%env.sonar_password%"" \
# --config-var TC_SONAR_QUBE_VERSION=""%build.counter%"" \
# --config-var TC_SONAR_QUBE_NUMBER=""$prNumber"" \
# teamcity