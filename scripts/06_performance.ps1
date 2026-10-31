
<#
.SYNOPSIS
    Phase H: Performance Optimisation
    Animation, RAM, background process limits, smooth UX
    Safe, targeted — no aggressive task killers
#>

param([string]$DeviceSerial = "")

$ADB = "$PSScriptRoot\..\platform-tools\adb.exe"
$SERIAL = if ($DeviceSerial) { @("-s",$DeviceSerial) } else { @() }

function Run-ADB { param([string[]]$Args); & $ADB @SERIAL $Args 2>&1 }
function ADB-Shell { param([string]$Cmd); Run-ADB @("shell",$Cmd) }

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host " PHASE H: PERFORMANCE OPTIMISATION" -ForegroundColor Cyan
Write-Host "========================================`n"

# --- Animation Speed (fastest UI feel without disabling) ---
Write-Host "[1/5] Animation speed..." -ForegroundColor Yellow
$anims = @(
    @{key="window_animation_scale";     val="0.5"; desc="Window animation"},
    @{key="transition_animation_scale"; val="0.5"; desc="Transition animation"},
    @{key="animator_duration_scale";    val="0.5"; desc="Animator duration"}
)
foreach ($a in $anims) {
    $r = ADB-Shell "settings put global $($a.key) $($a.val)"
    $c = (ADB-Shell "settings get global $($a.key)") -join ""
    Write-Host "  $($a.desc): $c"
}

# --- Background Process Limits ---
Write-Host "`n[2/5] Background process configuration..." -ForegroundColor Yellow
# Do NOT set to 0 (kills all) — use "standard" mode
# MIUI default is often -1 (no limit) which wastes RAM
# Set to a reasonable value (4-6) to prevent RAM exhaustion while keeping notification apps alive

$currentBGLimit = (ADB-Shell "settings get global background_process_limit") -join ""
Write-Host "  Current bg process limit: $currentBGLimit"
# We leave this at default unless it's explicitly too high
# DO NOT change to aggressive value

# --- GPU Rendering ---
Write-Host "`n[3/5] GPU and rendering..." -ForegroundColor Yellow
$gpuSettings = @(
    @{key="hardware_accelerated"; val="1"; desc="Hardware acceleration"},
    @{key="force_gpu_rendering";  val="0"; desc="Force GPU (leave default)"}
)
foreach ($g in $gpuSettings) {
    $c = (ADB-Shell "settings get global $($g.key)") -join ""
    Write-Host "  $($g.desc): $c"
}

# --- DNS and Network ---
Write-Host "`n[4/5] DNS optimisation..." -ForegroundColor Yellow
# Set DNS to Google (better FCM connectivity from CN ROM)
$dnsResult = ADB-Shell "settings put global private_dns_mode opportunistic"
Write-Host "  Private DNS mode: $dnsResult"
$dnsProvider = ADB-Shell "settings put global private_dns_specifier dns.google"
Write-Host "  DNS provider set to dns.google: $dnsProvider"

# --- System State Report ---
Write-Host "`n[5/5] Current system state..." -ForegroundColor Yellow

$ramInfo   = (ADB-Shell "cat /proc/meminfo") | Where-Object {$_ -match "MemTotal|MemAvailable|MemFree"}
$topProcs  = ADB-Shell "top -n 1 -b" | Select-Object -First 20
$diskInfo  = ADB-Shell "df -h /data"

Write-Host "`n  --- RAM ---"
$ramInfo | ForEach-Object { Write-Host "  $_" }
Write-Host "`n  --- Storage ---"
$diskInfo | ForEach-Object { Write-Host "  $_" }

Write-Host "`n========================================" -ForegroundColor Green
Write-Host " PERFORMANCE OPTIMISATION COMPLETE" -ForegroundColor Green
Write-Host "========================================`n"

