@echo off
REM Build NanoTool and create JAR files for the ImageJ/Fiji plugins\Tools folder.
REM Run by double-clicking or from a terminal in the project directory.

cd /d "%~dp0"

if not exist src\plugins-nanotool.config (
    echo ERROR: src\plugins-nanotool.config was not found.
    echo Check the file name in File Explorer ^(with extensions visible^).
    pause
    exit /b 1
)

if exist bin rmdir /s /q bin
mkdir bin

echo Compiling...
javac -source 8 -target 8 -d bin -cp "lib\ij-1.48v.jar" src\*.java src\cellMagicWand\*.java
if errorlevel 1 (
    echo Compilation failed. See the errors above.
    pause
    exit /b 1
)

echo Packaging JAR files for plugins\Tools\...
REM Each JAR has its own configuration to avoid duplicate ImageJ commands.
copy /y src\plugins-cell.config bin\plugins.config
if not exist bin\plugins.config (
    echo ERROR: failed to copy plugins.config.
    pause
    exit /b 1
)

cd bin
if not exist macros mkdir macros
copy /y ..\src\macros\RunAtStartup.ijm macros\RunAtStartup.ijm
jar cf ..\Cell_Magic_Wand_Tool.jar plugins.config *.class cellMagicWand\*.class
copy /y ..\src\plugins-nanotool.config plugins.config
jar cf ..\NanoTool_Launcher_Tool.jar plugins.config *.class cellMagicWand\*.class
copy /y ..\src\plugins-nanotool.config plugins.config
jar cf ..\NanoTool.jar plugins.config *.class cellMagicWand\*.class
cd ..

if not exist Cell_Magic_Wand_Tool.jar (
    echo ERROR: Cell_Magic_Wand_Tool.jar was not created.
    pause
    exit /b 1
)

echo Done! NanoTool_Launcher_Tool.jar contains NanoTool and Cell Magic Wand.
echo To install NanoTool automatically, run install_nanotool.bat.
copy /y src\macros\RunAtStartup.ijm RunAtStartup.ijm
if errorlevel 1 (
    echo WARNING: unable to copy RunAtStartup.ijm next to the installer.
)
pause