
<#
.SYNOPSIS
    Phase A: ADB Connection Helper
    Establishes wireless ADB connection to Xiaomi 13 Chinese ROM
#>

param(
    [string]$DeviceIP = "",
    [int]$Port = 5555
)

$ADB = "$PSScriptRoot\..\platform-tools\adb.exe"

function Test-ADB {
    if (-not (Test-Path $ADB)) {
        Write-Error "ADB not found at $ADB. Run setup first."
        exit 1
    }
    $ver = & $ADB version 2>&1 | Select-Object -First 1
    Write-Host "[OK] $ver" -ForegroundColor Green
}

function Get-ConnectedDevices {
    $devices = & $ADB devices 2>&1
    Write-Host "`n=== Connected Devices ===" -ForegroundColor Cyan
    $devices | ForEach-Object { Write-Host $_ }
    return $devices
}

function Connect-Wireless {
    param([string]$IP, [int]$P)
    Write-Host "`nConnecting to $IP`:$P ..." -ForegroundColor Yellow
    $result = & $ADB connect "$IP`:$P" 2>&1
    Write-Host $result
    if ($result -match "connected") {
        Write-Host "[OK] Connected successfully." -ForegroundColor Green
        return $true
    }
    Write-Host "[FAIL] Could not connect. Check IP and that wireless debugging is enabled." -ForegroundColor Red
    return $false
}

# --- MAIN ---
Test-ADB
$devList = Get-ConnectedDevices

# If no device connected, prompt for IP
$connected = $devList | Where-Object { $_ -match "\d+\.\d+\.\d+\.\d+" -and $_ -match "device$" }
if (-not $connected) {
    if (-not $DeviceIP) {
        $DeviceIP = Read-Host "`nNo device found. Enter your phone's IP address (from Settings > Developer options > Wireless debugging)"
    }
    if ($DeviceIP) {
        Connect-Wireless -IP $DeviceIP -P $Port
        Get-ConnectedDevices
    }
} else {
    Write-Host "`n[OK] Device already connected: $connected" -ForegroundColor Green
}
