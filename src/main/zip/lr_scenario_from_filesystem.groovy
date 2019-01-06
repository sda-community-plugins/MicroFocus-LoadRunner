// --------------------------------------------------------------------------------
// Execute LoadRunner tests from the filesystem
// --------------------------------------------------------------------------------

import com.serena.air.plugin.loadrunner.*

import com.serena.air.StepFailedException
import com.serena.air.StepPropertiesHelper
import com.urbancode.air.AirPluginTool

//
// Create some variables that we can use throughout the plugin step.
// These are mainly for checking what operating system we are running on.
//
final def PLUGIN_HOME = System.getenv()['PLUGIN_HOME']
final String lineSep = System.getProperty('line.separator')
final String osName = System.getProperty('os.name').toLowerCase(Locale.US)
final String pathSep = System.getProperty('path.separator')
final boolean windows = (osName =~ /windows/)
final boolean vms = (osName =~ /vms/)
final boolean os9 = (osName =~ /mac/ && !osName.endsWith('x'))
final boolean unix = (pathSep == ':' && !vms && !os9)

//
// Initialise the plugin tool and retrieve all the properties that were sent to the step.
//
final def  apTool = new AirPluginTool(this.args[0], this.args[1])
final def  props  = new StepPropertiesHelper(apTool.getStepProperties(), true)

//
// Set a variable for each of the plugin steps's inputs.
// We can check whether a required input is supplied (the helper will fire an exception if not) and
// if it is of the required type.
//
File workDir = new File('.').canonicalFile
String lrInstallDir = props.notNull('lrInstallDir')
String testPath = props.notNull('testPath')
String lrResultsName = props.notNull('lrResultsName')
boolean debugMode = props.optionalBoolean("debugMode", false)

// are we running on windows?
if (!windows) {
    println("Sorry, this plugin step is only supported on Windows!")
    System.exit(2);
}

println "----------------------------------------"
println "-- STEP INPUTS"
println "----------------------------------------"

//
// Print out each of the property values.
//
println "Working directory: ${workDir.canonicalPath}"
println "LoadRunner Installation Directory: ${lrInstallDir}"
println "Test Scenario Path: ${testPath}"
println "LoadRunner Results Name: ${lrResultsName}"
println "Debug mode value: ${debugMode}"
if (debugMode) { props.setDebugLoggingMode() }

println "----------------------------------------"
println "-- STEP EXECUTION"
println "----------------------------------------"

int exitCode = 0

//
// The main body of the plugin step - wrap it in a try/catch statement for handling any exceptions.
//
try {
    LoadRunnerHelper wlHelper = new LoadRunnerHelper(lrInstallDir, workDir)
    wlHelper.setDebugMode(debugMode)
    exitCode = wlHelper.runTestScenario(testPath, lrResultsName)
} catch (StepFailedException e) {
    //
    // Catch any exceptions we find and print their details out.
    //
    println "ERROR - ${e.message}"
    // An exit with a non-zero value will be deemed a failure
    System.exit 1
}

//
// An exit with a zero value means the plugin step execution will be deemed successful.
//
System.exit(exitCode)
