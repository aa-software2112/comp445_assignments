In order to create the executable:
1. Run the "run.bat" in order to compile the classes and create the necessary JAR file
2. Use Launch4j to create the executable - use the instructions found in https://stackoverflow.com/Questions/804466/how-do-i-create-executable-java-program to help with the process. Make sure Launch4j has "Header Type" as "Console" under the "Header" tab... The button that looks like a settings button actually generates the .exe! The "Jar" field must be specified, (from step 1), as well as the "Output file".

Note to marker:
1. The main() function is in driver.java, and the httpc.exe is the executable responsible for the "httpc" client.