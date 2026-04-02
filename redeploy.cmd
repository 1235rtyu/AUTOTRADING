@echo off
setlocal
chcp 65001 >nul

set "PROJECT_DIR=%~dp0"
set "TOMCAT_HOME=D:\apache-tomcat-9.0.115"
set "CATALINA_HOME=%TOMCAT_HOME%"
set "CATALINA_BASE=%TOMCAT_HOME%"
set "JAVA_HOME=C:\Program Files\Java\jdk-11"
set "WAR_NAME=auto-trading.war"
set "APP_DIR=auto-trading"
set "MVN_CMD="
set "CATALINA_OPTS=-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 %CATALINA_OPTS%"
set "MAVEN_USER_HOME=%PROJECT_DIR%.m2home"
set "MAVEN_REPO_LOCAL=%PROJECT_DIR%.m2repo"
set "BUILD_LOG=%PROJECT_DIR%redeploy-build.log"

rem Ensure JAVA_HOME is applied for this session
set "PATH=%JAVA_HOME%\bin;%PATH%"

for /f "delims=" %%M in ('where mvn.cmd 2^>nul') do (
  set "MVN_CMD=%%M"
  goto :mvn_found
)
for /f "delims=" %%M in ('where mvn.bat 2^>nul') do (
  set "MVN_CMD=%%M"
  goto :mvn_found
)
for /f "delims=" %%M in ('where mvn 2^>nul') do (
  set "MVN_CMD=%%M"
  goto :mvn_found
)
if not defined MVN_CMD (
  if exist "D:\apache-maven-3.9.9\bin\mvn.cmd" (
    set "MVN_CMD=D:\apache-maven-3.9.9\bin\mvn.cmd"
  ) else if exist "C:\Users\mh\Downloads\org.apache.Maven-3.9.9-apache-maven-3.9.9-bin\apache-maven-3.9.9\bin\mvn.cmd" (
    set "MVN_CMD=C:\Users\mh\Downloads\org.apache.Maven-3.9.9-apache-maven-3.9.9-bin\apache-maven-3.9.9\bin\mvn.cmd"
  ) else if exist "C:\Users\mh\Downloads\apache-maven-3.9.9-bin\apache-maven-3.9.9\bin\mvn.cmd" (
    set "MVN_CMD=C:\Users\mh\Downloads\apache-maven-3.9.9-bin\apache-maven-3.9.9\bin\mvn.cmd"
  ) else (
    echo [ERROR] mvn command not found.
    echo [HINT] Add Maven bin to PATH, or update MVN_CMD in redeploy.cmd.
    pause
    exit /b 1
  )
)
:mvn_found

if not exist "%TOMCAT_HOME%\bin\startup.bat" (
  echo [ERROR] TOMCAT_HOME is invalid: %TOMCAT_HOME%
  pause
  exit /b 1
)
if not exist "%JAVA_HOME%\bin\java.exe" (
  echo [ERROR] JAVA_HOME is invalid: %JAVA_HOME%
  echo [HINT] JDK path mismatch. Update JAVA_HOME in redeploy.cmd.
  pause
  exit /b 1
)

pushd "%PROJECT_DIR%"
echo [1/4] Building WAR...
if not exist "%MAVEN_USER_HOME%" mkdir "%MAVEN_USER_HOME%"
if not exist "%MAVEN_REPO_LOCAL%" mkdir "%MAVEN_REPO_LOCAL%"
call "%MVN_CMD%" -DskipTests clean package > "%BUILD_LOG%" 2>&1
if errorlevel 1 (
  echo [WARN] Default build failed. Retrying with local repo under project...
  call "%MVN_CMD%" -Dmaven.user.home="%MAVEN_USER_HOME%" -Dmaven.repo.local="%MAVEN_REPO_LOCAL%" -DskipTests clean package >> "%BUILD_LOG%" 2>&1
)
if errorlevel 1 goto :fail

echo [2/4] Stopping Tomcat...
call "%TOMCAT_HOME%\bin\shutdown.bat" >nul 2>&1
timeout /t 2 /nobreak >nul

echo [3/4] Deploying WAR...
if exist "%TOMCAT_HOME%\webapps\%WAR_NAME%" del /q "%TOMCAT_HOME%\webapps\%WAR_NAME%"
if exist "%TOMCAT_HOME%\webapps\%APP_DIR%" rmdir /s /q "%TOMCAT_HOME%\webapps\%APP_DIR%"
copy /y "target\%WAR_NAME%" "%TOMCAT_HOME%\webapps\%WAR_NAME%" >nul
if errorlevel 1 goto :fail

echo [4/4] Starting Tomcat...
call "%TOMCAT_HOME%\bin\startup.bat"

echo [OK] Redeploy complete.
echo [URL] http://localhost:8080/%APP_DIR%
popd
pause
exit /b 0

:fail
echo [ERROR] Redeploy failed.
echo [LOG] %BUILD_LOG%
powershell -NoProfile -Command "if (Test-Path '%BUILD_LOG%') { Get-Content -Tail 60 -Path '%BUILD_LOG%' }"
popd
pause
exit /b 1
