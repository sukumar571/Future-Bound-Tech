# ================================================================
# Future Bound Tech — MySQL backup (Windows)
# Usage (PowerShell):
#   $env:BACKUP_DB_PASSWORD = "<from your secret store>"
#   .\deploy\backup.ps1
#
# Schedule: Task Scheduler -> "Run task", daily 02:15
#   Program: powershell.exe
#   Arguments: -ExecutionPolicy Bypass -File E:\future-bound-tech\deploy\backup.ps1
# ================================================================

$ErrorActionPreference = "Stop"

$DbHost  = if ($env:DB_HOST) { $env:DB_HOST } else { "localhost" }
$DbPort  = if ($env:DB_PORT) { $env:DB_PORT } else { "3306" }
$DbName  = if ($env:DB_NAME) { $env:DB_NAME } else { "future_bound_tech_db" }
$DbUser  = if ($env:BACKUP_DB_USERNAME) { $env:BACKUP_DB_USERNAME } else { "fbt_backup" }
if (-not $env:BACKUP_DB_PASSWORD) { throw "BACKUP_DB_PASSWORD must be set before running backup.ps1" }

$Stamp      = Get-Date -Format "yyyyMMdd-HHmmss"
$BackupDir  = if ($env:BACKUP_DIR) { $env:BACKUP_DIR } else { "E:\backups\future-bound-tech" }
$UploadDir  = if ($env:UPLOAD_DIR) { $env:UPLOAD_DIR } else { ".\uploads" }
$RetentionDays = if ($env:RETENTION_DAYS) { [int]$env:RETENTION_DAYS } else { 30 }

New-Item -ItemType Directory -Force -Path $BackupDir | Out-Null

# 1. Database dump. MYSQL_PWD keeps the password out of the command line
#    and out of any shell history.
$env:MYSQL_PWD = $env:BACKUP_DB_PASSWORD
$dumpFile = Join-Path $BackupDir "db-$Stamp.sql"
& mysqldump "-h$DbHost" "-P$DbPort" "-u$DbUser" `
    --single-transaction --routines --triggers --default-character-set=utf8mb4 `
    $DbName | Out-File -FilePath $dumpFile -Encoding utf8
Remove-Item Env:MYSQL_PWD

Compress-Archive -Path $dumpFile -DestinationPath "$dumpFile.zip" -Force
Remove-Item $dumpFile

# 2. Uploaded files (public + private) travel with every backup.
foreach ($dir in @($UploadDir, $(if ($env:PRIVATE_UPLOAD_DIR) { $env:PRIVATE_UPLOAD_DIR } else { ".\data\private-uploads" }))) {
    if (Test-Path $dir) {
        $name = Split-Path $dir -Leaf
        Compress-Archive -Path (Join-Path $dir "*") -DestinationPath (Join-Path $BackupDir "$name-$Stamp.zip") -Force -ErrorAction SilentlyContinue
    }
}

# 3. Retention.
Get-ChildItem $BackupDir -Filter "*.zip" |
    Where-Object { $_.LastWriteTime -lt (Get-Date).AddDays(-$RetentionDays) } |
    Remove-Item -Force

Write-Host "Backup written to $BackupDir (stamp $Stamp)."
