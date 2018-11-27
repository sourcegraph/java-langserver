#!/bin/bash

if [ -z ${ANDROID_HOME} ]; then
    echo "\$ANDROID_HOME is not set, exiting"
    exit 1
fi

if [ ! -d ./sdk-deployer ]; then
    git clone https://github.com/simpligility/maven-android-sdk-deployer ./sdk-deployer
fi

cd ./sdk-deployer

LANGSERVER_ROOT=${LANGSERVER_ROOT:-$HOME/.java-langserver}
MAVEN_OPTS="-Dmaven.repo.local=${LANGSERVER_ROOT}/artifacts" mvn install

cd -
