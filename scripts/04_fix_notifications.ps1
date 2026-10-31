
<#
.SYNOPSIS
    Phase F: Notification Fix — Comprehensive
    Fixes WhatsApp, Gmail, Telegram notification delays on Xiaomi 13 CN ROM
    Targets: Doze whitelist, autostart, battery restrictions, FCM, background data
#>

param([string]$DeviceSerial = "")

$ADB = "$PSScriptRoot\..\platform-tools\adb.exe"
$SERIAL = if ($DeviceSerial) { @("-s",$DeviceSerial) } else { @() }

function Run-ADB { param([string[]]$Args); & $ADB @SERIAL $Args 2>&1 }
function ADB-Shell { param([string]$Cmd); Run-ADB @("shell",$Cmd) }

# Priority apps for notification fix
$notifApps = @(
    @{pkg="com.whatsapp";                    name="WhatsApp"},
    @{pkg="com.google.android.gm";           name="Gmail"},
    @{pkg="org.telegram.messenger";          name="Telegram"},
    @{pkg="org.telegram.messenger.web";      name="Telegram Web"},
    @{pkg="com.google.android.apps.messaging";name="Google Messages"},
    @{pkg="com.microsoft.teams";             name="Microsoft Teams"},
    @{pkg="com.slack";                       name="Slack"},
    @{pkg="com.discord";                     name="Discord"},
    @{pkg="com.instagram.android";           name="Instagram"},
    @{pkg="com.facebook.katana";             name="Facebook"},
    @{pkg="com.twitter.android";             name="X/Twitter"}
)

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host " PHASE F: NOTIFICATION FIX" -ForegroundColor Cyan
Write-Host "========================================`n"

# ============================================================
# 1. DOZE WHITELIST — Add critical apps to battery optimization exemption
# ============================================================
Write-Host "[1/7] Adding apps to Doze whitelist (battery optimization exemption)..." -ForegroundColor Yellow

foreach ($app in $notifApps) {
    $installed = Run-ADB @("shell","pm","list","packages",$app.pkg)
    if ($installed -match "package:$($app.pkg)") {
        $result = ADB-Shell "cmd deviceidle whitelist +$($app.pkg)"
        Write-Host "  $($app.name): $result"
    } else {
        Write-Host "  $($app.name): not installed — skip" -ForegroundColor DarkGray
    }
}

# Also ensure GMS and Play Store are whitelisted
$coreApps = @("com.google.android.gms","com.android.vending","com.google.android.gsf","com.xiaomi.xmsf")
foreach ($pkg in $coreApps) {
    $result = ADB-Shell "cmd deviceidle whitelist +$pkg"
    Write-Host "  Core [$pkg]: $result"
}

# ============================================================
# 2. BACKGROUND AUTOSTART — Enable for notification apps
# ============================================================
Write-Host "`n[2/7] Configuring background autostart permissions..." -ForegroundColor Yellow

foreach ($app in $notifApps) {
    $installed = Run-ADB @("shell","pm","list","packages",$app.pkg)
    if ($installed -match "package:$($app.pkg)") {
        # Allow run in background
        $r1 = ADB-Shell "cmd appops set $($app.pkg) RUN_IN_BACKGROUND allow"
        # Allow run any in background
        $r2 = ADB-Shell "cmd appops set $($app.pkg) RUN_ANY_IN_BACKGROUND allow"
        # Allow background data
        $r3 = ADB-Shell "cmd appops set $($app.pkg) WAKE_LOCK allow"
        Write-Host "  $($app.name): BG=$r1 | BGA=$r2 | WL=$r3"
    }
}

# ============================================================
# 3. BATTERY SAVER — Ensure not in restricted mode for key apps
# ============================================================
Write-Host "`n[3/7] Removing battery restrictions from notification apps..." -ForegroundColor Yellow

foreach ($app in $notifApps) {
    $installed = Run-ADB @("shell","pm","list","packages",$app.pkg)
    if ($installed -match "package:$($app.pkg)") {
        # Set app standby bucket to ACTIVE (not restricted)
        $r1 = ADB-Shell "am set-standby-bucket $($app.pkg) active"
        Write-Host "  $($app.name) standby bucket -> active: $r1"
    }
}

# ============================================================
# 4. GLOBAL NOTIFICATION SETTINGS
# ============================================================
Write-Host "`n[4/7] Configuring global notification settings..." -ForegroundColor Yellow

$globalSettings = @(
    @{ns="global"; key="heads_up_notifications_enabled"; val="1";    desc="Heads-up notifications"},
    @{ns="global"; key="notification_badging";           val="1";    desc="Notification badges"},
    @{ns="secure"; key="notification_bubbles";           val="1";    desc="Notification bubbles"},
    @{ns="global"; key="zen_mode";                       val="0";    desc="DND/Zen mode OFF"},
    @{ns="global"; key="app_standby_enabled";            val="1";    desc="App standby (keeps managed)"},
    @{ns="global"; key="adaptive_battery_management_enabled"; val="0"; desc="Adaptive battery (disable aggressive restrictions)"}
)

foreach ($s in $globalSettings) {
    $result = ADB-Shell "settings put $($s.ns) $($s.key) $($s.val)"
    $current = (ADB-Shell "settings get $($s.ns) $($s.key)") -join ""
    Write-Host "  $($s.desc): set=$result | now=$current"
}

# ============================================================
# 5. XIAOMI-SPECIFIC — MIUI Autostart & Background restrictions
# ============================================================
Write-Host "`n[5/7] Xiaomi MIUI-specific notification settings..." -ForegroundColor Yellow

# MIUI autostart allowlist via appops
foreach ($app in $notifApps) {
    $installed = Run-ADB @("shell","pm","list","packages",$app.pkg)
    if ($installed -match "package:$($app.pkg)") {
        # MIUI uses OP_AUTO_START (133 in some MIUI versions)
        $r1 = ADB-Shell "cmd appops set $($app.pkg) AUTO_START allow"
        Write-Host "  $($app.name) AUTO_START: $r1"
    }
}

# Disable MIUI aggressive memory management for key apps
# MIUI Background App Kill Prevention
Write-Host "  Checking MIUI memory killer settings..."
$miuiMemKey = ADB-Shell "settings get global miui_optimization"
Write-Host "  MIUI optimization: $miuiMemKey"

# ============================================================
# 6. NETWORK — Background data & unrestricted
# ============================================================
Write-Host "`n[6/7] Background network access..." -ForegroundColor Yellow

foreach ($app in $notifApps) {
    $installed = Run-ADB @("shell","pm","list","packages",$app.pkg)
    if ($installed -match "package:$($app.pkg)") {
        # Get UID for network policy
        $uid = (ADB-Shell "cmd package resolve-activity --brief $($app.pkg)") -join ""
        # Remove background network restrictions
        $r1 = ADB-Shell "cmd netpolicy set restrict-background-app $($app.pkg) false"
        Write-Host "  $($app.name) network restriction removed: $r1"
    }
}

# ============================================================
# 7. VERIFY — Check WhatsApp specifically
# ============================================================
Write-Host "`n[7/7] WhatsApp notification verification..." -ForegroundColor Yellow

$waChecks = @(
    @{cmd="cmd deviceidle whitelist";        grep="whatsapp";   label="Doze whitelist"},
    @{cmd="cmd appops get com.whatsapp RUN_IN_BACKGROUND"; grep=""; label="BG run permission"},
    @{cmd="cmd appops get com.whatsapp AUTO_START";         grep=""; label="Autostart permission"},
    @{cmd="am get-standby-bucket com.whatsapp";             grep=""; label="Standby bucket"}
)

foreach ($check in $waChecks) {
    $result = ADB-Shell $check.cmd
    if ($check.grep) {
        $result = $result | Where-Object {$_ -match $check.grep}
    }
    Write-Host "  WA $($check.label): $($result -join ' ')"
}

Write-Host "`n========================================" -ForegroundColor Green
Write-Host " NOTIFICATION FIX COMPLETE" -ForegroundColor Green
Write-Host " IMPORTANT: Also manually verify in Settings:" -ForegroundColor Yellow
Write-Host "   > Apps > WhatsApp > Battery > Unrestricted" -ForegroundColor Yellow
Write-Host "   > Apps > WhatsApp > Notifications > All enabled" -ForegroundColor Yellow
Write-Host "   > Security > Autostart > WhatsApp enabled" -ForegroundColor Yellow
Write-Host "========================================`n"
