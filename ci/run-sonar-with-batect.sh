#!/bin/bash
set -e

echo "Running Script"

projectKey="SonarCubeTest"
projectName="SonarCubeTest"

echo "Use E  ${TC_SONAR_QUBE_USE}";
echo "Server E ${TC_SONAR_QUBE_SERVER}";
echo "User E ${TC_SONAR_QUBE_USER}";
echo "Password E ${TC_SONAR_QUBE_PASSWORD}";
echo "Number E ${TC_SONAR_QUBE_NUMBER}";
echo "Version E  ${TC_SONAR_QUBE_VERSION}";

sonarUse=${TC_SONAR_QUBE_USE};
server=${TC_SONAR_QUBE_SERVER};
user=${TC_SONAR_QUBE_USER};
password=${TC_SONAR_QUBE_PASSWORD};
number=${TC_SONAR_QUBE_NUMBER};
version="2.0.${TC_SONAR_QUBE_VERSION}";

echo "SonarUse $sonarUse";
echo "Server $server";
echo "User $user";
echo "Number $number"
echo "Version $version"

#Not needed for now
cd project

if [ "$sonarUse" -eq 1 ]; then
    #If no PR number provided
    if [[ $number == "NOT_SET" ]]; then
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
fi

# #https://stackoverflow.com/questions/69368514/how-can-i-properly-generate-both-trx-files-and-code-coverage-results-with-one-ca
set +e
dotnet test TCSonarCube.sln -c Release -p:CollectCoverage=true -p:CoverletOutputFormat=opencover%2cteamcity --results-directory "/test-results" --logger 'trx;logfilename=testresults.trx' || error=true
set -e
if [ "$sonarUse" -eq 1 ]; then
    dotnet-sonarscanner end /d:sonar.login="$user" /d:sonar.password="$password"
fi

#Fail the build if there was an error
if [ $error ]
then
    exit 1
fi

