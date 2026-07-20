# Publish a new LakePulse APK to GitHub Releases.
# Always uploads as "LakePulse.apk" so this NFC/share URL stays stable:
#   https://github.com/doorcountydrone/LakePulse/releases/latest/download/LakePulse.apk
#
# Usage (from repo root):
#   .\scripts\publish-apk.ps1
#   .\scripts\publish-apk.ps1 -VersionName "1.1"

param(
    [string]$VersionName = ""
)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $repoRoot

$gradleFile = Join-Path $repoRoot "app\build.gradle.kts"
$gradleText = Get-Content $gradleFile -Raw

if (-not ($gradleText -match 'versionCode\s*=\s*(\d+)')) {
    throw "Could not find versionCode in app/build.gradle.kts"
}
$oldCode = [int]$Matches[1]
$newCode = $oldCode + 1

if (-not ($gradleText -match 'versionName\s*=\s*"([^"]+)"')) {
    throw "Could not find versionName in app/build.gradle.kts"
}
$oldName = $Matches[1]
if ([string]::IsNullOrWhiteSpace($VersionName)) {
    # 1.0 -> 1.0.1, 1.0.1 -> 1.0.2, or append .1 if no patch
    if ($oldName -match '^(\d+)\.(\d+)$') {
        $VersionName = "$($Matches[1]).$($Matches[2]).1"
    } elseif ($oldName -match '^(\d+)\.(\d+)\.(\d+)$') {
        $patch = [int]$Matches[3] + 1
        $VersionName = "$($Matches[1]).$($Matches[2]).$patch"
    } else {
        $VersionName = "$oldName.$newCode"
    }
}

$tag = "v$VersionName"
Write-Host "Bumping versionCode $oldCode -> $newCode, versionName $oldName -> $VersionName (tag $tag)"

$gradleText = $gradleText -replace 'versionCode\s*=\s*\d+', "versionCode = $newCode"
$gradleText = $gradleText -replace 'versionName\s*=\s*"[^"]+"', "versionName = `"$VersionName`""
Set-Content -Path $gradleFile -Value $gradleText -NoNewline

Write-Host "Building debug APK..."
& .\gradlew.bat assembleDebug --quiet
if ($LASTEXITCODE -ne 0) { throw "Gradle build failed" }

$builtApk = Join-Path $repoRoot "app\build\outputs\apk\debug\app-debug.apk"
if (-not (Test-Path $builtApk)) { throw "APK not found at $builtApk" }

$releaseApk = Join-Path $repoRoot "LakePulse.apk"
Copy-Item $builtApk $releaseApk -Force

$env:Path = [System.Environment]::GetEnvironmentVariable("Path", "Machine") + ";" +
    [System.Environment]::GetEnvironmentVariable("Path", "User")

$notes = @"
LakePulse $VersionName (build $newCode)

**Install:** download ``LakePulse.apk`` below, allow install from this source if asked, then open the file.

Stable download link (for NFC tags):
https://github.com/doorcountydrone/LakePulse/releases/latest/download/LakePulse.apk
"@

Write-Host "Creating GitHub release $tag..."
gh release create $tag $releaseApk `
    --repo doorcountydrone/LakePulse `
    --title "LakePulse $VersionName" `
    --notes $notes

Remove-Item $releaseApk -Force -ErrorAction SilentlyContinue

$env:GIT_AUTHOR_NAME = "doorcountydrone"
$env:GIT_AUTHOR_EMAIL = "155830200+doorcountydrone@users.noreply.github.com"
$env:GIT_COMMITTER_NAME = $env:GIT_AUTHOR_NAME
$env:GIT_COMMITTER_EMAIL = $env:GIT_AUTHOR_EMAIL

git add app/build.gradle.kts
git commit -m "Bump version to $VersionName ($newCode) for APK release."
git -c http.sslVerify=false push origin main

Write-Host ""
Write-Host "Done."
Write-Host "NFC / share URL (never changes):"
Write-Host "https://github.com/doorcountydrone/LakePulse/releases/latest/download/LakePulse.apk"
Write-Host "Release page: https://github.com/doorcountydrone/LakePulse/releases/tag/$tag"
