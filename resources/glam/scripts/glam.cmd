@echo off

set extra_path=""
for /f "delims=" %%i in ('clojure -M:glam %*') do set extra_path="%%i"
echo "extra: %extra_path%"
if not "%extra_path%"=="" (
  set PATH=%extra_path%;%PATH%
)
set extra_path=
