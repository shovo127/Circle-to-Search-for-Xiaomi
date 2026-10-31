@echo off
:: ==============================================================
:: MASTER RESTORE SCRIPT FOR XIAOMI 13 (fuxi) - HyperOS 3 / Android 16
:: Generated: 10/31/2026 22:39:59
:: Reversible uninstalls restored via pm install-existing --user 0
:: ==============================================================

set ADB=D:\Debload Phone with a Single Command\platform-tools\adb.exe
echo Connecting to device...
"%ADB%" connect 192.168.1.160:37249

echo Restoring packages...
"%ADB%" shell pm install-existing --user 0 com.android.browser
"%ADB%" shell pm enable --user 0 com.android.browser >nul 2>&1
"%ADB%" shell pm unsuspend --user 0 com.android.browser >nul 2>&1
"%ADB%" shell pm install-existing --user 0 com.android.packageinstaller
"%ADB%" shell pm enable --user 0 com.android.packageinstaller >nul 2>&1
"%ADB%" shell pm unsuspend --user 0 com.android.packageinstaller >nul 2>&1
"%ADB%" shell pm install-existing --user 0 com.miui.bugreport
"%ADB%" shell pm enable --user 0 com.miui.bugreport >nul 2>&1
"%ADB%" shell pm unsuspend --user 0 com.miui.bugreport >nul 2>&1
"%ADB%" shell pm install-existing --user 0 com.miui.cit
"%ADB%" shell pm enable --user 0 com.miui.cit >nul 2>&1
"%ADB%" shell pm unsuspend --user 0 com.miui.cit >nul 2>&1
"%ADB%" shell pm install-existing --user 0 com.miui.contentcatcher
"%ADB%" shell pm enable --user 0 com.miui.contentcatcher >nul 2>&1
"%ADB%" shell pm unsuspend --user 0 com.miui.contentcatcher >nul 2>&1
"%ADB%" shell pm install-existing --user 0 com.miui.contentextension
"%ADB%" shell pm enable --user 0 com.miui.contentextension >nul 2>&1
"%ADB%" shell pm unsuspend --user 0 com.miui.contentextension >nul 2>&1
"%ADB%" shell pm install-existing --user 0 com.miui.greenguard
"%ADB%" shell pm enable --user 0 com.miui.greenguard >nul 2>&1
"%ADB%" shell pm unsuspend --user 0 com.miui.greenguard >nul 2>&1
"%ADB%" shell pm install-existing --user 0 com.miui.hybrid
"%ADB%" shell pm enable --user 0 com.miui.hybrid >nul 2>&1
"%ADB%" shell pm unsuspend --user 0 com.miui.hybrid >nul 2>&1
"%ADB%" shell pm install-existing --user 0 com.miui.nextpay
"%ADB%" shell pm enable --user 0 com.miui.nextpay >nul 2>&1
"%ADB%" shell pm unsuspend --user 0 com.miui.nextpay >nul 2>&1
"%ADB%" shell pm install-existing --user 0 com.miui.personalassistant
"%ADB%" shell pm enable --user 0 com.miui.personalassistant >nul 2>&1
"%ADB%" shell pm unsuspend --user 0 com.miui.personalassistant >nul 2>&1
"%ADB%" shell pm install-existing --user 0 com.miui.rom
"%ADB%" shell pm enable --user 0 com.miui.rom >nul 2>&1
"%ADB%" shell pm unsuspend --user 0 com.miui.rom >nul 2>&1
"%ADB%" shell pm install-existing --user 0 com.miui.systemAdSolution
"%ADB%" shell pm enable --user 0 com.miui.systemAdSolution >nul 2>&1
"%ADB%" shell pm unsuspend --user 0 com.miui.systemAdSolution >nul 2>&1
"%ADB%" shell pm install-existing --user 0 com.miui.uireporter
"%ADB%" shell pm enable --user 0 com.miui.uireporter >nul 2>&1
"%ADB%" shell pm unsuspend --user 0 com.miui.uireporter >nul 2>&1
"%ADB%" shell pm install-existing --user 0 com.miui.voiceassist
"%ADB%" shell pm enable --user 0 com.miui.voiceassist >nul 2>&1
"%ADB%" shell pm unsuspend --user 0 com.miui.voiceassist >nul 2>&1
"%ADB%" shell pm install-existing --user 0 com.miui.voiceassistoverlay
"%ADB%" shell pm enable --user 0 com.miui.voiceassistoverlay >nul 2>&1
"%ADB%" shell pm unsuspend --user 0 com.miui.voiceassistoverlay >nul 2>&1
"%ADB%" shell pm install-existing --user 0 com.miui.voicetrigger
"%ADB%" shell pm enable --user 0 com.miui.voicetrigger >nul 2>&1
"%ADB%" shell pm unsuspend --user 0 com.miui.voicetrigger >nul 2>&1
"%ADB%" shell pm install-existing --user 0 com.miui.yellowpage
"%ADB%" shell pm enable --user 0 com.miui.yellowpage >nul 2>&1
"%ADB%" shell pm unsuspend --user 0 com.miui.yellowpage >nul 2>&1
"%ADB%" shell pm install-existing --user 0 com.sohu.inputmethod.sogou.xiaomi
"%ADB%" shell pm enable --user 0 com.sohu.inputmethod.sogou.xiaomi >nul 2>&1
"%ADB%" shell pm unsuspend --user 0 com.sohu.inputmethod.sogou.xiaomi >nul 2>&1
"%ADB%" shell pm install-existing --user 0 com.xiaomi.aiasst.service
"%ADB%" shell pm enable --user 0 com.xiaomi.aiasst.service >nul 2>&1
"%ADB%" shell pm unsuspend --user 0 com.xiaomi.aiasst.service >nul 2>&1
"%ADB%" shell pm install-existing --user 0 com.xiaomi.aiasst.vision
"%ADB%" shell pm enable --user 0 com.xiaomi.aiasst.vision >nul 2>&1
"%ADB%" shell pm unsuspend --user 0 com.xiaomi.aiasst.vision >nul 2>&1
"%ADB%" shell pm install-existing --user 0 com.xiaomi.aiservice
"%ADB%" shell pm enable --user 0 com.xiaomi.aiservice >nul 2>&1
"%ADB%" shell pm unsuspend --user 0 com.xiaomi.aiservice >nul 2>&1
"%ADB%" shell pm install-existing --user 0 com.xiaomi.barrage
"%ADB%" shell pm enable --user 0 com.xiaomi.barrage >nul 2>&1
"%ADB%" shell pm unsuspend --user 0 com.xiaomi.barrage >nul 2>&1
"%ADB%" shell pm install-existing --user 0 com.xiaomi.gamecenter.sdk.service
"%ADB%" shell pm enable --user 0 com.xiaomi.gamecenter.sdk.service >nul 2>&1
"%ADB%" shell pm unsuspend --user 0 com.xiaomi.gamecenter.sdk.service >nul 2>&1
"%ADB%" shell pm install-existing --user 0 com.xiaomi.joyose
"%ADB%" shell pm enable --user 0 com.xiaomi.joyose >nul 2>&1
"%ADB%" shell pm unsuspend --user 0 com.xiaomi.joyose >nul 2>&1
"%ADB%" shell pm install-existing --user 0 com.xiaomi.metoknlp
"%ADB%" shell pm enable --user 0 com.xiaomi.metoknlp >nul 2>&1
"%ADB%" shell pm unsuspend --user 0 com.xiaomi.metoknlp >nul 2>&1
"%ADB%" shell pm install-existing --user 0 com.xiaomi.migameservice
"%ADB%" shell pm enable --user 0 com.xiaomi.migameservice >nul 2>&1
"%ADB%" shell pm unsuspend --user 0 com.xiaomi.migameservice >nul 2>&1
"%ADB%" shell pm install-existing --user 0 com.xiaomi.minigame
"%ADB%" shell pm enable --user 0 com.xiaomi.minigame >nul 2>&1
"%ADB%" shell pm unsuspend --user 0 com.xiaomi.minigame >nul 2>&1
"%ADB%" shell pm install-existing --user 0 com.xiaomi.payment
"%ADB%" shell pm enable --user 0 com.xiaomi.payment >nul 2>&1
"%ADB%" shell pm unsuspend --user 0 com.xiaomi.payment >nul 2>&1
"%ADB%" shell pm install-existing --user 0 com.xiaomi.security.onetrack
"%ADB%" shell pm enable --user 0 com.xiaomi.security.onetrack >nul 2>&1
"%ADB%" shell pm unsuspend --user 0 com.xiaomi.security.onetrack >nul 2>&1
"%ADB%" shell pm install-existing --user 0 org.mipay.android.manager
"%ADB%" shell pm enable --user 0 org.mipay.android.manager >nul 2>&1
"%ADB%" shell pm unsuspend --user 0 org.mipay.android.manager >nul 2>&1
echo Un-suspending suspended packages...
"%ADB%" shell pm unsuspend --user 0 com.miui.cleanmaster >nul 2>&1
"%ADB%" shell pm unsuspend --user 0 com.mfashiongallery.emag >nul 2>&1

echo Restoring original settings...
"%ADB%" shell settings put global private_dns_mode hostname
"%ADB%" shell settings put global private_dns_specifier dns.adguard.com

echo Master restore completed successfully.
pause
