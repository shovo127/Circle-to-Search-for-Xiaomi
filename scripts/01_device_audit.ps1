
<#
.SYNOPSIS
    Phase B: Full Device Audit
    Collects device info, package list, RAM/storage, notifications, locale
#>

param([string]$OutputDir = "$PSScriptRoot\..\reports")

$ADB = "$PSScriptRoot\..\platform-tools\adb.exe"
$ts  = Get-Date -Format "yyyyMMdd_HHmmss"
$rep = "$OutputDir\before_$ts"
New-Item -ItemType Directory -Path $rep -Force | Out-Null

function Run-ADB {
    param([string[]]$Args)
    & $ADB $Args 2>&1
}

function Save-Report {
    param([string]$Name, [string[]]$CmdArgs)
    $file = "$rep\$Name.txt"
    Write-Host "  Collecting: $Name ..." -NoNewline
    $out = Run-ADB $CmdArgs
    $out | Out-File $file -Encoding UTF8
    Write-Host " ($($out.Count) lines)" -ForegroundColor Green
    return $out
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host " PHASE B: DEVICE AUDIT" -ForegroundColor Cyan
Write-Host " Output: $rep" -ForegroundColor Cyan
Write-Host "========================================`n"

# --- Device Identity ---
Write-Host "[1/8] Device Identity" -ForegroundColor Yellow
Save-Report "device_info" @("shell","getprop") | Out-Null

$props = @{
    "Model"       = "ro.product.model"
    "Brand"       = "ro.product.brand"
    "Device"      = "ro.product.device"
    "Android"     = "ro.build.version.release"
    "SDK"         = "ro.build.version.sdk"
    "Build"       = "ro.build.display.id"
    "HyperOS"     = "ro.mi.os.version.name"
    "MIUI"        = "ro.miui.ui.version.name"
    "Fingerprint" = "ro.build.fingerprint"
    "Region"      = "ro.product.locale"
    "Sim Country" = "gsm.sim.operator.iso-country"
}

$identLines = @()
foreach ($k in $props.Keys) {
    $v = (Run-ADB @("shell","getprop",$props[$k])) -join ""
    $identLines += "  $($k.PadRight(14)): $v"
    Write-Host "  $($k.PadRight(14)): $v"
}
$identLines | Out-File "$rep\device_identity_summary.txt" -Encoding UTF8

# --- Package Inventory ---
Write-Host "`n[2/8] Package Inventory" -ForegroundColor Yellow
Save-Report "packages_all"       @("shell","pm","list","packages","-f") | Out-Null
Save-Report "packages_system"    @("shell","pm","list","packages","-s") | Out-Null
Save-Report "packages_third"     @("shell","pm","list","packages","-3") | Out-Null
Save-Report "packages_disabled"  @("shell","pm","list","packages","-d") | Out-Null
Save-Report "packages_enabled"   @("shell","pm","list","packages","-e") | Out-Null

$allPkgs  = (Get-Content "$rep\packages_all.txt" | Where-Object {$_ -match "^package:"})
$sysPkgs  = (Get-Content "$rep\packages_system.txt" | Where-Object {$_ -match "^package:"})
$thirdPkgs= (Get-Content "$rep\packages_third.txt" | Where-Object {$_ -match "^package:"})
$disabledPkgs = (Get-Content "$rep\packages_disabled.txt" | Where-Object {$_ -match "^package:"})

Write-Host "  Total packages : $($allPkgs.Count)"
Write-Host "  System packages: $($sysPkgs.Count)"
Write-Host "  User packages  : $($thirdPkgs.Count)"
Write-Host "  Disabled       : $($disabledPkgs.Count)"

# --- RAM & Storage ---
Write-Host "`n[3/8] RAM & Storage" -ForegroundColor Yellow
Save-Report "memory_info"    @("shell","cat","/proc/meminfo")    | Out-Null
Save-Report "storage_info"   @("shell","df","-h")                | Out-Null
Save-Report "dumpsys_memory" @("shell","dumpsys","meminfo","-a") | Out-Null

$memFree = (Run-ADB @("shell","cat","/proc/meminfo")) | Where-Object {$_ -match "MemAvailable"} | Select-Object -First 1
$storFree = (Run-ADB @("shell","df","-h","/data")) | Select-Object -Last 1
Write-Host "  RAM Available  : $memFree"
Write-Host "  /data Storage  : $storFree"

# --- Notification Configuration ---
Write-Host "`n[4/8] Notification Configuration" -ForegroundColor Yellow
Save-Report "notification_settings" @("shell","settings","list","global") | Out-Null
Save-Report "secure_settings"       @("shell","settings","list","secure") | Out-Null
Save-Report "system_settings"       @("shell","settings","list","system") | Out-Null
Save-Report "notification_policy"   @("shell","cmd","notification","list") | Out-Null

# Key notification-related settings
$notifKeys = @(
    "global/heads_up_notifications_enabled",
    "global/notification_badging",
    "secure/notification_bubbles",
    "global/zen_mode",
    "global/doze_enabled",
    "global/app_standby_enabled",
    "global/adaptive_battery_management_enabled"
)
$notifSummary = @()
foreach ($key in $notifKeys) {
    $parts = $key -split "/"
    $val = (Run-ADB @("shell","settings","get",$parts[0],$parts[1])) -join ""
    $notifSummary += "  $($key.PadRight(50)): $val"
}
$notifSummary | Out-File "$rep\notification_key_settings.txt" -Encoding UTF8
$notifSummary | ForEach-Object { Write-Host $_ }

# --- Language & Locale ---
Write-Host "`n[5/8] Language & Locale" -ForegroundColor Yellow
$localeKeys = @(
    @("shell","getprop","persist.sys.locale"),
    @("shell","getprop","persist.sys.language"),
    @("shell","getprop","persist.sys.country"),
    @("shell","settings","get","system","system_locales"),
    @("shell","getprop","ro.product.locale"),
    @("shell","am","get-config")
)
$localeSummary = @()
foreach ($cmd in $localeKeys) {
    $label = $cmd[-1]
    $val = (Run-ADB $cmd) -join " "
    $localeSummary += "  $($label.PadRight(40)): $val"
    Write-Host "  $($label.PadRight(40)): $val"
}
$localeSummary | Out-File "$rep\locale_settings.txt" -Encoding UTF8

# --- Background & Battery Restrictions ---
Write-Host "`n[6/8] Battery & Background Restrictions" -ForegroundColor Yellow
Save-Report "battery_stats"         @("shell","dumpsys","battery")           | Out-Null
Save-Report "deviceidle_config"     @("shell","dumpsys","deviceidle")        | Out-Null
Save-Report "powermanager"          @("shell","dumpsys","power")              | Out-Null
Save-Report "app_standby_buckets"   @("shell","am","get-standby-bucket")     | Out-Null

# Check WhatsApp specifically
$waAutostart = Run-ADB @("shell","cmd","appops","get","com.whatsapp","RUN_IN_BACKGROUND")
$waBattery   = Run-ADB @("shell","dumpsys","deviceidle","whitelist") | Where-Object {$_ -match "whatsapp"}
Write-Host "  WhatsApp RUN_IN_BACKGROUND: $($waAutostart -join ' ')"
Write-Host "  WhatsApp Doze whitelist   : $($waBattery -join ' ')"

# --- FCM / Google Services ---
Write-Host "`n[7/8] Google Services & FCM" -ForegroundColor Yellow
Save-Report "gms_info" @("shell","dumpsys","package","com.google.android.gms") | Out-Null
$gmsState = (Run-ADB @("shell","pm","list","packages","com.google.android.gms")) -join ""
$fcmState = (Run-ADB @("shell","pm","list","packages","com.google.firebase")) -join ""
Write-Host "  GMS state : $gmsState"
Write-Host "  Firebase  : $fcmState"

# --- Running Processes ---
Write-Host "`n[8/8] Running Processes" -ForegroundColor Yellow
Save-Report "running_processes" @("shell","ps","-A") | Out-Null
Save-Report "top_processes"     @("shell","top","-n","1","-b") | Out-Null
$procCount = (Get-Content "$rep\running_processes.txt").Count
Write-Host "  Running processes: $procCount"

# --- Summary ---
Write-Host "`n========================================" -ForegroundColor Green
Write-Host " AUDIT COMPLETE" -ForegroundColor Green
Write-Host " Reports saved to: $rep" -ForegroundColor Green
Write-Host "========================================`n"

# Return the report path for chaining
return $rep
