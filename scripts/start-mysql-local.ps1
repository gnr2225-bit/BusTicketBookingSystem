$mysqld = 'C:\Program Files\MySQL\MySQL Server 8.4\bin\mysqld.exe'
$baseDir = 'C:\Program Files\MySQL\MySQL Server 8.4'
$dataDir = Join-Path $PSScriptRoot '..\mysql-data'
$dataDir = [System.IO.Path]::GetFullPath($dataDir)
$cfgPath = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\mysql-local.ini'))
$baseDirIni = $baseDir -replace '\\','/'
$dataDirIni = $dataDir -replace '\\','/'

if (-not (Test-Path $dataDir)) {
  New-Item -ItemType Directory -Path $dataDir | Out-Null
  & $mysqld --initialize-insecure "--basedir=$baseDir" "--datadir=$dataDir"
}

@"
[mysqld]
basedir=$baseDirIni
datadir=$dataDirIni
port=3306
bind-address=127.0.0.1
log-error=$dataDirIni/runtime.err
"@ | Set-Content -Path $cfgPath -Encoding ASCII

Start-Process -FilePath $mysqld -ArgumentList @(
  "--defaults-file=$cfgPath"
) -WindowStyle Hidden

Start-Sleep -Seconds 3
& 'C:\Program Files\MySQL\MySQL Server 8.4\bin\mysqladmin.exe' -h 127.0.0.1 -P 3306 -u root ping
