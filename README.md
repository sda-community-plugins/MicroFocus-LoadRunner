# Micro Focus LoadRunner plugin

The _Micro Focus LoadRunner_ plugin is a quality automation based plugin. 
It is run during development and deployment process to automate the execution of performance tests.

This plugin is a work in progress but it is intended to provide the following steps:

* [x] **Execute LoadRunner Scenario (Wlrun)** - Execute a LoadRunner Scenario using Wlrun.exem
* [x] **Analyze LoadRunner Scenario (AnalysisUI)** - Analyze an executed LoadRunner scenario and produce a HTML Report using AnalysisUI.exe

The plugin currently uses the _WlRun.exe_ and _Analysis.exe_ executables - the intention is to use the _HpToolsLauncher_
and _LRAnalysisLauncher_ executables from the [Micro Focus Application Automation Tools](https://github.com/jenkinsci/hpe-application-automation-tools-plugin/)
plugin for more configurability and stability at a later date.

### Installing the plugin
 
Download the latest version from the _release_ directory and install into Deployment Automation from the 
**Administration\Automation\Plugins** page.

Because of the nature of LoadRunner and its integration this plugin only works on Windows based systems.

### Building the plugin

To build the plugin you will need to clone the following repositories (at the same level as this repository):

 - [mavenBuildConfig](https://github.com/sda-community-plugins/mavenBuildConfig)
 - [plugins-build-parent](https://github.com/sda-community-plugins/plugins-build-parent)
 - [air-plugin-build-script](https://github.com/sda-community-plugins/air-plugin-build-script)
 
 and then compile using the following command
 ```
   mvn clean package
 ```  

This will create a _.zip_ file in the `target` directory when you can then install into Deployment Automation
from the **Administration\Automation\Plugins** page.

If you have any feedback or suggestions on this template then please contact me using the details below.

Kevin A. Lee

kevin.lee@microfocus.com

**Please note: this plugins is provided as a "community" plugin and is not supported by Micro Focus in any way**.
