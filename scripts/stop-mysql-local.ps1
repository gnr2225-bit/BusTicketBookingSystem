Get-Process mysqld -ErrorAction SilentlyContinue | Stop-Process -Force
Write-Host "Stopped mysqld process(es)."
