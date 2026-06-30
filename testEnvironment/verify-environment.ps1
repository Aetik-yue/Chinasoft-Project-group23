$ErrorActionPreference = 'Stop'

function Assert-Command {
    param([Parameter(Mandatory = $true)][string]$Name)
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "Required command '$Name' was not found. Open a new terminal and try again."
    }
    Write-Host "[OK] $Name"
}

$javaHome = [Environment]::GetEnvironmentVariable('JAVA_HOME', 'User')
$mavenHome = [Environment]::GetEnvironmentVariable('MAVEN_HOME', 'User')
if ($javaHome) { $env:JAVA_HOME = $javaHome }
if ($mavenHome) { $env:MAVEN_HOME = $mavenHome }
if ($javaHome -and $mavenHome) {
    $env:Path = "$javaHome\bin;$mavenHome\bin;$env:Path"
}

Write-Host 'Checking local toolchain...'
foreach ($command in @('java', 'javac', 'mvn', 'git', 'docker', 'wsl')) {
    Assert-Command $command
}

$previousErrorPreference = $ErrorActionPreference
$ErrorActionPreference = 'Continue'
$javaVersion = (& java -version 2>&1 | Out-String)
$ErrorActionPreference = $previousErrorPreference
if ($javaVersion -notmatch 'version "21\.') {
    throw "Java 21 is required. Detected: $javaVersion"
}

docker info | Out-Null
Write-Host '[OK] Docker engine'

& wsl -d Ubuntu-22.04 -u root -- true
if ($LASTEXITCODE -ne 0) { throw 'Ubuntu-22.04 WSL2 distribution is unavailable.' }
Write-Host '[OK] WSL2 Ubuntu-22.04'

$expectedContainers = @('smart-smoke-redis', 'maxkb', 'dataease', 'mysql-de')
foreach ($container in $expectedContainers) {
    $status = docker inspect --format '{{.State.Status}}' $container 2>$null
    if ($LASTEXITCODE -ne 0 -or $status -ne 'running') {
        throw "Container '$container' is not running."
    }
    Write-Host "[OK] container $container"
}

$previousPassword = $env:MYSQL_PASSWORD
if (-not $env:MYSQL_PASSWORD) {
    $securePassword = Read-Host 'Enter the dream26 MySQL password' -AsSecureString
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
    try {
        $env:MYSQL_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
    }
}

try {
    Push-Location $PSScriptRoot
    try {
        Write-Host 'Running Spring Boot integration checks...'
        & mvn test
        if ($LASTEXITCODE -ne 0) { throw "Maven tests failed with exit code $LASTEXITCODE." }
    } finally {
        Pop-Location
    }
} finally {
    if ($null -eq $previousPassword) {
        Remove-Item Env:MYSQL_PASSWORD -ErrorAction SilentlyContinue
    } else {
        $env:MYSQL_PASSWORD = $previousPassword
    }
}

Write-Host 'All environment checks passed.' -ForegroundColor Green
