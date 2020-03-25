del *.class
del httpc.exe
del httpc.jar

javac -cp ".;./../" *.java
jar -cfm httpc.jar .\manif.txt *.class
