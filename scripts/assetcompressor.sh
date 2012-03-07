#!/bin/sh

# Find out where we're starting
baseDir=$(dirname $_)
baseDir="${baseDir}/.."
buildDir="${baseDir}/build"
yuiBootClassPath="${baseDir}/lib/yuicompressor-2.4.2.jar"
args="$*"

# Options
minSuffix="-min";

# Command used on both JS and CSS files to minify content
minifyCommand="java -Xbootclasspath/p:${yuiBootClassPath} -jar ${baseDir}/assetcompressor.jar --suffix ${minSuffix}"

${minifyCommand} ${args}
