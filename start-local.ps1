# 本地开发链路一键启动脚本
#
# 同时启动完整的数据链路，让前端能看到模拟的实时烟感数据：
#   1. getData   订阅 MQTT group23 → 写入 smoke_data / temperature_data / humidity_data
#   2. simulate  每秒生成正态分布温湿度，发布到 MQTT group23
#   3. backend   起 REST 服务 (8080)，前端从这里拉数据
#
# 启动顺序有依赖：先拉起 getData 让它连上 MQTT 并订阅，再启动 simulator 开始发数据，
# 最后启动 backend 对外提供接口。Ctrl+C 会按相反顺序停止所有窗口。
#
# 双幂等：已打开的同名 title 窗口会排在后面启动前提醒你关闭；脚本本身可重复运行。
#
# 也可双击 start-local.bat 启动（便于绕过 PowerShell 执行策略）。

$ErrorActionPreference = "Continue"
$Root = $PSScriptRoot
if (-not $Root) { $Root = Get-Location }

# 每个服务的窗口：title=服务名、工作目录、以及启动后留给下游的就绪等待秒数
$services = @(
    @{ Name = "getData";  Title = "dev-getData";  Path = "$Root\device\getData";  Wait = 12 },
    @{ Name = "simulate"; Title = "dev-simulate"; Path = "$Root\device\simulate"; Wait = 6 },
    @{ Name = "backend";  Title = "dev-backend";  Path = "$Root\backend";        Wait = 0 }
)

$procs = [System.Collections.ArrayList]::new()

function Test-Port($Port) {
    try {
        $client = New-Object System.Net.Sockets.TcpClient
        $client.Connect("127.0.0.1", $Port)
        $client.Close(); return $true
    } catch { return $false }
}

Write-Host "========================================" -ForegroundColor Magenta
Write-Host "  本地开发链路: getData -> simulate -> backend" -ForegroundColor Magenta
Write-Host "  Ctrl+C 停止全部" -ForegroundColor Magenta
Write-Host "========================================" -ForegroundColor Magenta

foreach ($svc in $services) {
    Write-Host ""
    Write-Host "[$($svc.Name)] 启动中 -> $($svc.Path)" -ForegroundColor Yellow
    $cmdLine = "cmd /c title $($svc.Title) && cd /d $($svc.Path) && $($svc.Cmd)"
    $p = Start-Process -FilePath "cmd.exe" `
        -ArgumentList "/c title $($svc.Title) && cd /d $($svc.Path) && mvn spring-boot:run" `
        -PassThru -ErrorAction SilentlyContinue
    if (-not $p) {
        Write-Host "[$($svc.Name)] 启动失败，请检查路径和 Maven 环境。" -ForegroundColor Red
        continue
    }
    [void]$procs.Add([PSCustomObject]@{ Name = $svc.Name; Proc = $p; Wait = $svc.Wait })

    if ($svc.Wait -gt 0) {
        Write-Host "[$($svc.Name)] 等待 $($svc.Wait)s，让服务就绪后再启动下一个 ..." -ForegroundColor DarkGray
        Start-Sleep -Seconds $svc.Wait
    }
}

Write-Host ""
Write-Host "----------------------------------------" -ForegroundColor Green
Write-Host "[OK] 全部启动完成:" -ForegroundColor Green
foreach ($svc in $services) { Write-Host "   - $($svc.Name)" -ForegroundColor Gray }
Write-Host "按 Ctrl+C 停止所有服务。" -ForegroundColor White
Write-Host "----------------------------------------" -ForegroundColor Green

# 等待任意一个子进程退出（或服务被 Ctrl+C 终止）
try {
    $ids = @($procs | ForEach-Object { $_.Proc.Id })
    Wait-Process -Id $ids -ErrorAction SilentlyContinue
} finally {
    Write-Host ""
    Write-Host "=== 收到停止信号，关闭所有服务 ===" -ForegroundColor Red
    foreach ($item in $procs) {
        $id = $item.Proc.Id
        # taskkill /T 干掉进程树，连带终止 mvn 启动的 java 子进程
        Start-Process -FilePath "taskkill.exe" -ArgumentList "/PID", $id, "/T", "/F" `
            -NoNewWindow -Wait -ErrorAction SilentlyContinue | Out-Null
        Write-Host "  已停止 $($item.Name)" -ForegroundColor DarkGray
    }
    Write-Host "=== 全部已关闭 ===" -ForegroundColor Red
}
