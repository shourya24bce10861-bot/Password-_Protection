@echo off
cd /d %~dp0

echo Setting classpath...

set CP=.
set CP=%CP%;lib\jbcrypt-0.4.jar
set CP=%CP%;lib\sqlite-jdbc-3.46.1.0.jar
set CP=%CP%;lib\slf4j-api-1.7.36.jar
set CP=%CP%;lib\slf4j-simple-1.7.36.jar

echo %CP%

echo Compiling...
javac -cp "%CP%" -d . src\Main.java src\ui\*.java src\service\*.java src\database\*.java src\model\*.java

echo Running...
java -cp "%CP%" Main

pause