# Run with Windows PowerShell for the built-in .NET Framework UI Automation assemblies.
param([Parameter(Mandatory = $true)][string]$InstallerPath)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName UIAutomationClient
Add-Type -AssemblyName UIAutomationTypes
Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.Windows.Forms
$installer = Get-Item $InstallerPath
$kind = $installer.Extension.TrimStart('.')
$reports = Join-Path (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path 'build/reports/installer'
New-Item -ItemType Directory -Force -Path $reports | Out-Null
$log = Join-Path $reports "$kind-ui-install.log"
$root = [System.Windows.Automation.AutomationElement]::RootElement
$buttonCondition = New-Object System.Windows.Automation.PropertyCondition(
    [System.Windows.Automation.AutomationElement]::ControlTypeProperty,
    [System.Windows.Automation.ControlType]::Button)
function Get-InstallerWindows {
    $windows = $root.FindAll([System.Windows.Automation.TreeScope]::Children,
        [System.Windows.Automation.Condition]::TrueCondition)
    foreach ($window in $windows) {
        if ($window.Current.Name -like '*Algora*' -and !$window.Current.IsOffscreen) { $window }
    }
}
if ($kind -eq 'msi') {
    $process = Start-Process msiexec.exe -ArgumentList @('/i', "`"$($installer.FullName)`"", '/L*v', "`"$log`"") -PassThru
} else {
    $process = Start-Process $installer.FullName -ArgumentList @('/L*v', "`"$log`"") -PassThru
}
try {
    $deadline = [DateTime]::UtcNow.AddSeconds(45)
    $found = $false
    while ([DateTime]::UtcNow -lt $deadline) {
        foreach ($window in @(Get-InstallerWindows)) {
            $buttons = $window.FindAll([System.Windows.Automation.TreeScope]::Descendants, $buttonCondition)
            $names = @($buttons | ForEach-Object { $_.Current.Name })
            if ($names -match 'Next|Install|Repair|Finish') {
                $rectangle = $window.Current.BoundingRectangle
                if ($rectangle.Width -gt 0 -and $rectangle.Height -gt 0) {
                    "$($window.Current.Name): $($names -join ', ')" | Set-Content (Join-Path $reports "$kind-ui.txt")
                    $screen = [System.Windows.Forms.Screen]::PrimaryScreen.Bounds
                    $bitmap = New-Object System.Drawing.Bitmap($screen.Width, $screen.Height)
                    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
                    try {
                        $graphics.CopyFromScreen($screen.Location, [System.Drawing.Point]::Empty, $screen.Size)
                        $bitmap.Save((Join-Path $reports "$kind-ui.png"), [System.Drawing.Imaging.ImageFormat]::Png)
                    } finally { $graphics.Dispose(); $bitmap.Dispose() }
                    $found = $true
                    break
                }
            }
        }
        if ($found) { break }
        if ($process.HasExited) { throw "Installer exited before showing its wizard: $($process.ExitCode)" }
        Start-Sleep -Milliseconds 300
    }
    if (!$found) { throw "Installer did not show a visible wizard within 45 seconds ($kind)" }
    Write-Output "ALGORA_WINDOWS_INSTALLER_UI_OK: $kind"
} finally {
    # Cancel the wizard without installing; the silent test exercises installation separately.
    foreach ($window in @(Get-InstallerWindows)) {
        $pattern = $null
        if ($window.TryGetCurrentPattern([System.Windows.Automation.WindowPattern]::Pattern, [ref]$pattern)) {
            $pattern.Close()
        }
    }
    $deadline = [DateTime]::UtcNow.AddSeconds(15)
    while (!$process.HasExited -and [DateTime]::UtcNow -lt $deadline) {
        foreach ($window in @(Get-InstallerWindows)) {
            $buttons = $window.FindAll([System.Windows.Automation.TreeScope]::Descendants, $buttonCondition)
            foreach ($button in $buttons) {
                if ($button.Current.Name -match '^(&?Yes|&?Finish)$') {
                    $invoke = $button.GetCurrentPattern([System.Windows.Automation.InvokePattern]::Pattern)
                    $invoke.Invoke()
                }
            }
        }
        Start-Sleep -Milliseconds 300
    }
    if (!$process.HasExited) {
        taskkill.exe /PID $process.Id /T /F | Out-Null
        throw "Installer wizard did not close after cancellation ($kind)"
    }
}
