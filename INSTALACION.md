# Instalación e inicialización de NAMI

Guía reproducible para Windows PowerShell. El proyecto usa datos ficticios y el
APK `debug` se conecta exclusivamente a Firebase Emulator Suite.

## 1. Herramientas del sistema

Ejecuta PowerShell como administrador:

```powershell
winget source update
winget install --id Git.Git --exact
winget install --id Google.AndroidStudio --exact
winget install --id Microsoft.OpenJDK.21 --exact
winget install --id CoreyButler.NVMforWindows --exact
```

Cierra y abre PowerShell de nuevo:

```powershell
nvm install 22.14.0
nvm use 22.14.0
npm install --global firebase-tools

git --version
node --version
npm --version
java --version
firebase --version
firebase login
```

## 2. Android SDK y emulador

Abre Android Studio una vez para completar la instalación del SDK y acepta las
licencias. Después ejecuta:

```powershell
$sdk = "$env:LOCALAPPDATA\Android\Sdk"
$sdkmanager = "$sdk\cmdline-tools\latest\bin\sdkmanager.bat"
$avdmanager = "$sdk\cmdline-tools\latest\bin\avdmanager.bat"

& $sdkmanager --sdk_root=$sdk `
  "platform-tools" `
  "platforms;android-37" `
  "build-tools;35.0.0" `
  "emulator" `
  "system-images;android-36;google_apis;x86_64"

& $sdkmanager --licenses

"no" | & $avdmanager `
  create avd `
  --force `
  --name NAMI_API_36 `
  --package "system-images;android-36;google_apis;x86_64" `
  --device "pixel_2"
```

## 3. Clonar y preparar el proyecto

```powershell
git clone https://github.com/Proyecto-NAMI/2DAM-G06-MVP.git
Set-Location .\2DAM-G06-MVP

$sdk = "$env:LOCALAPPDATA\Android\Sdk"
$sdkDir = $sdk -replace '\\','/'

"sdk.dir=$sdkDir" | Set-Content .\local.properties
Copy-Item .\firebase.local.properties.example .\firebase.local.properties

npm --prefix functions ci
npm --prefix functions test
npm --prefix functions run build
```

## 4. Compilar el APK debug

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
```

El APK queda en:

```text
app\build\outputs\apk\debug\app-debug.apk
```

## 5. Arrancar el emulador Android

```powershell
$sdk = "$env:LOCALAPPDATA\Android\Sdk"
$adb = "$sdk\platform-tools\adb.exe"

Start-Process `
  -FilePath "$sdk\emulator\emulator.exe" `
  -ArgumentList @("-avd","NAMI_API_36","-no-snapshot","-gpu","swiftshader_indirect")

Start-Sleep -Seconds 20
& $adb wait-for-device
& $adb devices
```

## 6. Arrancar Firebase Emulator Suite

Abre otra PowerShell en la raíz del proyecto y déjala abierta:

```powershell
Set-Location C:\ruta\al\2DAM-G06-MVP

npm --prefix functions ci
npm --prefix functions run build

firebase emulators:start `
  --project demo-nami `
  --only auth,firestore,functions,storage
```

Los puertos esperados son:

```text
Auth:       9099
Firestore:  8080
Functions:  5001
Storage:    9199
```

## 7. Instalar y abrir la aplicación

En otra PowerShell:

```powershell
Set-Location C:\ruta\al\2DAM-G06-MVP

$sdk = "$env:LOCALAPPDATA\Android\Sdk"
$adb = "$sdk\platform-tools\adb.exe"

& $adb install -r .\app\build\outputs\apk\debug\app-debug.apk
& $adb shell pm clear com.proyectonami.app
& $adb shell am start -n com.proyectonami.app/.MainActivity
```

## 8. Código de verificación ficticio

El emulador no envía SMS reales. Después de pulsar **Enviar código de
verificación** en la app, consulta el código con:

```powershell
Invoke-RestMethod `
  "http://127.0.0.1:9099/emulator/v1/projects/demo-nami/verificationCodes" |
  ConvertTo-Json -Depth 10
```

Introduce en la app el `code` asociado al teléfono utilizado.

## 9. Comprobar los servicios

```powershell
Test-NetConnection 127.0.0.1 -Port 9099
Test-NetConnection 127.0.0.1 -Port 8080
Test-NetConnection 127.0.0.1 -Port 5001
Test-NetConnection 127.0.0.1 -Port 9199
```

Cada prueba debe mostrar:

```text
TcpTestSucceeded : True
```

## 10. Comandos opcionales para la demo

Grabar solo la pantalla del emulador:

```powershell
$sdk = "$env:LOCALAPPDATA\Android\Sdk"
$adb = "$sdk\platform-tools\adb.exe"

& $adb shell screenrecord `
  --time-limit 120 `
  --size 720x1600 `
  --bit-rate 4000000 `
  /sdcard/nami_demo.mp4

& $adb pull /sdcard/nami_demo.mp4 .\entrega_G06\nami_demo.mp4
Start-Process .\entrega_G06\nami_demo.mp4
```

`screenrecord` captura vídeo, pero no audio. Para audio del emulador se debe
usar OBS con captura de ventana del emulador y captura de audio de la aplicación
`qemu-system-x86_64`, sin micrófono ni audio global del escritorio.
