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
jar cf ..\Cell_Magic_Wand_Tool.jar plugins.config *.class cellMagicWand\*.class
copy /y ..\src\plugins-nanotool.config plugins.config
jar cf ..\NanoTool_Launcher_Tool.jar plugins.config *.class cellMagicWand\*.class
jar cf ..\NanoTool.jar plugins.config *.class cellMagicWand\*.class
cd ..

if not exist Cell_Magic_Wand_Tool.jar (
    echo ERROR: Cell_Magic_Wand_Tool.jar was not created.
    pause
    exit /b 1
)

echo Done! Copy Cell_Magic_Wand_Tool.jar and NanoTool_Launcher_Tool.jar to your Fiji installation's plugins\Tools folder.
pause