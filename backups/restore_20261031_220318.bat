@echo off
:: RESTORE SCRIPT for Xiaomi 13 (fuxi) - HyperOS 3 / Android 16
:: Generated: 10/31/2026 22:03:18
:: Run this if anything needs to be restored

set ADB=D:\Debload Phone with a Single Command\platform-tools\adb.exe

echo Restoring removed packages...
"%ADB%" connect 192.168.1.160:37249 >nul
"%ADB%" shell pm install-existing --user 0 com.miui.personalassistant
"%ADB%" shell pm install-existing --user 0 com.miui.contentextension
"%ADB%" shell pm install-existing --user 0 com.miui.hybrid
"%ADB%" shell pm install-existing --user 0 com.miui.systemAdSolution
"%ADB%" shell pm install-existing --user 0 com.miui.bugreport
"%ADB%" shell pm install-existing --user 0 com.xiaomi.joyose
"%ADB%" shell pm install-existing --user 0 com.xiaomi.metoknlp
"%ADB%" shell pm install-existing --user 0 com.miui.yellowpage
"%ADB%" shell pm install-existing --user 0 com.xiaomi.payment
"%ADB%" shell pm install-existing --user 0 com.miui.nextpay
"%ADB%" shell pm install-existing --user 0 org.mipay.android.manager
"%ADB%" shell pm install-existing --user 0 com.miui.cit
"%ADB%" shell pm install-existing --user 0 com.miui.voiceassist
"%ADB%" shell pm install-existing --user 0 com.miui.voiceassistoverlay
"%ADB%" shell pm install-existing --user 0 com.miui.voicetrigger
"%ADB%" shell pm install-existing --user 0 com.miui.uireporter
"%ADB%" shell pm install-existing --user 0 com.xiaomi.gamecenter.sdk.service
"%ADB%" shell pm install-existing --user 0 com.xiaomi.minigame
"%ADB%" shell pm install-existing --user 0 com.xiaomi.migameservice
"%ADB%" shell pm install-existing --user 0 com.sohu.inputmethod.sogou.xiaomi
"%ADB%" shell pm install-existing --user 0 com.miui.rom
"%ADB%" shell pm install-existing --user 0 com.miui.contentcatcher
echo Restore complete.
pause
