package com.serena.air.plugin.loadrunner

import com.serena.air.StepFailedException

import java.io.File
import java.text.SimpleDateFormat
import java.util.List
import java.util.regex.Matcher
import java.util.regex.Pattern
import org.apache.commons.io.FileUtils
import org.ini4j.Wini

class LoadRunnerHelper {
    private File workDir
    private File lrInstallDir
    private File wlRunExe
    private File analysisUIExe
    private boolean debugMode = false

    /* Non-configurable fields */
    private final String DA_ANALYSIS_TEMPLATE = "DA_ANALYSIS_TEMPLATE" // Custom template name
    private final String LR_CONFIG_FILE = "LRAnalysis80.ini" // INI file used to import templates
    private final String TEMPLATE_SECTION = "AvailableTemplates" // Section of the INI file that contains templates

    public setDebugMode(boolean debuMode) {
        this.debugMode = debugMode
    }

    public LoadRunnerHelper(String lrInstallPath, File workDir) {
        this.workDir = workDir
        lrInstallDir = resolveFilePath(lrInstallPath)
        wlRunExe = new File(lrInstallDir, "bin" + File.separator + "Wlrun.exe")
        analysisUIExe = new File(lrInstallDir, "bin" + File.separator + "AnalysisUI.exe")
    }

    /* Execute an LRS file and analyze the results. */
    public int runTestScenario(String testPath, String lrResultsName) {
        File testFile = resolveFilePath(testPath)

        List<String> cmdArgs = [
            wlRunExe.absolutePath,
            "-TestPath",
            testFile.absolutePath,
            "-Run",
            "-ResultLocation",
            workDir.absolutePath,
            "-ResultCleanName",
            lrResultsName]

        println("Running test scenario '${testPath}'.")
        int exitVal = executeCLICommand(cmdArgs)
        if (exitVal != 0) {
            println("Test scenario failed with exit code: ${exitVal}")
        } else {
            println("Test scenario has executed successfully.")
        }

        return exitVal
    }

    /* Execute analysis on the generated LRR file and create an HTML report using the given template. */
    public int runTestAnalysis(String templateName, String lrResultsName) {
        File resultsFile = new File(workDir, lrResultsName + File.separator + "${lrResultsName}.lrr")

        if (!resultsFile.isFile()) {
            throw new StepFailedException("Results file doesn't exist at '${resultsFile.absolutePath}'.")
        }

        /* Copy and import the DA analysis template if it has been specified */
        if (DA_ANALYSIS_TEMPLATE.equals(templateName)
            && (copyTemplate() != 0 || importDATemplate() != 0))
        {
            return -1
        }

        List<String> cmdArgs = [
            analysisUIExe.absolutePath,
            "-RESULTPATH",
            resultsFile.absolutePath,
            "-TEMPLATENAME",
            templateName]

        /* Generate the HTML analysis file */
        println("Analyzing the test results.")
        int exitVal = executeCLICommand(cmdArgs)
        if (exitVal != 0) {
            println("Test results analysis failed with exit code: ${exitVal}")
        } else {
            println("Completed test result analysis.")
        }

        return exitVal
    }

    public static String getTimestamp() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("[MM/dd/yyyy HH:mm:ss]")

        return dateFormat.format(new Date())
    }

    /* Copy the DA analysis template from PLUGIN_HOME to the working directory
     * This must be done, otherwise LoadRunner would delete the template permanently if the user deleted
     * the template using the LoadRunner Analysis tool.
     */
    private int copyTemplate() {
        File sourceTemplateDir = new File(System.getenv("PLUGIN_HOME"), DA_ANALYSIS_TEMPLATE)
        File destTemplateDir = new File(workDir, DA_ANALYSIS_TEMPLATE)

        try {
            if (this.debugMode) println("DEBUG - Copying DA analysis template from ${sourceTemplateDir} to ${workDir}.")
            FileUtils.copyDirectory(sourceTemplateDir, destTemplateDir)
        }
        catch (IOException ex) {
            throw new StepFailedException("Failed to copy DA analysis template from ${sourceTemplateDir} to ${destTemplateDir}." +
                ex.printStackTrace())
        }

        if (this.debugMode) println("DEBUG - Successfully copied DA analysis template.")
        return 0
    }

    /* Import DA analysis template if it has not already been imported */
    private int importDATemplate() {
        File analysisConfig = new File(lrInstallDir, "config" + File.separator + LR_CONFIG_FILE)

        if (!analysisConfig.exists()) throw new StepFailedException(
                "Analysis config file '${analysisConfig.absolutePath}' does not exist." +
                "Please startup the Analysis tool at least once on the server.")

        println("Importing DA analysis template in LoadRunner Analysis.")

        /* Import/Update the template if the agent user has permissions to edit analysis config */
        if (analysisConfig.canRead() && analysisConfig.canWrite()) {
            Wini analysisIni = new Wini(analysisConfig)

            /* INI file must contain AvailableTemplates section */
            if (analysisIni.keySet().contains(TEMPLATE_SECTION)) {
                String newTemplatePath = workDir.absolutePath + File.separator
                int numOfTemplates = analysisIni.get(TEMPLATE_SECTION, "NumOfTemplates", int.class)

                /* Update the path to the template if it has already been imported */
                boolean templateImported = updateTemplatePath(analysisIni, newTemplatePath)

                /* Import DA_ANALYSIS_TEMPLATE if it has never been imported */
                if (!templateImported) {
                    /* Insert the new template entry into the INI file and update template count */
                    analysisIni.put(TEMPLATE_SECTION, "Template${numOfTemplates}_Name", DA_ANALYSIS_TEMPLATE)
                    analysisIni.put(TEMPLATE_SECTION, "Template${numOfTemplates}_Path", newTemplatePath)
                    analysisIni.put(TEMPLATE_SECTION, "NumOfTemplates", ++numOfTemplates)
                    analysisIni.store()

                    println("DA analysis template has been successfully imported.")
                }
                else {
                    println("DA analysis template path has been updated.")
                }
            }
            else {
               throw new StepFailedException("${analysisConfig.absolutePath} file is missing 'AvailableTemplates' section.")
            }
        }
        else {
            throw new StepFailedException("Template import failed. DA agent user doesn't have read/write permissions to " +
                "file path '${analysisConfig.absolutePath}'. " +
                "You may create your own custom analysis template or fix the file permissions. " +
                "Please see the plugin documentation for further details.")
        }

        return 0
    }

    /* Update current DA analysis template path and return if it exists in the analysis config. */
    private boolean updateTemplatePath(Wini analysisIni, String templatePath) {
        Pattern p = Pattern.compile("(Template[0-9]+)_Name") // Pattern to match Template<number>_Name key

        /* Iterate through all templates to check if DA_ANALYSIS_TEMPLATE already exists */
        for (String keyName : analysisIni.get(TEMPLATE_SECTION).keySet()) {
            Matcher m = p.matcher(keyName)

            if (m.find()) {
                /* Get the template name from the whole matched expression */
                String templateName = analysisIni.get(TEMPLATE_SECTION, m.group(0))

                /* Template named DA_ANALYSIS_TEMPLATE will exist if it's already been imported */
                if (DA_ANALYSIS_TEMPLATE.equals(templateName)) {
                    println("DA analysis template was already imported, updating path.")
                    String pathKey = m.group(1) + "_Path"   // The name of the template's path key
                    analysisIni.put(TEMPLATE_SECTION, pathKey, templatePath)
                    analysisIni.store()
                    return true
                }
            }
        }

        return false
    }

    /* Resolve a relative or absolute file system path to a file or directory */
    private File resolveFilePath(String path) {
        File entry = new File(path)

        /* Do not build path with workDir if user specifies an absolute file path */
        if (!entry.isAbsolute()) {
            entry = new File(workDir, path)
        }

        if (!entry.exists()) {
            throw new FileNotFoundException("File system path '${entry.absolutePath}' does not exist.")
        }

        return entry
    }

    /* Run the CLI command. */
    private int executeCLICommand(List<String> commandLine) {
        def exitCode = 0
        // print out command info
        println("INFO - Executing: ${commandLine.join(' ')}")

        //
        // Launch Process
        //
        final def processBuilder = new ProcessBuilder(commandLine as String[]).directory(workDir)
        final def process = processBuilder.start()
        process.out.close() // close stdin
        process.waitForProcessOutput(System.out, System.err) // forward stdout and stderr
        process.waitFor()

        exitCode = process.exitValue()

        if (debugMode) println "DEBUG - command exit code: ${exitCode}"
        return exitCode
    }
}
