# Exercise the EXE wrapper and the installed application, retaining MSI diagnostics.
param([string]$InstallerPath)

$ErrorActionPreference = 'Stop'
$desktop = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$installer = if ($InstallerPath) { Get-Item $InstallerPath } else {
    Get-ChildItem (Join-Path $desktop 'build/jpackage/installer/*.exe') | Select-Object -First 1
}
if (!$installer) { throw 'Windows installer not found' }
$reports = Join-Path $desktop 'build/reports/installer'
New-Item -ItemType Directory -Force -Path $reports | Out-Null
$kind = $installer.Extension.TrimStart('.')
$log = Join-Path $reports "$kind-install.log"
# jpackage's EXE forwards these arguments to msiexec.
$arguments = @('/qn', '/norestart', '/L*v', "`"$log`"")
if ($kind -eq 'msi') {
    $install = Start-Process msiexec.exe -ArgumentList (@('/i', "`"$($installer.FullName)`"") + $arguments) -PassThru
} else {
    $install = Start-Process -FilePath $installer.FullName -ArgumentList $arguments -PassThru
}
if (!$install.WaitForExit(120000)) {
    Stop-Process -Id $install.Id -Force
    throw "Installer timed out; see $log"
}
if ($install.ExitCode -notin @(0, 3010)) { throw "Installer exit code $($install.ExitCode); see $log" }
# Query Windows Installer rather than assuming the location/view of ARP registry keys.
$match = [regex]::Match((Get-Content $log -Raw), 'Property\(S\): ProductCode = (\{[0-9A-Fa-f-]+\})')
if (!$match.Success) { throw 'MSI product code missing from installation log' }
$productCode = $match.Groups[1].Value
$msi = New-Object -ComObject WindowsInstaller.Installer
$state = $msi.GetType().InvokeMember('ProductState', [Reflection.BindingFlags]::GetProperty, $null, $msi, @($productCode))
if ($state -ne 5) { throw "Windows Installer product state is $state instead of installed" }
$location = $msi.GetType().InvokeMember('ProductInfo', [Reflection.BindingFlags]::GetProperty, $null, $msi, @($productCode, 'InstallLocation'))
try {
    $executable = Join-Path $location 'Algora.exe'
    if (!(Test-Path $executable)) { throw "Installed launcher missing: $executable" }
    $shell = New-Object -ComObject WScript.Shell
    $shortcuts = @(Get-ChildItem ([Environment]::GetFolderPath('Desktop')) -Filter '*Algora*.lnk') +
        @(Get-ChildItem ([Environment]::GetFolderPath('Programs')) -Recurse -Filter '*Algora*.lnk')
    if ($shortcuts.Count -lt 2) { throw 'Desktop and Start Menu shortcuts were not both created' }
    foreach ($file in $shortcuts) {
        $shortcut = $shell.CreateShortcut($file.FullName)
        if ($shortcut.TargetPath -ne $executable) { throw "Shortcut target mismatch: $($file.FullName)" }
    }
    $app = Start-Process -FilePath $executable -ArgumentList '--smoke-test' -PassThru
    if (!$app.WaitForExit(30000)) {
        Stop-Process -Id $app.Id -Force
        throw 'Installed application timed out'
    }
    if ($app.ExitCode -ne 0) { throw "Installed application exit code $($app.ExitCode)" }
    Write-Output 'ALGORA_WINDOWS_INSTALL_OK'
} finally {
    $uninstallLog = Join-Path $reports "$kind-uninstall.log"
    $remove = Start-Process msiexec.exe -ArgumentList @('/x', $productCode, '/qn', '/norestart', '/L*v', "`"$uninstallLog`"") -Wait -PassThru
    if ($remove.ExitCode -notin @(0, 3010)) { throw "Uninstall exit code $($remove.ExitCode); see $uninstallLog" }
    Write-Output 'ALGORA_WINDOWS_UNINSTALL_OK'
}
