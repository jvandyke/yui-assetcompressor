Asset Compressor
================

A wrapper for the Yahoo! JavaScript and CSS Compressor intended for use during a build process.

# Usage

	$ java -Xbootclasspath/p:<path to yui_compressor jar> -jar assetcompressor.jar [options] <input directory> [<input directory> ...]
	
	Global Options
	  -h, --help                Displays this information
	  --charset <charset>       Read the input file using <charset>
	  --line-break <column>     Insert a line break after the specified column number
	  -v, --verbose             Display informational messages and warnings
	  -s, --suffix <suffix>     Append <suffix> to output file before file extension.
	                            Default: -min
	  -i, --ignore <pattern>    Ignore file paths matching the given <pattern>. May be used multiple
	                            times.
	
	JavaScript Options
	  --nomunge                 Minify only, do not obfuscate
	  --preserve-semi           Preserve all semicolons
	  --disable-optimizations   Disable all micro optimizations

## Shell script example

	$ ./assetcompressor.sh [opts]

Look at the above script in `scripts/` for a good example of how
to run this via a shell script executable from any directory.

## Ignoring files and paths

Sometimes you need to ignore certain directories or files because they are already
compressed.  You can ignore these by specifying one or multiple patterns to ignore
with the `--ignore <pattern>` option.  The given pattern will be matched against
the absolute path of the file.

The pattern is used in a standard Java regular expression as such:

	filepath.match("/.*?" + <pattern> + ".*?/");

### Example

	$ ./assetcompressor.sh ../static --ignore "/jquery/" --ignore "\.compressed"

The above example will ignore any directory with `/jquery/` in the path and any
file or directory with `.compressed` in the file name or path.

# Description

Asset Compressor is a wrapper for YUI Compressor for use in a build system,
such as Bamboo.  It provides what we feel as a few necessary features to
the wonderful YUI Compressor runner.

* Recursive directory traversing
* Better build tool reporting (File names are output along with errors and warnings)
* Better build tool integration (Non-zero exit status for failing builds)
* Capability to ignore files or paths based on regular expressions
* Many times faster than running YUI Compressor once per file

# Notes

Since the Asset Compressor is intended for use in a build system, and
therefore meant to be used in many situations and configurations, YUI
Compressor is not built into the JAR file.  To run YUI Compressor using
this wrapper you need to include the `-Xbootclasspath/p:<path>` option, where
<path> is the path to your desired version of YUI Compressor.  Most often
one would use the latest version of YUI Compressor, but forcing a particular
version along with this wrapper could cause an unexpected error if the version
we decide to bundle breaks something in your application.  Of course, an
unsupported version might break our implementation as well, and if it does,
we'll release a new version to fix it.  You should always be safe with the
version bundled under the `lib/` directory.

## Also
* The asset compressor requires Java version >= 1.5.
* All unmentioned caveats of the YUI Compressor apply here.

# License
License: All code specific to YUI Compressor is issued under a BSD license.

AssetCompressor for YUI Compressor License Agreement (BSD License)

Copyright (c) 2011, iContact Corporation
All rights reserved.