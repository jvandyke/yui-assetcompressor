package com.icontact.buildtools;

import jargs.gnu.CmdLineParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Collection;
import java.util.Vector;

import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;

import com.yahoo.platform.yui.compressor.CssCompressor;
import com.yahoo.platform.yui.compressor.JavaScriptCompressor;

public class AssetCompressor {
	public Options o = null;
	
	public AssetCompressor(Options o) {
		this.o = o;
	}	
	
	/**
	 * Compresses all javascript and css files in the given path.
	 * @param args
	 */
    public static void main(String args[]) {
    	Options o = new Options();
    	o.readFromCommandLine(args);
    	AssetCompressor assetCompressor = new AssetCompressor(o);
        String[] fileArgs = o.getFiles();
        Collection<File> filesToCompress = new Vector<File>();
        
        if (fileArgs == null || fileArgs.length == 0) {
        	System.err.println("No directory or file given.");
        	Options.usage();
        	System.exit(1);
        }
        
        // Get contents of given directory
        for (String path : fileArgs) {
	        File inputPath = new File(path);
			FilenameFilter filter = AssetCompressor.buildFilter(o.ignored);
			filesToCompress.addAll(listFiles(inputPath, filter, true));
        }
        
        boolean hasErrors = assetCompressor.compress(filesToCompress, o);
		
		if (!hasErrors) {
			System.out.println("** Minification of CSS and JavaScript completed successfully **");
		} else {
			System.err.println("** Minification of CSS and JavaScript unsuccessful! **");
			// Exit with non-zero to signify build failure.
			System.exit(1);
		}
    }

	/**
	 * @param filesToCompress
	 * @param o
	 * @return
	 */
	public boolean compress(Collection<File> filesToCompress, Options o) {
		boolean hasErrors = false;
        
		for (File f : filesToCompress) {
			String inputFilename = f.getAbsolutePath();
	    	try {
				AssetErrorReporter errorReporter = new AssetErrorReporter();
				String outputFilename = inputFilename;
				System.out.println("Compressing: " + inputFilename);
				
				if (inputFilename.endsWith(".js")) {
					outputFilename = inputFilename.replace(".js", o.suffix + ".js");
					AssetCompressor.compressJavaScript(inputFilename, outputFilename, o, errorReporter);
				} else if (inputFilename.endsWith(".css")) {
					outputFilename = inputFilename.replace(".css", o.suffix + ".css");
					AssetCompressor.compressCss(inputFilename, outputFilename, o, errorReporter);
				}
				
				if (errorReporter.hasErrors()) {
					System.err.println("SEVERE: in file " + inputFilename);
					errorReporter.reportErrors();
					hasErrors = true;
				}
				
				if (o.verbose && errorReporter.hasWarnings()) {
					System.out.println("WARNING: in file " + inputFilename);
					errorReporter.reportWarnings();
				}
			} catch (Exception e) {
				System.err.println("[Error] " + inputFilename);
				e.printStackTrace();
				hasErrors = true;
			}
		}
		return hasErrors;
	}
    
    /**
     * Build a filter to ignore undesired files.
     * 
     * @param ignored
     * @return FilenameFIlter
     */
    private static FilenameFilter buildFilter(final Collection<String> ignored) {
    	FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				boolean accepted = true;
				
				if (!name.endsWith(".css") && !name.endsWith(".js")) {
					accepted = false;
				} else if (name.matches(".*[.-]min.*")) {
					// Ignore files with standard minified suffix.
					accepted = false;
				} else if (dir.isHidden()) {
					accepted = false;
				} else if (ignored != null && !ignored.isEmpty()) {
					for (String ignore : ignored) {
						String fullPath = dir.getAbsolutePath() + name;
						if (fullPath.matches(ignore)) {
							accepted = false;
							break;
						}
					}
				}
				
				return accepted;
			}
		};
		
		return filter;
    }
    
    /**
     * Compresses given CSS file
     * 
     * @param inputFilename
     * @param outputFilename
     * @param o
     * @param errorReporter
     * @throws IOException
     */
	private static void compressCss(String inputFilename, String outputFilename, Options o, AssetErrorReporter errorReporter) throws IOException {
		Reader in = null;
		Writer out = null;
		try {
			in = new InputStreamReader(new FileInputStream(inputFilename));
			CssCompressor compressor = new CssCompressor(in);
			in.close(); in = null;
			
			out = new OutputStreamWriter(new FileOutputStream(outputFilename), o.charset);
			compressor.compress(out, o.lineBreakPos);
			out.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Compresses given JavaScript file.
	 * 
	 * @param inputFilename - Path to file
	 * @param outputFilename - Path to output file.
	 * @param o - Options object
	 * @param errorReporter - Object to report errors to.
	 * @throws IOException
	 */
	private static void compressJavaScript(String inputFilename, String outputFilename, Options o, AssetErrorReporter errorReporter) throws IOException {
		Reader in = null;
		Writer out = null;
		try {
			in = new InputStreamReader(new FileInputStream(inputFilename));
			JavaScriptCompressor compressor = new JavaScriptCompressor(in, errorReporter);
			in.close(); in = null;

			out = new OutputStreamWriter(new FileOutputStream(outputFilename), o.charset);
			compressor.compress(out, o.lineBreakPos, o.munge, o.verbose, o.preserveAllSemiColons, o.disableOptimizations);
			out.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * List all files in a given directory.  Filter can be used to remove undesired files.
	 * 
	 * @param directory
	 * @param filter
	 * @param recurse
	 * @return
	 */
	private static Collection<File> listFiles(File directory, FilenameFilter filter, boolean recurse) {
		// List of files / directories
		Vector<File> files = new Vector<File>();
		
		// Get files / directories in the directory
		File[] entries = directory.listFiles();
		
		// Go over entries
		for (File entry : entries) {
			// If there is no filter or the filter accepts the 
			// file / directory, add it to the list
			if (filter == null || filter.accept(directory, entry.getName())) {
				files.add(entry);
			}
			
			// If the file is a directory and the recurse flag
			// is set, recurse into the directory
			if (recurse && entry.isDirectory()) {
				files.addAll(listFiles(entry, filter, recurse));
			}
		}
		
		// Return collection of files
		return files;		
	}
	
	/**
	 * Error reporter for an instantiation of YUICompressor.
	 */
	private static class AssetErrorReporter implements ErrorReporter {
		public Vector<String> warnings = new Vector<String>();
		public Vector<String> errors = new Vector<String>();
		
		/* (non-Javadoc)
		 * @see org.mozilla.javascript.ErrorReporter#warning(java.lang.String, java.lang.String, int, java.lang.String, int)
		 */
		public void warning(String message, String sourceName, int line, String lineSource, int lineOffset) {
			if (line < 0) {
				warnings.add(message);
			} else {
				warnings.add(line + ':' + lineOffset + ": " + message);
			}
		}
		
		/* (non-Javadoc)
		 * @see org.mozilla.javascript.ErrorReporter#error(java.lang.String, java.lang.String, int, java.lang.String, int)
		 */
		public void error(String message, String sourceName, int line, String lineSource, int lineOffset) {
			if (line < 0) {
				errors.add(message);
			} else {
				errors.add(line + ':' + lineOffset + ": " + message);
			}
		}
		
		/* (non-Javadoc)
		 * @see org.mozilla.javascript.ErrorReporter#runtimeError(java.lang.String, java.lang.String, int, java.lang.String, int)
		 */
		public EvaluatorException runtimeError(String message, String sourceName, int line, String lineSource, int lineOffset) {
			error(message, sourceName, line, lineSource, lineOffset);
			return new EvaluatorException(message);
		}
		
		/**
		 * Returns true if there are errors.
		 * @return boolean
		 */
		public boolean hasErrors() {
			return !errors.isEmpty();
		}
		
		/**
		 * Prints all errors reported to this instance.
		 */
		public void reportErrors() {
			for (String error : errors) {
				System.err.println("SEVERE: " + error);
			}
		}
		
		/**
		 * Returns true if there are warnings.
		 * @return boolean
		 */
		public boolean hasWarnings() {
			return !warnings.isEmpty();
		}
		
		/**
		 * Print all warnings reported to this instance.
		 */
		public void reportWarnings() {
			for (String warning : warnings) {
				System.err.println("WARNING: " + warning);
			}
		}
	}
	
	/**
	 * Read expected arguments from the command line.
	 * Credit: Mostly taken from the YUICompressor script. 
	 */
	private static class Options {
		private CmdLineParser clp = new CmdLineParser();
		public String charset = "UTF-8";
		public String suffix = "-min";
		public int lineBreakPos = -1;
		public boolean munge = true;
		public boolean verbose = true;
		public boolean preserveAllSemiColons = false;
		public boolean disableOptimizations = false;
		public Collection<String> ignored = null;
		
		/**
		 * Used to get options from command line.
		 * @param args
		 */
		public void readFromCommandLine(String args[]) {
	        CmdLineParser.Option verboseOpt = clp.addBooleanOption('v', "verbose");
	        CmdLineParser.Option nomungeOpt = clp.addBooleanOption("nomunge");
	        CmdLineParser.Option linebreakOpt = clp.addStringOption("line-break");
	        CmdLineParser.Option preserveSemiOpt = clp.addBooleanOption("preserve-semi");
	        CmdLineParser.Option disableOptimizationsOpt = clp.addBooleanOption("disable-optimizations");
	        CmdLineParser.Option helpOpt = clp.addBooleanOption('h', "help");
	        CmdLineParser.Option charsetOpt = clp.addStringOption("charset");
	        CmdLineParser.Option suffixOpt = clp.addStringOption('s', "suffix");
	        CmdLineParser.Option ignoreOpt = clp.addStringOption('i', "ignore");
	        
	        try {
		        clp.parse(args);

		        Boolean help = (Boolean) clp.getOptionValue(helpOpt);
		        if (help != null && help.booleanValue()) {
		        	usage();
		        	System.exit(0);
		        }
		        
		        String optCharset = (String) clp.getOptionValue(charsetOpt);
		        if (optCharset != null) {
		        	charset = optCharset;
		        }
		        
		        String optSuffix = (String) clp.getOptionValue(suffixOpt);
		        if (optSuffix != null) {
		        	suffix = optSuffix;
		        }
		        
		        String optLineBreakPos = (String) clp.getOptionValue(linebreakOpt);
		        if (optLineBreakPos != null) {
	                try {
	                    lineBreakPos = Integer.parseInt(optLineBreakPos, 10);
	                } catch (NumberFormatException e) {
	                    usage();
	                    System.exit(1);
	                }
	            }
		        
		        while (true) {
		        	String ignoreFile = (String) clp.getOptionValue(ignoreOpt);
		        	if (ignoreFile == null) {
		        		break;
		        	} else {
		        		ignored.add(ignoreFile);
		        	}
		        }
		        
		        verbose = clp.getOptionValue(verboseOpt) != null;
		        munge = clp.getOptionValue(nomungeOpt) == null;
		        preserveAllSemiColons = clp.getOptionValue(preserveSemiOpt) != null;
		        disableOptimizations = clp.getOptionValue(disableOptimizationsOpt) != null;
	        }
	        catch (CmdLineParser.OptionException e) {
	        	usage();
                System.exit(1);
	        }
		}
		
		/**
		 * Returns options that aren't proceeded by an option flag.
		 * @return
		 */
		public String[] getFiles() {
			return clp.getRemainingArgs();
		}
		
		/**
		 * Prints the usage text.
		 */
		public static void usage() {
			System.err.println(
					"\nUsage: java -Xbootclasspath/p:<path to yui_compressor jar> -jar minify.jar <input directory>\n\n"

					+ "Global Options\n"
					+ "  -h, --help                Displays this information\n"
					+ "  --charset <charset>       Read the input file using <charset>\n"
					+ "  --line-break <column>     Insert a line break after the specified column number\n"
					+ "  -v, --verbose             Display informational messages and warnings\n"
					+ "  -s, --suffix <suffix>         Append <suffix> to output file before file extension.\n"
					+ "                            Default: -min"
					+ "  -i, --ignore <pattern>    Ignore file paths matching the given <pattern>. May be used multiple"
					+ "                            times.\n\n"

					+ "JavaScript Options\n"
					+ "  --nomunge                 Minify only, do not obfuscate\n"
					+ "  --preserve-semi           Preserve all semicolons\n"
					+ "  --disable-optimizations   Disable all micro optimizations");
		}
	}
}
