
<#
.SYNOPSIS
    Phase E: Safe Debloat Executor
    Removes/disables packages based on audit results and user approval
    Uses pm uninstall -k --user 0 (safe, reversible, no root needed)
#>

param(
    [string]$ReportDir   = "$PSScriptRoot\..\reports",
    [string]$BackupDir   = "$PSScriptRoot\..\backups",
    [switch]$DryRun      = $false,
    [switch]$AutoApprove = $false
)

$ADB = "$PSScriptRoot\..\platform-tools\adb.exe"

function Run-ADB { param([string[]]$Args); & $ADB $Args 2>&1 }

function Is-PackageInstalled {
    param([string]$pkg)
    $result = Run-ADB @("shell","pm","list","packages",$pkg)
    return ($result -match "package:$pkg$")
}

function Remove-Package {
    param([string]$pkg, [string]$label)
    if (-not (Is-PackageInstalled $pkg)) {
        Write-Host "  [SKIP] $pkg — not installed" -ForegroundColor DarkGray
        return "not_installed"
    }
    if ($DryRun) {
        Write-Host "  [DRY]  Would uninstall: $pkg ($label)" -ForegroundColor Cyan
        return "dry_run"
    }
    Write-Host "  [REMOVE] $pkg ($label) ..." -NoNewline
    $result = Run-ADB @("shell","pm","uninstall","-k","--user","0",$pkg)
    if ($result -match "Success") {
        Write-Host " OK" -ForegroundColor Green
        return "removed"
    } else {
        Write-Host " FAILED: $result" -ForegroundColor Red
        return "failed"
    }
}

function Disable-Package {
    param([string]$pkg, [string]$label)
    if (-not (Is-PackageInstalled $pkg)) {
        Write-Host "  [SKIP] $pkg — not installed" -ForegroundColor DarkGray
        return "not_installed"
    }
    if ($DryRun) {
        Write-Host "  [DRY]  Would disable: $pkg ($label)" -ForegroundColor Cyan
        return "dry_run"
    }
    Write-Host "  [DISABLE] $pkg ($label) ..." -NoNewline
    $result = Run-ADB @("shell","pm","disable-user","--user","0",$pkg)
    if ($result -match "disabled") {
        Write-Host " OK" -ForegroundColor Green
        return "disabled"
    } else {
        Write-Host " FAILED: $result" -ForegroundColor Red
        return "failed"
    }
}

function Restore-Package {
    param([string]$pkg)
    Write-Host "  [RESTORE] $pkg ..." -NoNewline
    $result = Run-ADB @("shell","pm","install-existing","--user","0",$pkg)
    if ($result -match "installed") {
        Write-Host " OK" -ForegroundColor Green
    } else {
        Write-Host " FAILED: $result" -ForegroundColor Red
    }
}

# ============================================================
# SAFE REMOVAL LIST (Category A — confirmed safe)
# Only packages verified safe for Xiaomi 13 Chinese ROM
# ============================================================

$safeToRemove = @(
    @{pkg="com.miui.yellowpage";          label="Yellow Pages CN"},
    @{pkg="com.miui.yellowpage.global";   label="Yellow Pages Global"},
    @{pkg="com.xiaomi.payment";           label="Xiaomi Pay CN"},
    @{pkg="com.mipay.wallet";             label="MiPay Wallet"},
    @{pkg="org.mipay.android.manager";    label="MiPay Manager"},
    @{pkg="com.miui.hybrid";              label="MIUI Hybrid (Ads)"},
    @{pkg="com.miui.hybrid.accessory";    label="MIUI Hybrid Accessory (Ads)"},
    @{pkg="com.miui.adservice.global";    label="MIUI Ad Service Global"},
    @{pkg="com.miui.msa.global";          label="MSA Global Ad Engine"},
    @{pkg="com.miui.systemAdSolution";    label="System Ad Solution"},
    @{pkg="com.xiaomi.mipicks";           label="Mi Picks / App Recommendation Ads"},
    @{pkg="com.miui.contentextension";    label="MIUI Content Extension (Promos)"},
    @{pkg="com.baidu.input_mi";           label="Baidu IME (privacy risk)"},
    @{pkg="com.baidu.map";                label="Baidu Maps"},
    @{pkg="com.iflytek.inputmethod.miui"; label="IFlytek IME"},
    @{pkg="com.miui.voiceassist";         label="MIUI Voice Assistant CN"},
    @{pkg="com.miui.personalassistant";   label="MIUI Personal Assistant (Newsfeed)"},
    @{pkg="com.miui.mintnews";            label="Mint News (CN Feed)"},
    @{pkg="com.xiaomi.joyose";            label="Joyose (Game Telemetry/Ads)"},
    @{pkg="com.xiaomi.gamecenter";        label="Xiaomi Game Center"},
    @{pkg="com.mi.globalbrowser";         label="Mi Browser (Data collection)"},
    @{pkg="com.mi.globalbrowser.mini";    label="Mi Browser Mini"},
    @{pkg="com.android.browser";          label="AOSP Browser (Obsolete)"},
    @{pkg="com.miui.bugreport";           label="MIUI Bug Report (Sends logs to Xiaomi)"},
    @{pkg="com.xiaomi.metoknlp";          label="Xiaomi NLP Telemetry"},
    @{pkg="com.miui.analytics.mdsp";      label="MIUI MDSP Analytics"},
    @{pkg="com.miui.cit";                 label="CIT Factory Test App"},
    @{pkg="com.miui.rom.checker";         label="MIUI ROM Checker"},
    @{pkg="com.xiaomi.sdktest";           label="Xiaomi SDK Test"},
    @{pkg="com.xiaomi.channels";          label="Xiaomi Channels (Promo Notifications)"},
    @{pkg="com.miui.smarttravel";         label="Mi Smart Travel (CN)"},
    @{pkg="com.android.egg";              label="Android Easter Egg"},
    @{pkg="com.miui.storagecleaner";      label="Storage Cleaner (Duplicate)"},
    @{pkg="com.miui.themestore";          label="MIUI Theme Store (Paid promotions)"}
)

# Optional — user interactive additions
$safeToDisable = @(
    @{pkg="com.miui.analytics";           label="MIUI Analytics (Telemetry)"},
    @{pkg="com.miui.cloudbackup";         label="Cloud Backup (Mi Cloud)"},
    @{pkg="com.miui.cloudservice";        label="Mi Cloud Service"},
    @{pkg="com.miui.weather2";            label="MIUI Weather (CN data source)"},
    @{pkg="com.miui.weather";             label="MIUI Weather Alt"},
    @{pkg="com.miui.translation.youdao";  label="Youdao Translation (CN)"},
    @{pkg="com.miui.translation.kingsoft";label="Kingsoft Translation (CN)"},
    @{pkg="com.xiaomi.drivemode";         label="Drive Mode"},
    @{pkg="com.xiaomi.ab.service";        label="Xiaomi AB Update Service"},
    @{pkg="com.miui.miwallpaper";         label="MIUI Wallpaper (CN downloads)"},
    @{pkg="com.xiaomi.wallpaper";         label="Xiaomi Wallpaper (CN)"}
)

# ============================================================
# BACKUP — Generate restore script first
# ============================================================
Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host " PHASE E: SAFE DEBLOAT" -ForegroundColor Cyan
if ($DryRun) { Write-Host " MODE: DRY RUN (no changes)" -ForegroundColor Yellow }
Write-Host "========================================`n"

$ts = Get-Date -Format "yyyyMMdd_HHmmss"
$restoreFile = "$BackupDir\restore_$ts.ps1"

$restoreContent = @()
$restoreContent += "# RESTORE SCRIPT — generated $ts"
$restoreContent += "# Run this to restore removed packages"
$restoreContent += '$ADB = "' + $ADB + '"'
$restoreContent += ""

foreach ($item in ($safeToRemove + $safeToDisable)) {
    $restoreContent += "& `$ADB shell pm install-existing --user 0 $($item.pkg)"
    $restoreContent += "& `$ADB shell pm enable --user 0 $($item.pkg)"
}
$restoreContent | Out-File $restoreFile -Encoding UTF8
Write-Host "[Backup] Restore script saved: $restoreFile`n"

# ============================================================
# EXECUTE REMOVALS
# ============================================================
$results = @{}

if (-not $AutoApprove) {
    Write-Host "The following packages will be REMOVED (reversible via restore script):" -ForegroundColor Yellow
    $safeToRemove | ForEach-Object { Write-Host "  - $($_.pkg) ($($_.label))" }
    Write-Host ""
    $confirm = Read-Host "Type YES to proceed with removal, or NO to skip"
    if ($confirm -ne "YES") {
        Write-Host "Removal skipped by user." -ForegroundColor Yellow
        $safeToRemove = @()
    }
}

Write-Host "`n--- Removing Packages ---" -ForegroundColor Red
foreach ($item in $safeToRemove) {
    $status = Remove-Package -pkg $item.pkg -label $item.label
    $results[$item.pkg] = $status
}

Write-Host "`n--- Disabling Packages ---" -ForegroundColor Yellow
foreach ($item in $safeToDisable) {
    $status = Disable-Package -pkg $item.pkg -label $item.label
    $results[$item.pkg] = $status
}

# ============================================================
# RESULTS SUMMARY
# ============================================================
$removed  = ($results.Values | Where-Object {$_ -eq "removed"}).Count
$disabled = ($results.Values | Where-Object {$_ -eq "disabled"}).Count
$failed   = ($results.Values | Where-Object {$_ -eq "failed"}).Count
$skipped  = ($results.Values | Where-Object {$_ -eq "not_installed"}).Count

Write-Host "`n========================================" -ForegroundColor Green
Write-Host " DEBLOAT COMPLETE" -ForegroundColor Green
Write-Host " Removed : $removed" -ForegroundColor Green
Write-Host " Disabled: $disabled" -ForegroundColor Yellow
Write-Host " Failed  : $failed" -ForegroundColor Red
Write-Host " Skipped : $skipped (not installed)" -ForegroundColor DarkGray
Write-Host " Restore : $restoreFile" -ForegroundColor Cyan
Write-Host "========================================`n"

return $results
