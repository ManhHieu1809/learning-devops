$ErrorActionPreference = 'Stop'
Get-Content (Join-Path $PSScriptRoot '.env') | ForEach-Object {
  if ($_ -match '^([^=]+)=(.*)$') { [Environment]::SetEnvironmentVariable($matches[1], $matches[2], 'Process') }
}
$env:APP_DB_URL = 'jdbc:mariadb://127.0.0.1:3306/devops_learning'
& (Join-Path $PSScriptRoot 'mvnw.cmd') spring-boot:run
