# Chromium Conversion Server
> Praveen Sakthivel 8/13/17

Server will automatically handle the build proccess to convert Chrome apps to standalone Mac Apps and Universal Windows Apps

# Installation Instructions

  - Install Desktop App Converter from the Windows Store
  - Download all Files and place them in an accessible directory
  - Open ServerProject in an IDE of your choice
  - Go through the main class and change all file paths to match your system
  - Change the jenkins link and corresponding credentials for your desired build
  - Change the Azure Storage connection string and credentials to match your desired output location or modify uploadBuild() for your desired cloud storage
  - Build project and export the executable jar to an accesible directory
  - Open the document linked below and complete steps 6-10
  - Open ServerFiles directory and open all powershell and batch scripts
  - Change any file paths to match your system
  - Modify Windows settings to run the StartUp.bat upon system startup
  - Set UAC security to lowest setting
  - Open port 28 on your system for TCP

# Warnings
  - Server will only run on Windows 10 with Windows Store enabled
  - A static IP will be required for this server
  - Server is not designed to handle multiple builds within one server session. Expect to restart or shutdown server after every build

# Updating NW.JS
 - Windows NW.JS templates can be changed without a problem
 - Mac NW.JS templates must be edited
 - Open the nwjs.app and navigate to Contents>Versions>CurrentVersion>nwjsFramework
 - Delete all the aliases and then navigate from this directory to Versions>CurrentVersions
 - Move all the files in this directory to the where the aliases were stored.

> To manually convert builds refer to this [document]

[document]: <https://docs.google.com/a/pearson.com/document/d/1NlLtc8eSIcqjTflX-dqF1cr5RCYmCeYFqgi2G1xFNK8/edit?usp=sharing>
