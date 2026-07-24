$ErrorActionPreference = 'Stop'

$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
$WorkspaceParent = Split-Path -Parent $Root

# Final release destination:
# C:\Users\hunga\Documents\Coder\Minecraft\myMod\mods
$OutputDirectory = Join-Path $WorkspaceParent 'mods'

# NeoForge currently writes its release JAR into this staging directory.
$NeoForgeStagingDirectory = Join-Path $Root 'mods'

$OriginalJavaHome = $env:JAVA_HOME
$OriginalPath = $env:Path

function Use-JavaHome([string]$VariableName, [string]$Label) {
    $candidate = [Environment]::GetEnvironmentVariable($VariableName)

    if (-not [string]::IsNullOrWhiteSpace($candidate)) {
        if (-not (Test-Path (Join-Path $candidate 'bin\java.exe'))) {
            throw "$VariableName points to an invalid JDK directory: $candidate"
        }

        $env:JAVA_HOME = $candidate
        $env:Path = "$(Join-Path $candidate 'bin');$OriginalPath"
        Write-Host "Using $Label from ${VariableName}: $candidate"
        return
    }

    if (-not [string]::IsNullOrWhiteSpace($OriginalJavaHome)) {
        $env:JAVA_HOME = $OriginalJavaHome
        $env:Path = "$(Join-Path $OriginalJavaHome 'bin');$OriginalPath"
        Write-Host "${VariableName} is not set; using current JAVA_HOME for ${Label}: $OriginalJavaHome" -ForegroundColor Yellow
        return
    }

    $env:JAVA_HOME = $null
    $env:Path = $OriginalPath
    Write-Host "${VariableName} and JAVA_HOME are not set; Gradle will use java.exe from PATH for ${Label}." -ForegroundColor Yellow
}

function Invoke-Build(
    [string]$DisplayName,
    [string]$Folder,
    [string]$JavaVariable,
    [string]$JavaLabel
) {
    Write-Host ""
    Write-Host "Building $DisplayName" -ForegroundColor Cyan

    Use-JavaHome $JavaVariable $JavaLabel

    Push-Location (Join-Path $Root $Folder)
    try {
        & .\gradlew.bat clean build --stacktrace
        if ($LASTEXITCODE -ne 0) {
            throw "$DisplayName build failed with exit code $LASTEXITCODE"
        }
    }
    finally {
        Pop-Location
    }
}

function Get-LatestReleaseJar(
    [string]$Folder,
    [string]$NamePattern,
    [string]$DisplayName
) {
    if (-not (Test-Path $Folder)) {
        throw "$DisplayName output directory does not exist: $Folder"
    }

    $jar = Get-ChildItem -Path $Folder -Filter $NamePattern -File |
        Where-Object {
            $_.Name -notmatch '(-sources|-dev|-javadoc|-slim|-plain)\.jar$'
        } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1

    if ($null -eq $jar) {
        throw "Could not find the release JAR for $DisplayName in: $Folder"
    }

    return $jar
}

function Copy-ReleaseJar(
    [System.IO.FileInfo]$Jar,
    [string]$DisplayName
) {
    $destination = Join-Path $OutputDirectory $Jar.Name
    Copy-Item -Path $Jar.FullName -Destination $destination -Force
    Write-Host "Copied $DisplayName -> $destination" -ForegroundColor Green
}

New-Item -ItemType Directory -Path $OutputDirectory -Force | Out-Null

try {
    Invoke-Build 'NeoForge 1.21.1' 'neoforge-1.21.1' 'JAVA21_HOME' 'JDK 21'
    Invoke-Build 'Fabric 1.21.1' 'fabric-1.21.1' 'JAVA21_HOME' 'JDK 21'
    Invoke-Build 'Forge 1.20.1' 'forge-1.20.1' 'JAVA17_HOME' 'JDK 17'

    $neoForgeJar = Get-LatestReleaseJar `
        $NeoForgeStagingDirectory `
        'camera_lockon-1.21.1-neoforge-*.jar' `
        'NeoForge 1.21.1'

    $fabricJar = Get-LatestReleaseJar `
        (Join-Path $Root 'fabric-1.21.1\build\libs') `
        'camera-lock-on-fabric-1.21.1-*.jar' `
        'Fabric 1.21.1'

    $forgeJar = Get-LatestReleaseJar `
        (Join-Path $Root 'forge-1.20.1\build\libs') `
        'camera-lock-on-forge-1.20.1-*.jar' `
        'Forge 1.20.1'

    Copy-ReleaseJar $neoForgeJar 'NeoForge 1.21.1'
    Copy-ReleaseJar $fabricJar 'Fabric 1.21.1'
    Copy-ReleaseJar $forgeJar 'Forge 1.20.1'

    Write-Host ""
    Write-Host 'All three builds completed successfully.' -ForegroundColor Green
    Write-Host "Release JAR directory: $OutputDirectory" -ForegroundColor Cyan
}
finally {
    $env:JAVA_HOME = $OriginalJavaHome
    $env:Path = $OriginalPath
}
