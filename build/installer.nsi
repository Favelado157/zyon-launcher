; NSIS installer built on Linux; uninstaller is generated on the destination Windows PC.
!include "MUI2.nsh"
!include "LogicLib.nsh"
!include "${BUILD_RESOURCES_DIR}\installer-files.nsh"
Name "Zyon Launcher"
RequestExecutionLevel user
InstallDir "$LOCALAPPDATA\Programs\Zyon Launcher"
InstallDirRegKey HKCU "Software\ZyonLauncherInstall" "InstallDir"
SetCompressor /SOLID lzma
SetCompressorDictSize 32
ShowInstDetails show
ShowUninstDetails show
BrandingText "Zyon Launcher ${VERSION}"
!define MUI_ABORTWARNING
!define MUI_WELCOMEPAGE_TITLE "Bem-vindo ao Zyon Launcher"
!define MUI_WELCOMEPAGE_TEXT "Este assistente instala o Zyon Visual, cliente independente para Minecraft Java 1.8.9.$\r$\n$\r$\nFeche o Zyon Launcher antes de instalar ou atualizar.$\r$\n$\r$\nRequer conta Microsoft com acesso verificado ao Minecraft Java. O aplicativo depende de autorização para os serviços oficiais."
!define MUI_FINISHPAGE_RUN "$INSTDIR\Zyon Launcher.exe"
!define MUI_FINISHPAGE_RUN_TEXT "Abrir Zyon Launcher"
!insertmacro MUI_PAGE_WELCOME
!insertmacro MUI_PAGE_DIRECTORY
!insertmacro MUI_PAGE_INSTFILES
!insertmacro MUI_PAGE_FINISH
!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES
!insertmacro MUI_LANGUAGE "PortugueseBR"

Function .onVerifyInstDir
  ; A fresh install must use an empty directory. Updating our own installation is allowed.
  IfFileExists "$INSTDIR\zyon-install.marker" accepted
  FindFirst $0 $1 "$INSTDIR\*.*"
  check_files:
    StrCmp $1 "" empty
    StrCmp $1 "." next_file
    StrCmp $1 ".." next_file
    FindClose $0
    MessageBox MB_ICONEXCLAMATION "Escolha uma pasta vazia para o Zyon Launcher."
    Abort
  next_file:
    FindNext $0 $1
    Goto check_files
  empty:
    FindClose $0
  accepted:
FunctionEnd

Section "Zyon Launcher" main
  SetShellVarContext current
  SetOutPath "$INSTDIR"
  ; Remove only older client binaries shipped by this application; worlds stay in AppData.
  Delete "$INSTDIR\resources\payload\zyon-1.8.9-1.0.0.jar"
  Delete "$INSTDIR\resources\payload\zyon-1.8.9-1.1.0.jar"
  Delete "$INSTDIR\resources\payload\zyon-1.8.9-1.2.0.jar"
  Delete "$INSTDIR\resources\payload\zyon-1.8.9-1.2.1.jar"
  Delete "$INSTDIR\resources\payload\zyon-1.8.9-1.2.2.jar"
  Delete "$INSTDIR\resources\payload\zyon-1.8.9-1.3.0.jar"
  File /r "${PROJECT_DIR}\dist\win-unpacked\*"
  FileOpen $0 "$INSTDIR\zyon-install.marker" w
  FileWrite $0 "dev.zyon.launcher"
  FileClose $0
  WriteUninstaller "$INSTDIR\Uninstall Zyon Launcher.exe"
  CreateDirectory "$SMPROGRAMS\Zyon Launcher"
  CreateShortcut "$SMPROGRAMS\Zyon Launcher\Zyon Launcher.lnk" "$INSTDIR\Zyon Launcher.exe" "" "$INSTDIR\resources\zyon-icon-${VERSION}.ico" 0
  CreateShortcut "$DESKTOP\Zyon Launcher.lnk" "$INSTDIR\Zyon Launcher.exe" "" "$INSTDIR\resources\zyon-icon-${VERSION}.ico" 0
  WriteRegStr HKCU "Software\ZyonLauncherInstall" "InstallDir" "$INSTDIR"
  WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\ZyonLauncher" "DisplayName" "Zyon Launcher"
  WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\ZyonLauncher" "DisplayVersion" "${VERSION}"
  WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\ZyonLauncher" "Publisher" "Zyon"
  WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\ZyonLauncher" "DisplayIcon" "$INSTDIR\resources\zyon-icon-${VERSION}.ico"
  WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\ZyonLauncher" "UninstallString" '"$INSTDIR\Uninstall Zyon Launcher.exe"'
  WriteRegDWORD HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\ZyonLauncher" "NoModify" 1
  WriteRegDWORD HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\ZyonLauncher" "NoRepair" 1
SectionEnd

Section "Uninstall"
  SetShellVarContext current
  ; Remove only files shipped with the launcher. Instances and worlds in AppData are retained.
  !insertmacro ZyonUninstallFiles
  Delete "$INSTDIR\zyon-install.marker"
  Delete "$INSTDIR\Uninstall Zyon Launcher.exe"
  Delete "$DESKTOP\Zyon Launcher.lnk"
  Delete "$SMPROGRAMS\Zyon Launcher\Zyon Launcher.lnk"
  RMDir "$SMPROGRAMS\Zyon Launcher"
  RMDir "$INSTDIR"
  DeleteRegKey HKCU "Software\ZyonLauncherInstall"
  DeleteRegKey HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\ZyonLauncher"
SectionEnd
