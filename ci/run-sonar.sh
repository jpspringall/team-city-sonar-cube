#!/bin/bash
set -e

echo "Running Script"

projectKey="SonarQubeLocalhostTest"
projectName="SonarQubeLocalhostTest"

while getopts :s:u:p:n:v: flag
do
    case "${flag}" in
        s) server=${OPTARG};; 
        u) user=${OPTARG};; 
        p) password=${OPTARG};; 
        n) number=${OPTARG};;
        v) version="2.0.${OPTARG}";;
        \?) echo "Invalid option: -$OPTARG" >&2;; 
    esac
done

echo "Server $server";
echo "User $user";
echo "Number $number"
echo "Version $version"

#Not needed for now
cd project


#If no PR number provided
if [ -z "$number" ]; then
    dotnet-sonarscanner begin \
    /k:"$projectKey" \
    /n:"$projectName" \
    /v:"$version" \
    /d:sonar.verbose="true" \
    /d:sonar.host.url="$server" \
    /d:sonar.login="$user" \
    /d:sonar.password="$password" \
    /d:sonar.cs.opencover.reportsPaths="**/coverage.opencover.xml"
else
    dotnet-sonarscanner begin \
    /k:"$projectKey" \
    /n:"$projectName" \
    /v:"$version" \
    /d:sonar.verbose="true" \
    /d:sonar.host.url="$server" \
    /d:sonar.login="$user" \
    /d:sonar.password="$password" \
    /d:sonar.cs.opencover.reportsPaths="**/coverage.opencover.xml" \
    /d:sonar.pullrequest.key="$number" \
    /d:sonar.pullrequest.branch="pull/$number" \
    /d:sonar.pullrequest.base="master"
fi

#https://stackoverflow.com/questions/69368514/how-can-i-properly-generate-both-trx-files-and-code-coverage-results-with-one-ca
dotnet test TCSonarCube.sln -v:n -c Release -p:CollectCoverage=true -p:CoverletOutputFormat=opencover%2cteamcity --results-directory "/test-results" --logger 'trx;logfilename=testresults.trx'
dotnet-sonarscanner end /d:sonar.login="$user" /d:sonar.password="$password"
