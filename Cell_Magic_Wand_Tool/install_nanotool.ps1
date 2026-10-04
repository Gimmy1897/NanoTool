$ErrorActionPreference = "Stop"

$packageDir = [IO.Path]::GetFullPath((Split-Path -Parent $MyInvocation.MyCommand.Path))
$jarFiles = @(
    "NanoTool_Launcher_Tool.jar"
)
$macroFile = Join-Path $packageDir "RunAtStartup.ijm"

function Test-ImageJDirectory([string]$path) {
    return (Test-Path (Join-Path $path "ij.jar")) -or
        (Test-Path (Join-Path $path "ImageJ.exe")) -or
        (Test-Path (Join-Path $path "ImageJ-win64.exe"))
}

function Select-ImageJFolder {
    Add-Type -AssemblyName System.Windows.Forms
    $dialog = New-Object System.Windows.Forms.FolderBrowserDialog
    $dialog.Description = "Select the ImageJ or Fiji installation folder"
    $dialog.ShowNewFolderButton = $false
    $result = $dialog.ShowDialog()
    if ($result -eq [System.Windows.Forms.DialogResult]::OK) {
        return $dialog.SelectedPath
    }
    return $null
}

$candidateRoots = @(
    (Join-Path $env:ProgramFiles "ImageJ"),
    (Join-Path $env:ProgramFiles "Fiji.app"),
    (Join-Path $env:LOCALAPPDATA "Fiji.app"),
    (Join-Path $env:USERPROFILE "Fiji.app"),
    (Join-Path $env:USERPROFILE "Desktop\Fiji.app"),
    (Join-Path $env:USERPROFILE "Downloads\Fiji.app"),
    (Join-Path $env:USERPROFILE "Documents\Fiji.app"),
    (Join-Path $env:USERPROFILE "OneDrive\Fiji.app")
)
if (-not [string]::IsNullOrWhiteSpace(${env:ProgramFiles(x86)})) {
    $candidateRoots += Join-Path ${env:ProgramFiles(x86)} "ImageJ"
    $candidateRoots += Join-Path ${env:ProgramFiles(x86)} "Fiji.app"
}

$searchRoots = @(
    (Join-Path $env:USERPROFILE "Desktop"),
    (Join-Path $env:USERPROFILE "Downloads"),
    (Join-Path $env:USERPROFILE "Documents"),
    (Join-Path $env:USERPROFILE "OneDrive")
) | Where-Object { $_ -and (Test-Path $_) }

$oneDriveRoots = Get-ChildItem -Path (Join-Path $env:USERPROFILE "OneDrive*") `
    -Directory -ErrorAction SilentlyContinue | Select-Object -ExpandProperty FullName
$searchRoots += $oneDriveRoots

foreach ($root in $searchRoots) {
    $candidateRoots += Get-ChildItem -LiteralPath $root -Directory -Filter "Fiji.app" -Depth 3 -ErrorAction SilentlyContinue |
        Select-Object -ExpandProperty FullName
}

$candidates = @($candidateRoots | Where-Object {
    $_ -and (Test-ImageJDirectory $_)
} | Select-Object -Unique)
$candidates = @($candidates)

if ($candidates.Count -eq 1) {
    Write-Host "The following ImageJ/Fiji installation was found:"
} elseif ($candidates.Count -gt 1) {
    Write-Host "The following ImageJ/Fiji installations were found:"
} else {
    Write-Host "No ImageJ/Fiji installation was found automatically."
}

if ($candidates.Count -gt 0) {
    Write-Host "[0] Browse for an ImageJ/Fiji folder"
    for ($i = 0; $i -lt $candidates.Count; $i++) {
        Write-Host "[$($i + 1)] $($candidates[$i])"
    }
    do {
        $selection = Read-Host "Enter the number of the installation"
        $index = 0
        $validSelection = [int]::TryParse($selection, [ref]$index) -and
            $index -ge 0 -and $index -le $candidates.Count
        if ($validSelection -and $index -eq 0) {
            $imageJDir = Select-ImageJFolder
            $validSelection = $null -ne $imageJDir
            if ($validSelection -and -not (Test-ImageJDirectory $imageJDir)) {
                Write-Host "The selected folder does not appear to contain ImageJ or Fiji."
                $imageJDir = $null
                $validSelection = $false
            }
        }
        if (-not $validSelection) {
            Write-Host "Invalid selection. Enter 0 or one of the displayed numbers."
        }
    } while (-not $validSelection)
    if (-not $imageJDir) {
        $imageJDir = $candidates[$index - 1]
    }
    Write-Host "Selected installation: $imageJDir"
}

if (-not $imageJDir) {
    Write-Host "[0] Browse for an ImageJ/Fiji folder"
    do {
        $selection = Read-Host "Enter 0 to open the folder selector"
        $validSelection = $selection.Trim() -eq "0"
        if ($validSelection) {
            $imageJDir = Select-ImageJFolder
            if ($imageJDir -and -not (Test-ImageJDirectory $imageJDir)) {
                Write-Host "The selected folder does not appear to contain ImageJ or Fiji."
                $imageJDir = $null
                $validSelection = $false
            }
        } else {
            Write-Host "Invalid selection. Enter 0."
        }
    } while (-not $validSelection)
    if (-not $imageJDir) {
        throw "No ImageJ/Fiji folder was selected."
    }
}

if (-not $imageJDir) {
    while (-not $imageJDir -or -not (Test-ImageJDirectory $imageJDir)) {
        $inputPath = Read-Host "Enter the full ImageJ/Fiji folder path (for example C:\ImageJ)"
        if ([string]::IsNullOrWhiteSpace($inputPath)) {
            Write-Host "The path cannot be empty."
            $imageJDir = $null
            continue
        }
        $inputPath = $inputPath.Trim().Trim('"')
        if (-not [IO.Path]::IsPathRooted($inputPath) -or -not (Test-Path $inputPath -PathType Container)) {
            Write-Host "Folder not found. Please enter an absolute path."
            $imageJDir = $null
        } elseif (-not (Test-ImageJDirectory $inputPath)) {
            Write-Host "The folder does not appear to contain ImageJ or Fiji."
            $imageJDir = $null
        } else {
            $imageJDir = [IO.Path]::GetFullPath((Resolve-Path $inputPath).Path)
        }
    }
}

$imageJDir = [IO.Path]::GetFullPath((Resolve-Path $imageJDir).Path)
$pluginsDir = Join-Path $imageJDir "plugins\Tools"
$macrosDir = Join-Path $imageJDir "macros"
if ($imageJDir.TrimEnd('\') -eq $packageDir.TrimEnd('\')) {
    throw "The ImageJ/Fiji folder cannot be the installer package folder."
}
New-Item -ItemType Directory -Force -Path $pluginsDir, $macrosDir | Out-Null

foreach ($jarFile in $jarFiles) {
    $source = Join-Path $packageDir $jarFile
    if (-not (Test-Path $source)) {
        throw "Missing file: $source. Run build.bat first."
    }
    Copy-Item $source (Join-Path $pluginsDir $jarFile) -Force
}

if (-not (Test-Path $macroFile)) {
    throw "Missing macro: $macroFile."
}
Copy-Item $macroFile (Join-Path $macrosDir "RunAtStartup.ijm") -Force

Write-Host ""
Write-Host "Installation completed successfully."
Write-Host "Plugin: $pluginsDir"
Write-Host "Macro:  $(Join-Path $macrosDir 'RunAtStartup.ijm')"
Write-Host "Restart ImageJ/Fiji to load NanoTool."
Read-Host "Press ENTER to exit"
