# Package the single form document, its images, and the installable first-release JAR.
$ErrorActionPreference = 'Stop'
$workspace = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
if ((Get-Content -Raw -LiteralPath (Join-Path $workspace 'gradle.properties')) -notmatch '(?m)^mod_version=1\.0\.0\s*$') {
    throw 'This first-release package is fixed at version 1.0.0.'
}
$kit = Join-Path $workspace 'release\curseforge\v1.0.0'
$jarName = 'All-Chinese-Instructions-1.20.1-1.0.0.jar'
Copy-Item -LiteralPath (Join-Path $workspace ('build\libs\' + $jarName)) -Destination (Join-Path $kit $jarName) -Force

Add-Type -AssemblyName System.IO.Compression.FileSystem
$jar = [System.IO.Compression.ZipFile]::OpenRead((Join-Path $kit $jarName))
try {
    $reader = [System.IO.StreamReader]::new($jar.GetEntry('META-INF/mods.toml').Open())
    try { $metadata = $reader.ReadToEnd() } finally { $reader.Dispose() }
    if ($metadata -notmatch 'version="1.0.0"') { throw 'JAR internal version is not 1.0.0.' }
    if ($metadata -notmatch 'license="MIT"' -or $null -eq $jar.GetEntry('LICENSE')) {
        throw 'The release JAR must declare MIT and include its license text.'
    }
} finally { $jar.Dispose() }

$members = @(
    'CurseForge项目填写.md',
    $jarName,
    'assets/icon-400.png',
    'assets/gallery-cover.png',
    'assets/commands-guide.png',
    'assets/selectors-guide.png'
)
foreach ($member in $members) {
    if (-not (Test-Path -LiteralPath (Join-Path $kit $member) -PathType Leaf)) {
        throw ('Missing release file: ' + $member)
    }
}
$actual = @(Get-ChildItem -LiteralPath $kit -Recurse -File | ForEach-Object {
    [System.IO.Path]::GetRelativePath($kit, $_.FullName).Replace('\', '/')
})
if (@(Compare-Object $members $actual).Count -ne 0) {
    throw 'Keep this package limited to one form document, four images and one installable JAR.'
}
$zipPath = Join-Path $workspace 'release\curseforge\All-Chinese-Instructions-1.0.0-CurseForge-Kit.zip'
$stream = [System.IO.File]::Open($zipPath, [System.IO.FileMode]::Create)
$archive = [System.IO.Compression.ZipArchive]::new($stream, [System.IO.Compression.ZipArchiveMode]::Create)
try {
    foreach ($member in $members) {
        [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($archive, (Join-Path $kit $member), $member, [System.IO.Compression.CompressionLevel]::Optimal) | Out-Null
    }
} finally {
    $archive.Dispose()
    $stream.Dispose()
}
[pscustomobject]@{Package=$zipPath;Files=$members.Count;TextDocuments=1;Images=4;InstallableJars=1;Bytes=(Get-Item -LiteralPath $zipPath).Length}
