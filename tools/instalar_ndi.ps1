param(
    [Parameter(Mandatory = $true)]
    [string]$SdkDir
)

$ErrorActionPreference = "Stop"
$sdkRoot = (Resolve-Path $SdkDir).Path
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$includeTarget = Join-Path $projectRoot "app\src\main\cpp\include"
$jniTarget = Join-Path $projectRoot "app\src\main\jniLibs"

$mainHeader = Get-ChildItem -Path $sdkRoot -Recurse -File -Filter "Processing.NDI.Lib.h" | Select-Object -First 1
if (-not $mainHeader) {
    throw "Processing.NDI.Lib.h nao foi encontrado em: $sdkRoot"
}

New-Item -ItemType Directory -Force -Path $includeTarget | Out-Null
Get-ChildItem -Path $mainHeader.Directory.FullName -File -Filter "*.h" |
    Copy-Item -Destination $includeTarget -Force

$libraries = Get-ChildItem -Path $sdkRoot -Recurse -File -Filter "libndi.so"
$copiedArm64 = $false
$copiedArm32 = $false

foreach ($library in $libraries) {
    $path = $library.FullName.ToLowerInvariant()
    if ($path -match "arm64-v8a|aarch64|arm64") {
        $destination = Join-Path $jniTarget "arm64-v8a"
        New-Item -ItemType Directory -Force -Path $destination | Out-Null
        Copy-Item $library.FullName (Join-Path $destination "libndi.so") -Force
        $copiedArm64 = $true
    }
    elseif ($path -match "armeabi-v7a|armv7|armhf|arm32") {
        $destination = Join-Path $jniTarget "armeabi-v7a"
        New-Item -ItemType Directory -Force -Path $destination | Out-Null
        Copy-Item $library.FullName (Join-Path $destination "libndi.so") -Force
        $copiedArm32 = $true
    }
}

if (-not $copiedArm64) {
    throw "A biblioteca libndi.so ARM64 nao foi encontrada no SDK informado."
}

Remove-Item (Join-Path $includeTarget "COLOQUE_OS_HEADERS_NDI_AQUI.txt") -Force -ErrorAction SilentlyContinue
Remove-Item (Join-Path $jniTarget "arm64-v8a\COLOQUE_LIBNDI_SO_AQUI.txt") -Force -ErrorAction SilentlyContinue
if ($copiedArm32) {
    Remove-Item (Join-Path $jniTarget "armeabi-v7a\COLOQUE_LIBNDI_SO_AQUI.txt") -Force -ErrorAction SilentlyContinue
}

Write-Host "Headers NDI: OK" -ForegroundColor Green
Write-Host "ARM64: OK" -ForegroundColor Green
if ($copiedArm32) {
    Write-Host "ARM32: OK" -ForegroundColor Green
}
else {
    Write-Warning "ARM32 nao foi encontrado. Remova armeabi-v7a de abiFilters se o SDK nao oferecer essa arquitetura."
}

Remove-Item (Join-Path $projectRoot "app\.cxx") -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item (Join-Path $projectRoot "app\build") -Recurse -Force -ErrorAction SilentlyContinue
(Get-Item (Join-Path $projectRoot "app\src\main\cpp\CMakeLists.txt")).LastWriteTime = Get-Date

Write-Host "Agora sincronize e compile o projeto no Android Studio." -ForegroundColor Cyan
