rem java -cp ".;./json-20190722.jar;./dom4j-2.0.2.jar;./jsoup-1.13.1.jar" HTTPFS -p 8088 -d "/filedir/subdir/" -v


del *.class
del httpfs.exe
del httpfs.jar

javac -cp ".;./../SelectiveRepeat;./json-20190722.jar;./dom4j-2.0.2.jar;./jsoup-1.13.1.jar" *.java
jar -cfm httpfs.jar .\manif.txt *.class
