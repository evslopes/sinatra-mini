@echo off
REM Minimal mvnw.cmd shim: delegates to system mvn if available.
where mvn >nul 2>&1
if %errorlevel%==0 (
  mvn %*
) else (
  echo Maven not found. Install Maven and ensure 'mvn' is on PATH.
  exit /b 127
)
