@echo off
REM echo "path before: %PATH%"
set extra_path=
for /f "delims=" %%i in ('glam-bin %*') do set "extra_path=%%i"
if not "%extra_path%"=="" (
  set "PATH=%extra_path%;%PATH%"
)
REM echo "path after: %PATH%"
set extra_path=
