
<#
.SYNOPSIS
    MASTER RUNNER — Xiaomi 13 CN ROM Optimisation Suite
    Runs all phases in sequence with user approval gates
    
    Usage: .\RUN_ALL.ps1 -PhoneIP "192.168.1.xxx" -PhonePort 5555
           .\RUN_ALL.ps1 -PhoneIP "192.168.1.xxx" -PhonePort 37XXX  (wireless debug port)
#>

param(
    [string]$PhoneIP   = "",
    [int]$PhonePort    = 5555,
    [switch]$DryRun    = $false,
    [switch]$SkipAudit = $false
)

$BASE = $PSScriptRoot
$ADB  = "$BASE\platform-tools\adb.exe"
$LOGS = "$BASE\logs"
$ts   = Get-Date -Format "yyyyMMdd_HHmmss"
$LOG  = "$LOGS\master_$ts.log"

# Ensure log dir
New-Item -ItemType Directory -Path $LOGS -Force | Out-Null

function Log {
    param([string]$msg, [string]$color = "White")
    $entry = "[$(Get-Date -Format 'HH:mm:ss')] $msg"
    Write-Host $entry -ForegroundColor $color
    Add-Content -Path $LOG -Value $entry
}

function Run-ADB { param([string[]]$Args); & $ADB $Args 2>&1 }

function Connect-Device {
    param([string]$IP, [int]$Port)
    Log "Connecting to $IP`:$Port ..." "Yellow"
    $result = Run-ADB @("connect","$IP`:$Port")
    Log "Result: $($result -join ' ')"
    if ($result -match "connected|already") {
        Log "[OK] Device connected." "Green"
        return $true
    }
    Log "[FAIL] Could not connect." "Red"
    return $false
}

function Phase-Banner {
    param([string]$Name)
    Log ""
    Log ("=" * 60) "Cyan"
    Log "  $Name" "Cyan"
    Log ("=" * 60) "Cyan"
}

# ============================================================
# HEADER
# ============================================================
Clear-Host
Write-Host @"
╔══════════════════════════════════════════════════════════════╗
║     XIAOMI 13 CN ROM — COMPLETE OPTIMISATION SUITE          ║
║     ADB v$(& $ADB version 2>&1 | Select-String "version 1" | ForEach-Object{$_ -replace ".*version ",""})                                    ║
║     $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")                              ║
╚══════════════════════════════════════════════════════════════╝
"@ -ForegroundColor Cyan

Log "Master runner started. Log: $LOG"
if ($DryRun) { Log "DRY RUN MODE — no changes will be made" "Yellow" }

# ============================================================
# PHASE A: CONNECTION
# ============================================================
Phase-Banner "PHASE A: ADB CONNECTION"

$deviceFound = $false

# Check if already connected
$devices = Run-ADB @("devices")
$connected = $devices | Where-Object { $_ -match "\d+\.\d+\.\d+\.\d+" -and $_ -match "device$" }
if ($connected) {
    Log "[OK] Already connected: $($connected -join ', ')" "Green"
    $deviceFound = $true
} elseif ($PhoneIP) {
    $deviceFound = Connect-Device -IP $PhoneIP -Port $PhonePort
} else {
    Log "No device connected and no IP provided." "Red"
    Log ""
    Log "To connect:" "Yellow"
    Log "  1. On phone: Settings > Developer Options > Wireless Debugging > Enable" "Yellow"
    Log "  2. Note the IP address and port shown" "Yellow"
    Log "  3. Run: .\RUN_ALL.ps1 -PhoneIP 192.168.1.XXX -PhonePort PORT" "Yellow"
    Log ""
    $PhoneIP = Read-Host "Enter your phone's IP address (or press Enter to exit)"
    if (-not $PhoneIP) { exit 1 }
    $PhonePort = Read-Host "Enter the wireless debugging port (shown on phone, default 5555)"
    if (-not $PhonePort) { $PhonePort = 5555 }
    $deviceFound = Connect-Device -IP $PhoneIP -Port $PhonePort
}

if (-not $deviceFound) {
    Log "Cannot proceed without a connected device." "Red"
    exit 1
}

# Get device serial for targeted commands
$serialLine = (Run-ADB @("devices")) | Where-Object { $_ -match "device$" -and $_ -notmatch "List" } | Select-Object -First 1
$SERIAL = $serialLine -replace "\s+device$", ""
Log "Using device: $SERIAL"

# ============================================================
# PHASE B: DEVICE AUDIT
# ============================================================
if (-not $SkipAudit) {
    Phase-Banner "PHASE B: DEVICE AUDIT"
    & "$BASE\scripts\01_device_audit.ps1" 2>&1 | Tee-Object -Append -FilePath $LOG
}

# ============================================================
# PHASE C: PACKAGE DATABASE
# ============================================================
Phase-Banner "PHASE C: PACKAGE RESEARCH"
& "$BASE\scripts\02_package_database.ps1" 2>&1 | Tee-Object -Append -FilePath $LOG

# ============================================================
# PHASE E: DEBLOAT
# ============================================================
Phase-Banner "PHASE E: DEBLOAT"
$debloatArgs = @{ DryRun = $DryRun }
& "$BASE\scripts\03_debloat.ps1" @debloatArgs 2>&1 | Tee-Object -Append -FilePath $LOG

# ============================================================
# PHASE F: NOTIFICATIONS
# ============================================================
Phase-Banner "PHASE F: NOTIFICATION FIX"
& "$BASE\scripts\04_fix_notifications.ps1" 2>&1 | Tee-Object -Append -FilePath $LOG

# ============================================================
# PHASE G: ENGLISH LOCALE
# ============================================================
Phase-Banner "PHASE G: ENGLISH LOCALISATION"
& "$BASE\scripts\05_english_locale.ps1" 2>&1 | Tee-Object -Append -FilePath $LOG

# ============================================================
# PHASE H: PERFORMANCE
# ============================================================
Phase-Banner "PHASE H: PERFORMANCE"
& "$BASE\scripts\06_performance.ps1" 2>&1 | Tee-Object -Append -FilePath $LOG

# ============================================================
# FINAL SUMMARY
# ============================================================
Phase-Banner "COMPLETE — FINAL SUMMARY"
Log "All phases completed. Full log: $LOG" "Green"
Log ""
Log "MANUAL STEPS STILL NEEDED:" "Yellow"
Log "  1. Settings > Apps > WhatsApp > Battery > Select 'Unrestricted'" "Yellow"
Log "  2. Settings > Apps > WhatsApp > Notifications > Enable all" "Yellow"  
Log "  3. Security > Autostart > Enable WhatsApp, Telegram, Gmail" "Yellow"
Log "  4. Settings > Battery > Background App Management > Disable for notification apps" "Yellow"
Log "  5. Install Gboard from Play Store if not installed" "Yellow"
Log "  6. Settings > General > Language > Move English to top" "Yellow"
Log ""
Log "RESTORE COMMAND (if anything goes wrong):" "Cyan"
Log "  Get-ChildItem '$BASE\backups\restore_*.ps1' | Sort LastWriteTime | Select -Last 1 | % { & `$_.FullName }" "Cyan"

