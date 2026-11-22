Unicode true
ManifestDPIAware true
SetCompressor /SOLID lzma
RequestExecutionLevel admin

!include "MUI2.nsh"
!include "LogicLib.nsh"
!include "nsDialogs.nsh"
!include "WinVer.nsh"
!include "x64.nsh"

Var Target
Var Dialog
Var Radio7
Var Radio10
Var Radio11


!define APP_NAME "DevAI Studio"
!define APP_VERSION "2.12.0"
!define APP_EXE "$INSTDIR\runtime\bin\javaw.exe"
!define APP_JAR "$INSTDIR\app\DevAIStudio-all.jar"

Name "${APP_NAME}"
OutFile "DevAI-Studio-Setup-${APP_VERSION}.exe"
InstallDir "$PROGRAMFILES64\DevAI Studio"
InstallDirRegKey HKLM "Software\DevAI Studio" "InstallDir"
BrandingText "DevAI Studio · Android + Vexel"

!define MUI_ABORTWARNING
!define MUI_ICON "icon.ico"
!define MUI_UNICON "icon.ico"
!define MUI_WELCOMEPAGE_TITLE "Установка DevAI Studio"
!define MUI_WELCOMEPAGE_TEXT "DevAI Studio — помощник разработчика для Android, Kotlin и Vexel.$\r$\n$\r$\nВ установщик входит Java Runtime 8, отдельная Java не нужна."
!define MUI_FINISHPAGE_RUN "${APP_EXE}"
!define MUI_FINISHPAGE_RUN_PARAMETERS "-jar $\"${APP_JAR}$\" --target=$Target"
!define MUI_FINISHPAGE_RUN_TEXT "Запустить DevAI Studio"
!define MUI_FINISHPAGE_SHOWREADME "$INSTDIR\README-PC.txt"
!define MUI_FINISHPAGE_SHOWREADME_TEXT "Открыть README"

!insertmacro MUI_PAGE_WELCOME
Page custom TargetPageCreate TargetPageLeave
!insertmacro MUI_PAGE_DIRECTORY
!insertmacro MUI_PAGE_INSTFILES
!insertmacro MUI_PAGE_FINISH
!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES
!insertmacro MUI_LANGUAGE "Russian"

Function .onInit
  SetShellVarContext all
  SetRegView 64
  ${IfNot} ${RunningX64}
    MessageBox MB_ICONSTOP "DevAI Studio требует 64-битную Windows."
    Quit
  ${EndIf}
  StrCpy $Target 10
  ${If} ${IsWin7}
    StrCpy $Target 7
  ${EndIf}
  ReadRegStr $0 HKLM "SOFTWARE\Microsoft\Windows NT\CurrentVersion" "CurrentBuildNumber"
  ${If} $0 != ""
    IntOp $0 $0 + 0
    ${If} $0 >= 22000
      StrCpy $Target 11
    ${EndIf}
  ${EndIf}
FunctionEnd

Function TargetPageCreate
  !insertmacro MUI_HEADER_TEXT "Версия Windows" "Выберите целевую систему"
  nsDialogs::Create 1018
  Pop $Dialog
  ${If} $Dialog == error
    Abort
  ${EndIf}
  ${NSD_CreateLabel} 0 0 100% 30u "Приложение запускается на встроенной Java 8. Выберите версию Windows, чтобы включить подходящий режим оформления и совместимости."
  Pop $0
  ${NSD_CreateRadioButton} 0 38u 100% 14u "Windows 7 (64-bit)"
  Pop $Radio7
  ${NSD_CreateRadioButton} 0 58u 100% 14u "Windows 10 (64-bit)"
  Pop $Radio10
  ${NSD_CreateRadioButton} 0 78u 100% 14u "Windows 11 (64-bit)"
  Pop $Radio11
  ${If} $Target == 7
    ${NSD_SetState} $Radio7 ${BST_CHECKED}
  ${ElseIf} $Target == 11
    ${NSD_SetState} $Radio11 ${BST_CHECKED}
  ${Else}
    ${NSD_SetState} $Radio10 ${BST_CHECKED}
  ${EndIf}
  nsDialogs::Show
FunctionEnd

Function TargetPageLeave
  ${NSD_GetState} $Radio7 $0
  ${If} $0 == ${BST_CHECKED}
    StrCpy $Target 7
  ${EndIf}
  ${NSD_GetState} $Radio10 $0
  ${If} $0 == ${BST_CHECKED}
    StrCpy $Target 10
  ${EndIf}
  ${NSD_GetState} $Radio11 $0
  ${If} $0 == ${BST_CHECKED}
    StrCpy $Target 11
  ${EndIf}
FunctionEnd

Section "DevAI Studio" SecMain
  SetShellVarContext all
  SetOutPath "$INSTDIR"
  File "icon.ico"
  File "README-PC.txt"
  File "DevAI-Android.apk"
  SetOutPath "$INSTDIR\app"
  File "app\DevAIStudio-all.jar"
  SetOutPath "$INSTDIR\runtime"
  File /r "runtime\*.*"
  SetOutPath "$INSTDIR"
  FileOpen $0 "$INSTDIR\target.ini" w
  FileWrite $0 "target=$Target$\r$\n"
  FileClose $0
  WriteUninstaller "$INSTDIR\Uninstall.exe"

  CreateDirectory "$SMPROGRAMS\DevAI Studio"
  CreateShortcut "$SMPROGRAMS\DevAI Studio\DevAI Studio.lnk" "${APP_EXE}" "-jar $\"${APP_JAR}$\" --target=$Target" "$INSTDIR\icon.ico" 0
  CreateShortcut "$DESKTOP\DevAI Studio.lnk" "${APP_EXE}" "-jar $\"${APP_JAR}$\" --target=$Target" "$INSTDIR\icon.ico" 0

  WriteRegStr HKLM "Software\DevAI Studio" "InstallDir" "$INSTDIR"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\DevAI Studio" "DisplayName" "${APP_NAME}"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\DevAI Studio" "DisplayVersion" "${APP_VERSION}"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\DevAI Studio" "Publisher" "DevAI Studio"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\DevAI Studio" "DisplayIcon" "$INSTDIR\icon.ico"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\DevAI Studio" "InstallLocation" "$INSTDIR"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\DevAI Studio" "UninstallString" "$\"$INSTDIR\Uninstall.exe$\""
  WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\DevAI Studio" "NoModify" 1
  WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\DevAI Studio" "NoRepair" 1
SectionEnd

Section "Uninstall"
  SetShellVarContext all
  SetRegView 64
  Delete "$DESKTOP\DevAI Studio.lnk"
  Delete "$SMPROGRAMS\DevAI Studio\DevAI Studio.lnk"
  RMDir "$SMPROGRAMS\DevAI Studio"
  Delete "$INSTDIR\target.ini"
  Delete "$INSTDIR\icon.ico"
  Delete "$INSTDIR\README-PC.txt"
  Delete "$INSTDIR\DevAI-Android.apk"
  Delete "$INSTDIR\Uninstall.exe"
  RMDir /r "$INSTDIR\app"
  RMDir /r "$INSTDIR\runtime"
  RMDir "$INSTDIR"
  DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\DevAI Studio"
  DeleteRegKey HKLM "Software\DevAI Studio"
SectionEnd
