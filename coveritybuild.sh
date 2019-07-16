#!/bin/bash

export PATH=$PATH:/var/jenkins_home/cov-analysis-linux64-2019.06/bin


echo "Coverity configure command"
cov-configure --java

echo "Coverity build command"
cov-build --dir $IDIR ./gradlew clean build --no-daemon

echo "Coverity import scm"
cov-import-scm --dir $IDIR --scm git

echo "Coverity Analysis command: $PROJECT"
cov-analyze --dir $IDIR --all  --webapp-security --webapp-security-preview --preview --concurrency --security --distrust-all --strip-path `pwd`

echo "Coverity Commit command preview report"
cov-commit-defects --dir $IDIR --preview-report-v2 report.json --ssl --on-new-cert trust --host coverity.ocset.net  --dataport 9090 --auth-key-file $COVERITY_AUTH_KEY --stream "$STREAM" --scm git

echo "Coverity commit command"
cov-commit-defects --dir $IDIR --host coverity.ocset.net --dataport 9090 --ssl -on-new-cert trust  --auth-key-file $COVERITY_AUTH_KEY --stream "$STREAM" --scm git

jq -e '.issueInfo[] | select (.presentInComparisonSnapshot == false)' report.json > output.json

ERROR_STATUS=$?
echo $ERROR_STATUS
if [ $ERROR_STATUS -eq 0 ]; then
     echo "New introduced defect: Jenkins fail";
     cat output.json
     exit 1;
fi


