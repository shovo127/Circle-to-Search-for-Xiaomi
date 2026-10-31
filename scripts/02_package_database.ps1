
<#
.SYNOPSIS
    Phase C+D: Xiaomi 13 Package Classifier & Safe Debloat List
    Based on UAD-NG, MuntashirAkon/android-debloat-list, community research
    Last updated: 2026-10
    
    Legend:
    A = Safe to remove (pm uninstall -k --user 0)
    B = Safe to disable (pm disable-user --user 0)
    C = Investigate before action
    D = ESSENTIAL - NEVER REMOVE
#>

# ============================================================
# PACKAGE DATABASE — Xiaomi 13 (Shennong / SM8550), HyperOS
# ============================================================

$packages = [ordered]@{

    # ==========================================
    # D: ESSENTIAL — NEVER TOUCH
    # ==========================================
    "com.android.phone"                          = @{cat="D"; label="Android Phone/Dialer"; note="Core telephony"}
    "com.android.server.telecom"                 = @{cat="D"; label="Android Telecom"; note="Call routing"}
    "com.android.contacts"                       = @{cat="D"; label="Android Contacts"; note="Contact provider"}
    "com.android.systemui"                       = @{cat="D"; label="System UI"; note="Status bar, notifications"}
    "com.android.settings"                       = @{cat="D"; label="Settings"; note="System settings"}
    "com.android.launcher3"                      = @{cat="D"; label="Launcher3"; note="Base launcher"}
    "com.miui.home"                              = @{cat="D"; label="Xiaomi Launcher"; note="Primary launcher"}
    "com.android.providers.telephony"            = @{cat="D"; label="Telephony Provider"; note="SMS/MMS data"}
    "com.android.providers.contacts"             = @{cat="D"; label="Contacts Provider"; note="Contact data"}
    "com.android.providers.media"                = @{cat="D"; label="Media Provider"; note="Media access"}
    "com.android.providers.settings"             = @{cat="D"; label="Settings Provider"; note="System settings data"}
    "com.android.providers.downloads"            = @{cat="D"; label="Download Manager"; note="System downloads"}
    "com.android.packageinstaller"               = @{cat="D"; label="Package Installer"; note="App installation"}
    "com.miui.packageinstaller"                  = @{cat="D"; label="MIUI Package Installer"; note="APK installs"}
    "com.google.android.gms"                     = @{cat="D"; label="Google Play Services"; note="Core GMS, FCM"}
    "com.google.android.gsf"                     = @{cat="D"; label="Google Services Framework"; note="Core Google sync"}
    "com.android.vending"                        = @{cat="D"; label="Google Play Store"; note="App store"}
    "com.google.android.syncadapters.contacts"   = @{cat="D"; label="Google Contacts Sync"; note="Account sync"}
    "com.miui.securitycenter"                    = @{cat="D"; label="Xiaomi Security"; note="Antivirus + permissions"}
    "com.miui.securityadd"                       = @{cat="D"; label="Security Add-on"; note="Security extension"}
    "com.xiaomi.xmsf"                            = @{cat="D"; label="Xiaomi Push Service (XMSF)"; note="Push notifications"}
    "com.xiaomi.mipush.sdk.server"               = @{cat="D"; label="MiPush SDK"; note="Push delivery"}
    "com.miui.notification"                      = @{cat="D"; label="MIUI Notification Manager"; note="Notification routing"}
    "com.miui.core"                              = @{cat="D"; label="MIUI Core"; note="Framework core"}
    "com.xiaomi.micloud.sdk"                     = @{cat="D"; label="Xiaomi Cloud SDK"; note="Cloud infrastructure"}
    "com.miui.miservice"                         = @{cat="D"; label="MIUI Service Framework"; note="Core services"}
    "com.android.wifi"                           = @{cat="D"; label="Wi-Fi Service"; note="Connectivity"}
    "com.android.bluetooth"                      = @{cat="D"; label="Bluetooth"; note="Connectivity"}
    "com.android.nfc"                            = @{cat="D"; label="NFC Service"; note="NFC payments/data"}
    "com.xiaomi.bluetooth"                       = @{cat="D"; label="Xiaomi Bluetooth"; note="BT extensions"}
    "com.android.keychain"                       = @{cat="D"; label="Key Chain"; note="Security credentials"}
    "com.android.certinstaller"                  = @{cat="D"; label="Cert Installer"; note="Certificate management"}
    "com.qualcomm.location"                      = @{cat="D"; label="Qualcomm Location"; note="GPS accuracy"}
    "com.xiaomi.location.fused"                  = @{cat="D"; label="Xiaomi Fused Location"; note="Location services"}
    "com.android.location.fused"                 = @{cat="D"; label="Fused Location Provider"; note="Location provider"}
    "com.miui.powerkeeper"                       = @{cat="D"; label="MIUI Power Keeper"; note="Battery management"}
    "com.android.server.devicelock"              = @{cat="D"; label="Device Lock"; note="Security policy"}
    "com.xiaomi.finddevice"                      = @{cat="D"; label="Find My Device"; note="Anti-theft"}
    "com.android.camera2"                        = @{cat="D"; label="AOSP Camera"; note="Camera fallback"}
    "com.android.camera"                         = @{cat="D"; label="AOSP Camera"; note="Camera fallback"}
    "com.xiaomi.camera"                          = @{cat="D"; label="Xiaomi Camera"; note="Primary camera"}
    "com.android.biometrics.dialog"              = @{cat="D"; label="Biometrics Dialog"; note="Fingerprint/Face UI"}
    "com.android.server.biometrics"              = @{cat="D"; label="Biometric Service"; note="Fingerprint/Face"}
    "com.fingerprints.service"                   = @{cat="D"; label="Fingerprint Service"; note="FP unlock"}
    "com.miui.face"                              = @{cat="D"; label="MIUI Face Unlock"; note="Face recognition"}
    "com.android.inputmethod.latin"              = @{cat="D"; label="Latin IME"; note="Keyboard fallback"}
    "com.sohu.inputmethod.sogou.xiaomi"          = @{cat="D"; label="Sogou IME (Xiaomi)"; note="Primary IME — only remove if Gboard installed"}
    "com.google.android.inputmethod.latin"       = @{cat="D"; label="Gboard"; note="Google keyboard"}
    "com.miui.gallery"                           = @{cat="D"; label="MIUI Gallery"; note="Photo viewer"}
    "com.android.mms"                            = @{cat="D"; label="MMS Service"; note="Messaging infrastructure"}
    "com.android.messaging"                      = @{cat="D"; label="Android Messages"; note="SMS fallback"}
    "com.xiaomi.smarthome"                       = @{cat="C"; label="Xiaomi Smart Home"; note="IoT — keep if using Mi devices"}
    "com.xiaomi.market"                          = @{cat="D"; label="GetApps"; note="Xiaomi app store — per user request"}
    "com.xiaomi.getapps"                         = @{cat="D"; label="GetApps Alt"; note="GetApps variant"}
    "com.miui.analytics"                         = @{cat="B"; label="MIUI Analytics"; note="Telemetry — can disable"}
    "com.xiaomi.account"                         = @{cat="D"; label="Xiaomi Account"; note="Mi account login"}
    "com.miui.system"                            = @{cat="D"; label="MIUI System"; note="Core system component"}

    # ==========================================
    # A: SAFE TO REMOVE
    # ==========================================
    "com.miui.yellowpage"                        = @{cat="A"; label="Yellow Pages (CN)"; note="Chinese business directory"}
    "com.miui.yellowpage.global"                 = @{cat="A"; label="Yellow Pages Global"; note="Business directory"}
    "com.xiaomi.payment"                         = @{cat="A"; label="Xiaomi Pay (CN)"; note="Chinese mobile payment"}
    "com.mipay.wallet"                           = @{cat="A"; label="MiPay Wallet"; note="Chinese payment"}
    "com.mipay.wallet.in"                        = @{cat="A"; label="MiPay India"; note="India payment"}
    "com.xiaomi.mipay"                           = @{cat="A"; label="Xiaomi MiPay"; note="Payment service"}
    "com.miui.cleanmaster"                       = @{cat="A"; label="Clean Master"; note="Junk cleaner — aggressive RAM killer"}
    "com.miui.cleaner"                           = @{cat="A"; label="MIUI Cleaner"; note="Storage cleaner — duplicates Security"}
    "com.baidu.input_mi"                         = @{cat="A"; label="Baidu IME"; note="Chinese keyboard — privacy risk"}
    "com.baidu.map"                              = @{cat="A"; label="Baidu Maps"; note="Chinese maps service"}
    "com.iflytek.inputmethod.miui"               = @{cat="A"; label="IFlytek IME"; note="Chinese voice input"}
    "cn.wps.moffice_eng"                         = @{cat="A"; label="WPS Office"; note="Remove if not used — bloatware"}
    "com.tencent.qqlite"                         = @{cat="A"; label="QQ Lite (Tencent)"; note="Chinese social"}
    "com.tencent.mm"                             = @{cat="A"; label="WeChat (Tencent)"; note="Remove if not used"}
    "com.weico.international"                    = @{cat="A"; label="Weico"; note="Chinese Weibo client"}
    "com.sina.weibo"                             = @{cat="A"; label="Weibo"; note="Chinese social media"}
    "com.taobao.taobao"                          = @{cat="A"; label="Taobao (Alibaba)"; note="Chinese e-commerce"}
    "com.tmall.wireless"                         = @{cat="A"; label="Tmall (Alibaba)"; note="Chinese e-commerce"}
    "com.jingdong.app.mall"                      = @{cat="A"; label="JD.com"; note="Chinese e-commerce"}
    "com.xiaomi.shop"                            = @{cat="A"; label="Xiaomi Store"; note="Xiaomi shopping — CN only"}
    "com.xiaomi.vipaccount"                      = @{cat="A"; label="Xiaomi VIP Account"; note="CN loyalty service"}
    "com.miui.contentextension"                  = @{cat="A"; label="MIUI Content Extension"; note="Promotional content injection"}
    "com.miui.hybrid"                            = @{cat="A"; label="MIUI Hybrid"; note="Ad/promo framework"}
    "com.miui.hybrid.accessory"                  = @{cat="A"; label="MIUI Hybrid Accessory"; note="Ad extension"}
    "com.miui.adservice.global"                  = @{cat="A"; label="MIUI Ad Service Global"; note="Ad delivery service"}
    "com.miui.msa.global"                        = @{cat="A"; label="MSA Global (Ad engine)"; note="Xiaomi ad system — major"}
    "com.miui.systemAdSolution"                  = @{cat="A"; label="System Ad Solution"; note="System-level ads"}
    "com.xiaomi.mipicks"                         = @{cat="A"; label="Mi Picks (CN)"; note="App recommendation ads"}
    "com.miui.video"                             = @{cat="A"; label="MIUI Video"; note="CN streaming — replace with YouTube/VLC"}
    "com.miui.player"                            = @{cat="A"; label="MIUI Music Player"; note="CN streaming"}
    "com.miui.fm"                                = @{cat="A"; label="MIUI FM Radio"; note="FM — remove if no FM aerial"}
    "com.miui.videoplayer"                       = @{cat="A"; label="MIUI Video Player"; note="Duplicate video app"}
    "com.xiaomi.mitv.sideo"                      = @{cat="A"; label="Sideo/TV app"; note="Chinese TV streaming"}
    "com.miui.bugreport"                         = @{cat="A"; label="MIUI Bug Report"; note="Sends logs to Xiaomi servers"}
    "com.xiaomi.joyose"                          = @{cat="A"; label="Joyose (Game service)"; note="Game telemetry + ads"}
    "com.xiaomi.gamecenter"                      = @{cat="A"; label="Xiaomi Game Center"; note="Game store"}
    "com.miui.gamedetector"                      = @{cat="A"; label="Game Detector"; note="Game mode detection"}
    "com.miui.mintnews"                          = @{cat="A"; label="Mint News"; note="CN news feed"}
    "com.mi.globalbrowser"                       = @{cat="A"; label="Mi Browser Global"; note="Sends browsing data to Xiaomi"}
    "com.mi.globalbrowser.mini"                  = @{cat="A"; label="Mi Browser Mini"; note="Browser data collection"}
    "com.android.browser"                        = @{cat="A"; label="AOSP Browser"; note="Old browser — obsolete"}
    "com.miui.mishare.connectivity"              = @{cat="B"; label="Mi Share"; note="Local file sharing"}
    "com.miui.miwallpaper"                       = @{cat="B"; label="MIUI Wallpaper"; note="Wallpaper service — downloads CN wallpapers"}
    "com.xiaomi.wallpaper"                       = @{cat="B"; label="Xiaomi Wallpaper"; note="Wallpaper downloads"}
    "com.miui.themestore"                        = @{cat="A"; label="MIUI Theme Store"; note="Promotes paid themes"}
    "com.miui.themes"                            = @{cat="B"; label="MIUI Themes"; note="Theme engine — needed for custom themes"}
    "com.miui.backup"                            = @{cat="B"; label="MIUI Backup"; note="Backup to Mi Cloud"}
    "com.miui.cloudbackup"                       = @{cat="B"; label="Cloud Backup"; note="Mi Cloud backup — disable if not using"}
    "com.miui.cloudservice"                      = @{cat="B"; label="Mi Cloud Service"; note="Sync service"}
    "com.miui.cloudservice.sysbase"              = @{cat="B"; label="Mi Cloud Sysbase"; note="Cloud sync base"}
    "com.miui.vsimcore"                          = @{cat="C"; label="vSIM Core"; note="Virtual SIM — investigate"}
    "com.xiaomi.simactivate.service"             = @{cat="C"; label="SIM Activate Service"; note="Investigate before removing"}
    "com.miui.rom.checker"                       = @{cat="A"; label="ROM Checker"; note="MIUI ROM verification"}
    "com.miui.antispam"                          = @{cat="B"; label="MIUI Anti-spam"; note="Call spam filter"}
    "com.miui.storagecleaner"                    = @{cat="A"; label="Storage Cleaner"; note="Duplicate of Security cleaner"}
    "com.miui.touchassistant"                    = @{cat="B"; label="Touch Assistant"; note="Assistive touch ball"}
    "com.miui.daemon"                            = @{cat="D"; label="MIUI Daemon"; note="Core daemon — do not touch"}
    "com.miui.whetstone"                         = @{cat="B"; label="MIUI Whetstone"; note="Performance optimization service"}
    "com.xiaomi.ld.service"                      = @{cat="C"; label="Xiaomi LD Service"; note="Investigate — possible location/data service"}
    "com.xiaomi.passport"                        = @{cat="D"; label="Xiaomi Passport"; note="Account authentication"}
    "com.miui.systemui.global"                   = @{cat="D"; label="MIUI Global System UI"; note="Global SystemUI extension"}
    "com.miui.qr"                                = @{cat="B"; label="QR Scanner"; note="QR code scanner"}
    "com.miui.translation.youdao"                = @{cat="B"; label="Youdao Translation"; note="CN translation service"}
    "com.miui.translation.kingsoft"              = @{cat="B"; label="Kingsoft Translation"; note="CN translation"}
    "com.google.android.apps.translate"          = @{cat="B"; label="Google Translate"; note="Keep if using translation"}
    "com.miui.voiceassist"                       = @{cat="A"; label="MIUI Voice Assistant"; note="CN voice assistant — use Google instead"}
    "com.miui.voiceassist.global"                = @{cat="A"; label="MIUI Voice Assistant Global"; note="Global voice assistant — use Google/Gemini"}
    "com.xiaomi.xr.screenrecorder"              = @{cat="B"; label="Screen Recorder"; note="Built-in screen recorder"}
    "com.miui.screenrecorder"                    = @{cat="B"; label="MIUI Screen Recorder"; note="Screen recorder"}
    "com.miui.notes"                             = @{cat="B"; label="MIUI Notes"; note="Notes app — replace with Google Keep"}
    "com.miui.calculator"                        = @{cat="B"; label="MIUI Calculator"; note="Keep or replace"}
    "com.miui.compass"                           = @{cat="B"; label="MIUI Compass"; note="Compass app"}
    "com.miui.weather2"                          = @{cat="B"; label="MIUI Weather"; note="CN weather data"}
    "com.miui.weather"                           = @{cat="B"; label="MIUI Weather (alt)"; note="Weather service"}
    "org.mipay.android.manager"                  = @{cat="A"; label="MiPay Manager"; note="CN payment"}
    "com.xiaomi.ab.service"                      = @{cat="B"; label="Xiaomi AB Service"; note="A/B update tracking"}
    "com.android.stk"                            = @{cat="C"; label="SIM Toolkit"; note="Carrier toolkit — investigate"}
    "com.android.stk2"                           = @{cat="C"; label="SIM Toolkit 2"; note="Dual SIM toolkit"}
    "com.miui.filemanager"                       = @{cat="B"; label="MIUI File Manager"; note="Keep or use alternative"}
    "com.android.documentsui"                    = @{cat="D"; label="Documents UI"; note="File picker — essential"}
    "com.miui.bodysensor"                        = @{cat="B"; label="MIUI Body Sensor"; note="Health sensors"}
    "com.miui.misound"                           = @{cat="B"; label="Mi Sound"; note="Audio enhancement"}
    "com.dolby.daxappui"                         = @{cat="B"; label="Dolby DAX"; note="Dolby audio — keep for audio quality"}
    "com.widevine.l1"                            = @{cat="D"; label="Widevine L1"; note="DRM — needed for Netflix HD"}
    "com.xiaomi.micolors"                        = @{cat="B"; label="Mi Colors"; note="Display color profile"}
    "com.miui.screenshot"                        = @{cat="B"; label="MIUI Screenshot"; note="Screenshot tool"}
    "com.xiaomi.scanner"                         = @{cat="B"; label="Xiaomi Scanner"; note="Document scanner"}
    "com.xiaomi.sdktest"                         = @{cat="A"; label="SDK Test"; note="Development artifact — safe remove"}
    "com.xiaomi.channels"                        = @{cat="A"; label="Xiaomi Channels"; note="CN notification promotions"}
    "com.miui.newhome"                           = @{cat="C"; label="MIUI New Home"; note="Investigate — may be launcher component"}
    "com.miui.catcherpatch"                      = @{cat="D"; label="MIUI Catcher Patch"; note="Crash handler"}
    "com.xiaomi.metoknlp"                        = @{cat="A"; label="Xiaomi NLP"; note="CN natural language processing telemetry"}
    "com.miui.privacycomputing"                  = @{cat="B"; label="Privacy Computing"; note="On-device privacy — keep"}
    "com.xiaomi.mipush.sdk"                      = @{cat="D"; label="MiPush Client SDK"; note="Push — essential"}
    "com.miui.cit"                               = @{cat="A"; label="CIT (Factory test)"; note="Factory test app — safe remove"}
    "com.miui.guardprovider"                     = @{cat="B"; label="Guard Provider"; note="Virus scan engine"}
    "com.xiaomi.drivemode"                       = @{cat="B"; label="Drive Mode"; note="Driving assistant"}
    "com.xiaomi.misettings"                      = @{cat="D"; label="Xiaomi Settings Extension"; note="Settings extension"}
    "com.miui.rom"                               = @{cat="A"; label="MIUI ROM Manager"; note="OTA updates via MIUI"}
    "com.xiaomi.mi_connect_service"              = @{cat="B"; label="Mi Connect Service"; note="PC-phone linking"}
    "com.miui.analytics.mdsp"                    = @{cat="A"; label="MIUI MDSP Analytics"; note="Advanced telemetry"}
    "com.miui.personalassistant"                 = @{cat="A"; label="MIUI Personal Assistant (Negative Screen)"; note="Swipe-left newsfeed"}
    "com.miui.smarttravel"                       = @{cat="A"; label="Mi Smart Travel"; note="CN travel service"}
    "com.milink.service"                         = @{cat="B"; label="MiLink Service"; note="Xiaomi cross-device"}
    "com.xiaomi.mircs"                           = @{cat="C"; label="Xiaomi RCS"; note="RCS messaging — investigate"}
    "com.xiaomi.oversea.global"                  = @{cat="C"; label="Xiaomi Oversea Global"; note="Global services framework"}
    "com.android.egg"                            = @{cat="A"; label="Android Easter Egg"; note="Easter egg — cosmetic only"}
    "com.google.android.apps.wellbeing"          = @{cat="B"; label="Digital Wellbeing"; note="Screen time tracking"}
    "com.google.android.dialer"                  = @{cat="B"; label="Google Phone"; note="Alternative dialer"}
    "com.google.android.contacts"                = @{cat="B"; label="Google Contacts"; note="Alternative contacts"}
}

# ============================================================
# OUTPUT FUNCTIONS
# ============================================================

function Export-PackageReport {
    param([string]$OutputFile)
    
    $report = @()
    $report += "XIAOMI 13 PACKAGE AUDIT REPORT"
    $report += "Generated: $(Get-Date)"
    $report += "=" * 80
    
    foreach ($cat in @("A","B","C","D")) {
        $catNames = @{A="SAFE TO REMOVE"; B="SAFE TO DISABLE"; C="INVESTIGATE"; D="ESSENTIAL — DO NOT TOUCH"}
        $report += ""
        $report += "[$cat] $($catNames[$cat])"
        $report += "-" * 60
        $pkgsInCat = $packages.Keys | Where-Object { $packages[$_].cat -eq $cat }
        foreach ($pkg in ($pkgsInCat | Sort-Object)) {
            $info = $packages[$pkg]
            $report += "  $pkg"
            $report += "    Label: $($info.label)"
            $report += "    Note : $($info.note)"
            $report += ""
        }
    }
    
    $report | Out-File $OutputFile -Encoding UTF8
    Write-Host "Package report exported to: $OutputFile"
}

Export-PackageReport -OutputFile "$PSScriptRoot\..\reports\package_audit_database.txt"
Write-Host "Total packages in database: $($packages.Count)"
Write-Host "  A (Remove) : $(($packages.Values | Where-Object {$_.cat -eq 'A'}).Count)"
Write-Host "  B (Disable): $(($packages.Values | Where-Object {$_.cat -eq 'B'}).Count)"
Write-Host "  C (Investigate): $(($packages.Values | Where-Object {$_.cat -eq 'C'}).Count)"
Write-Host "  D (Essential): $(($packages.Values | Where-Object {$_.cat -eq 'D'}).Count)"

# Export the hashtable for use by other scripts
return $packages
