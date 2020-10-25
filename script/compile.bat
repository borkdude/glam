@echo off

if "%GRAALVM_HOME%"=="" (
    echo Please set GRAALVM_HOME
    exit /b
)

set JAVA_HOME=%GRAALVM_HOME%
set PATH=%GRAALVM_HOME%\bin;%PATH%

set /P GLAM_VERSION=< resources\glam\version
echo Building glam-bin %GLAM_VERSION%

if "%GRAALVM_HOME%"=="" (
echo Please set GRAALVM_HOME
exit /b
)

set PATH=%USERPROFILE%\deps.clj;%PATH%

if not exist "classes" mkdir classes
call deps -e "(compile 'glam.main)"
deps -Spath > .classpath
set /P GLAM_CLASSPATH=<.classpath

call %GRAALVM_HOME%\bin\gu.cmd install native-image

call %GRAALVM_HOME%\bin\native-image.cmd ^
  "-cp" "%GLAM_CLASSPATH%;classes" ^
  "-H:Name=glam-bin" ^
  "-H:+ReportExceptionStackTraces" ^
  "--initialize-at-build-time" ^
  "-H:IncludeResources=glam/.*" ^
  "-J-Dclojure.spec.skip-macros=true" ^
  "-J-Dclojure.compiler.direct-linking=true" ^
  "--report-unsupported-elements-at-runtime" ^
  "-H:EnableURLProtocols=http,https,jar" ^
  "--enable-all-security-services" ^
  "--verbose" ^
  "--no-fallback" ^
  "--no-server" ^
  "-J-Xmx3g" ^
  "glam.main"

del .classpath

if %errorlevel% neq 0 exit /b %errorlevel%

echo Creating zip archive
jar -cMf glam-%GLAM_VERSION%-windows-amd64.zip glam-bin.exe
