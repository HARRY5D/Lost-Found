@echo off
echo Getting SHA-1 fingerprint for debug keystore...
echo.

REM Try different possible Java locations
set JAVA_LOCATIONS[0]="C:\Program Files\Java\jdk*\bin\keytool.exe"
set JAVA_LOCATIONS[1]="C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe"
set JAVA_LOCATIONS[2]="C:\Program Files\Eclipse Adoptium\jdk*\bin\keytool.exe"
set JAVA_LOCATIONS[3]="%JAVA_HOME%\bin\keytool.exe"

REM Try to find keytool
for %%i in ("C:\Program Files\Java\jdk*\bin\keytool.exe") do (
    if exist "%%i" (
        echo Found keytool at: %%i
        "%%i" -list -v -keystore debug.keystore -alias androiddebugkey -storepass android -keypass android
        goto :end
    )
)

for %%i in ("C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe") do (
    if exist "%%i" (
        echo Found keytool at: %%i
        "%%i" -list -v -keystore debug.keystore -alias androiddebugkey -storepass android -keypass android
        goto :end
    )
)

echo.
echo keytool not found. Please run this command manually:
echo keytool -list -v -keystore debug.keystore -alias androiddebugkey -storepass android -keypass android
echo.
echo Look for the SHA1 fingerprint in the output and add it to Firebase Console.

:end
pause
