param(
  [Parameter(Mandatory=$true)] [string]$RootPassword
)

$mysql = Get-Command mysql -ErrorAction SilentlyContinue
if (-not $mysql) {
  Write-Error "mysql client not found in PATH. Install MySQL Server first."
  exit 1
}

& mysql -u root "-p$RootPassword" < "db/01_schema.sql"
& mysql -u root "-p$RootPassword" -e "CREATE USER IF NOT EXISTS 'admin'@'localhost' IDENTIFIED BY 'password123';"
& mysql -u root "-p$RootPassword" -e "GRANT ALL PRIVILEGES ON bus_ticket_system.* TO 'admin'@'localhost'; FLUSH PRIVILEGES;"
& mysql -u root "-p$RootPassword" < "db/02_seed.sql"

Write-Host "MySQL schema initialized. Login user: admin / password123"
