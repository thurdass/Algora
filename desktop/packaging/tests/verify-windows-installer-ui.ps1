# Inspect native MSI dialogs, including windows not exposed through the UI Automation root.
param([Parameter(Mandatory = $true)][string]$InstallerPath)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.Windows.Forms
Add-Type -TypeDefinition @'
using System;
using System.Collections.Generic;
using System.Runtime.InteropServices;
using System.Text;
public class InstallerWindow {
    public IntPtr Handle;
    public string Name, Class;
    public int ProcessId, Width, Height;
    public bool Visible;
}
public static class InstallerNative {
    private delegate bool Callback(IntPtr hwnd, IntPtr arg);
    [StructLayout(LayoutKind.Sequential)] private struct Rect { public int Left, Top, Right, Bottom; }
    [DllImport("user32.dll")] private static extern bool EnumWindows(Callback callback, IntPtr arg);
    [DllImport("user32.dll")] private static extern bool EnumChildWindows(IntPtr parent, Callback callback, IntPtr arg);
    [DllImport("user32.dll", CharSet=CharSet.Unicode)] private static extern int GetWindowText(IntPtr hwnd, StringBuilder text, int size);
    [DllImport("user32.dll", CharSet=CharSet.Unicode)] private static extern int GetClassName(IntPtr hwnd, StringBuilder text, int size);
    [DllImport("user32.dll")] private static extern bool IsWindowVisible(IntPtr hwnd);
    [DllImport("user32.dll")] private static extern bool GetWindowRect(IntPtr hwnd, out Rect rect);
    [DllImport("user32.dll")] private static extern uint GetWindowThreadProcessId(IntPtr hwnd, out uint pid);
    [DllImport("user32.dll")] private static extern bool PostMessage(IntPtr hwnd, uint message, IntPtr wparam, IntPtr lparam);
    private static InstallerWindow Read(IntPtr hwnd) {
        var name = new StringBuilder(2048); var cls = new StringBuilder(256);
        GetWindowText(hwnd, name, name.Capacity); GetClassName(hwnd, cls, cls.Capacity);
        uint pid; GetWindowThreadProcessId(hwnd, out pid); Rect rect; GetWindowRect(hwnd, out rect);
        return new InstallerWindow { Handle=hwnd, Name=name.ToString(), Class=cls.ToString(),
            ProcessId=(int)pid, Visible=IsWindowVisible(hwnd), Width=rect.Right-rect.Left, Height=rect.Bottom-rect.Top };
    }
    public static InstallerWindow[] Windows() {
        var windows = new List<InstallerWindow>();
        EnumWindows((hwnd, arg) => { windows.Add(Read(hwnd)); return true; }, IntPtr.Zero);
        return windows.ToArray();
    }
    public static InstallerWindow[] Children(IntPtr parent) {
        var windows = new List<InstallerWindow>();
        EnumChildWindows(parent, (hwnd, arg) => { windows.Add(Read(hwnd)); return true; }, IntPtr.Zero);
        return windows.ToArray();
    }
    public static void Close(IntPtr hwnd) { PostMessage(hwnd, 0x0010, IntPtr.Zero, IntPtr.Zero); }
    public static void Click(IntPtr hwnd) { PostMessage(hwnd, 0x00F5, IntPtr.Zero, IntPtr.Zero); }
}
'@
$installer = Get-Item $InstallerPath
$kind = $installer.Extension.TrimStart('.')
$reports = Join-Path (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path 'build/reports/installer'
New-Item -ItemType Directory -Force -Path $reports | Out-Null
$log = Join-Path $reports "$kind-ui-install.log"
function Get-InstallerWindows {
    foreach ($window in [InstallerNative]::Windows()) {
        $owner = Get-Process -Id $window.ProcessId -ErrorAction SilentlyContinue
        if ($owner.ProcessName -eq 'msiexec' -and $window.Visible) { $window }
    }
}
if ($kind -eq 'msi') {
    $process = Start-Process msiexec.exe -ArgumentList @('/i', "`"$($installer.FullName)`"", '/L*v', "`"$log`"") -PassThru
} else {
    $process = Start-Process $installer.FullName -ArgumentList @('/L*v', "`"$log`"") -PassThru
}
$found = $false
try {
    $deadline = [DateTime]::UtcNow.AddSeconds(45)
    while ([DateTime]::UtcNow -lt $deadline) {
        foreach ($window in @(Get-InstallerWindows)) {
            $children = [InstallerNative]::Children($window.Handle)
            $names = @($children | Where-Object { $_.Class -eq 'Button' -and $_.Visible } | ForEach-Object Name)
            $labels = ($window.Name + ' ' + (($children | ForEach-Object Name) -join ' '))
            if ($labels -match 'Algora' -and $names -match 'Next|Install|Repair|Finish' -and $window.Width -gt 0 -and $window.Height -gt 0) {
                $labels | Set-Content (Join-Path $reports "$kind-ui.txt")
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
        if ($found) { break }
        if ($process.HasExited) { throw "Installer exited before showing its wizard: $($process.ExitCode)" }
        Start-Sleep -Milliseconds 300
    }
    if (!$found) {
        [InstallerNative]::Windows() | ConvertTo-Json -Depth 3 | Set-Content (Join-Path $reports "$kind-windows.json")
        throw "Installer did not show a visible wizard within 45 seconds ($kind)"
    }
    Write-Output "ALGORA_WINDOWS_INSTALLER_UI_OK: $kind"
} finally {
    foreach ($window in @(Get-InstallerWindows)) { [InstallerNative]::Close($window.Handle) }
    $deadline = [DateTime]::UtcNow.AddSeconds(15)
    while (!$process.HasExited -and [DateTime]::UtcNow -lt $deadline) {
        foreach ($window in @(Get-InstallerWindows)) {
            foreach ($button in [InstallerNative]::Children($window.Handle)) {
                if ($button.Class -eq 'Button' -and $button.Visible -and $button.Name -match '^(&?Yes|&?Finish)$') {
                    [InstallerNative]::Click($button.Handle)
                }
            }
        }
        Start-Sleep -Milliseconds 300
    }
    if (!$process.HasExited) {
        taskkill.exe /PID $process.Id /T /F | Out-Null
        if ($found) { throw "Installer wizard did not close after cancellation ($kind)" }
    }
}
