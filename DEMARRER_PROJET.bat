@echo off
setlocal
cd /d "%~dp0"
where mvn >nul 2>&1
if errorlevel 1 (
    echo Maven est introuvable. Installez Maven et ajoutez-le au PATH.
    exit /b 1
)
set "CODRIVE_DEMO_DATA=true"
echo Demarrage de l'API Codrive avec les donnees de demonstration...
start "API Codrive" cmd /k "mvn spring-boot:run"
powershell -NoProfile -Command "$deadline = (Get-Date).AddSeconds(60); do { try { $null = Invoke-WebRequest -UseBasicParsing -Uri 'http://127.0.0.1:8081/api/trajets' -TimeoutSec 2; exit 0 } catch { Start-Sleep -Seconds 1 } } while ((Get-Date) -lt $deadline); exit 1"
if errorlevel 1 (
    echo L'API n'a pas repondu dans le delai prevu. Consultez la fenetre API Codrive.
    exit /b 1
)
start "" "%~dp0login.html"
endlocal
