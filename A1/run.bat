del *.class
del httpc.exe
del httpc.jar

javac *.java
jar -cfm httpc.jar .\manif.txt *.class
