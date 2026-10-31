
<#
.SYNOPSIS
    Phase G: English Localisation Fix
    Forces English locale on Xiaomi 13 CN ROM via ADB
    Handles system locale, MIUI locale, and per-app language where supported
#>

param([string]$DeviceSerial = "")

$ADB = "$PSScriptRoot\..\platform-tools\adb.exe"
$SERIAL = if ($DeviceSerial) { @("-s",$DeviceSerial) } else { @() }

function Run-ADB { param([string[]]$Args); & $ADB @SERIAL $Args 2>&1 }
function ADB-Shell { param([string]$Cmd); Run-ADB @("shell",$Cmd) }

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host " PHASE G: ENGLISH LOCALISATION" -ForegroundColor Cyan
Write-Host "========================================`n"

# --- Current state ---
Write-Host "[1/5] Current locale state:" -ForegroundColor Yellow
$currentLocale   = (ADB-Shell "getprop persist.sys.locale") -join ""
$currentLang     = (ADB-Shell "getprop persist.sys.language") -join ""
$currentCountry  = (ADB-Shell "getprop persist.sys.country") -join ""
$currentRegion   = (ADB-Shell "getprop ro.product.locale") -join ""
$currentConfig   = (ADB-Shell "am get-config") | Select-String "config"
Write-Host "  persist.sys.locale  : $currentLocale"
Write-Host "  persist.sys.language: $currentLang"
Write-Host "  persist.sys.country : $currentCountry"
Write-Host "  ro.product.locale   : $currentRegion"
Write-Host "  am config           : $currentConfig"

# --- Set system locale to en-US ---
Write-Host "`n[2/5] Setting system locale to en-US..." -ForegroundColor Yellow

# Method 1: ADB locale command (Android 7+)
$r1 = ADB-Shell "cmd locale set-app-locales en-US"
Write-Host "  cmd locale: $r1"

# Method 2: Settings put (more reliable on MIUI)
$r2 = ADB-Shell "settings put system system_locales en-US"
Write-Host "  settings system_locales: $r2"

# Method 3: setprop (requires some MIUI versions)
$r3 = ADB-Shell "setprop persist.sys.locale en-US"
Write-Host "  setprop locale: $r3"

$r4 = ADB-Shell "setprop persist.sys.language en"
Write-Host "  setprop language: $r4"

$r5 = ADB-Shell "setprop persist.sys.country US"
Write-Host "  setprop country: $r5"

# Method 4: am broadcast to restart locale
$r6 = ADB-Shell "am broadcast -a android.intent.action.LOCALE_CHANGED"
Write-Host "  Locale broadcast: $r6"

# --- MIUI Region Settings ---
Write-Host "`n[3/5] MIUI region settings..." -ForegroundColor Yellow

# Remove MIUI China-specific services by setting region
$miuiSettings = @(
    @{key="persist.sys.region"; val="US";      desc="Region US"},
    @{key="ro.miui.region";    val="GLOBAL";   desc="MIUI region GLOBAL"},
    @{key="ro.miui.locale";    val="en-US";    desc="MIUI locale en-US"}
)

foreach ($s in $miuiSettings) {
    $r = ADB-Shell "setprop $($s.key) $($s.val)"
    Write-Host "  $($s.desc): $r"
}

# Settings database entries
$settingsList = @(
    @{ns="global"; key="heads_up_notifications_enabled"; val="1"},
    @{ns="system"; key="time_12_24"; val="12"},
    @{ns="system"; key="date_format";val="MM/dd/yyyy"}
)

# --- Chinese Input Methods ---
Write-Host "`n[4/5] Chinese IME check..." -ForegroundColor Yellow

$chineseIMEs = @(
    "com.baidu.input_mi",
    "com.iflytek.inputmethod.miui",
    "com.sohu.inputmethod.sogou.xiaomi"
)

foreach ($ime in $chineseIMEs) {
    $installed = (Run-ADB @("shell","pm","list","packages",$ime)) -join ""
    if ($installed -match "package:$ime") {
        Write-Host "  Chinese IME installed: $ime" -ForegroundColor Yellow
        Write-Host "  Recommendation: Disable and install Gboard (com.google.android.inputmethod.latin)"
    } else {
        Write-Host "  $ime : not installed" -ForegroundColor DarkGray
    }
}

# Check if Gboard is installed
$gboard = (Run-ADB @("shell","pm","list","packages","com.google.android.inputmethod.latin")) -join ""
if ($gboard -match "package:") {
    Write-Host "`n  [OK] Gboard is installed." -ForegroundColor Green
    # Set Gboard as default IME
    $r = ADB-Shell "settings put secure default_input_method com.google.android.inputmethod.latin/com.android.inputmethod.latin.LatinIME"
    Write-Host "  Set default IME to Gboard: $r"
} else {
    Write-Host "`n  [!] Gboard not installed. Install from Play Store for best English experience." -ForegroundColor Yellow
}

# --- Per-app language (Android 13+) ---
Write-Host "`n[5/5] Verifying locale change..." -ForegroundColor Yellow

Start-Sleep -Seconds 2
$newLocale = (ADB-Shell "getprop persist.sys.locale") -join ""
$newConfig  = (ADB-Shell "am get-config") | Select-String "config"
Write-Host "  New locale  : $newLocale"
Write-Host "  New config  : $newConfig"

Write-Host "`n========================================" -ForegroundColor Green
Write-Host " LOCALISATION COMPLETE" -ForegroundColor Green
Write-Host " NOTE: Some Xiaomi apps (Mi Browser, Mi Store) may" -ForegroundColor Yellow
Write-Host " ignore system locale — this is a ROM limitation." -ForegroundColor Yellow
Write-Host " The following CANNOT be translated without a ROM change:" -ForegroundColor Yellow
Write-Host "   - CN-only system apps (Sogou, Baidu services)" -ForegroundColor Yellow
Write-Host "   - Chinese GetApps store listings" -ForegroundColor Yellow
Write-Host "   - MIUI-specific CN-region content" -ForegroundColor Yellow
Write-Host "========================================`n"

