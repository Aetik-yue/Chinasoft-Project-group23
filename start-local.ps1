# 本地开发链路一键启动脚本
#
# 同时启动四条链路，让前端能看到模拟的实时烟感数据并完成端到端联调：
#   1. getData   订阅 MQTT group23 -> 写库（smoke_data / temperature_data / humidity_data）
#   2. simulate  每秒生成正态分布温湿度，发布到 MQTT group23
#   3. backend   起 REST 服务 (8080)，前端从这里拉数据
#   4. frontend  起 vite dev 服务 (5173)，代理 /api -> backend
#
# 启动顺序与策略：
#   - getData / simulate / backend 三个服务并行启动。Spring Boot 上下文加载是启动
#     耗时大头，并行把总启动时间从 Σ(三服务) 压到 max(三服务)，省一两个服务的加载时间。
#   - backend 用 8080 端口探测替代固定盲等，端口通即返回；90s 未就绪给告警
#     但不阻塞，让 Wait-Process 接管（用户可去该窗口看日志）。
#   - frontend 必须在 backend 之后启动：等 backend 端口就绪（或 90s 超时告警后）
#     再起 vite，避免前端首屏代理打到一个还没起好的后端。vite 自身用 5173 端口探测，
#     60s 未就绪给告警但不阻塞。
#   - getData / simulate 是 MQTT 客户端，无本地端口可探测；它们连 broker 靠
#     Spring Bean 初始化自动完成，脚本不再为此固定 sleep。
#   - simulate 与 getData 并行起，getData 订阅完成前 simulate 发的头几秒
#     数据会丢；simulate 持续每秒发，getData 连上后即正常接收，开发场景
#     无影响。如需严格保序，把 simulate 的启动移到 getData 就绪之后即可。
#
# Ctrl+C 会停止所有窗口。
# 也可双击 start-local.bat 启动（便于绕过 PowerShell 执行策略）。

$ErrorActionPreference = "Continue"
$Root = $PSScriptRoot
if (-not $Root) { $Root = Get-Location }

# Maven 启动命令（getData / simulate / backend 共用）
$mavenCmd = "mvn -DskipTests -Dspring-boot.run.jvmArguments=-Duser.timezone=Asia/Shanghai spring-boot:run"

# 第一批：三个后端侧服务并行启动。
#   每个服务：窗口 title、工作目录、就绪探测端口（>0 启动后端口探测；=0 不探测）
$services = @(
    @{ Name = "getData";  Title = "dev-getData";  Path = "$Root\device\getData"; Port = 0 }
    @{ Name = "simulate"; Title = "dev-simulate"; Path = "$Root\device\simulate"; Port = 0 }
    @{ Name = "backend";  Title = "dev-backend";  Path = "$Root\backend";         Port = 8080 }
)

# 第二批：前端，须等 backend 端口就绪后再起（vite 代理依赖后端）
$frontend = @{ Name = "frontend"; Title = "dev-frontend"; Path = "$Root\frontend"; Port = 5173 }

$procs = [System.Collections.ArrayList]::new()

function Test-Port($Port) {
    try {
        $client = New-Object System.Net.Sockets.TcpClient
        $client.Connect("127.0.0.1", $Port)
        $client.Close(); return $true
    } catch { return $false }
}

# 启动一个服务：开 cmd 窗口（title=服务名），cd 到工作目录后执行给定命令；
# 进程记入 $procs 便于 Ctrl+C 时统一清理。
function Start-ServiceProc($svc, $cmd) {
    Write-Host "[$($svc.Name)] 启动中 -> $($svc.Path)" -ForegroundColor Yellow
    $p = Start-Process -FilePath "cmd.exe" `
        -ArgumentList "/c title $($svc.Title) && cd /d $($svc.Path) && $cmd" `
        -PassThru -ErrorAction SilentlyContinue
    if (-not $p) {
        Write-Host "[$($svc.Name)] 启动失败，请检查路径和环境。" -ForegroundColor Red
        return $null
    }
    [void]$procs.Add([PSCustomObject]@{ Name = $svc.Name; Proc = $p })
    return $p
}

# 端口就绪探测：通了返回 $true，超时返回 $false（不抛错，由调用方决定是否阻塞）
function Wait-PortReady($svc, $timeoutSec) {
    Write-Host "[$($svc.Name)] 等待端口 $($svc.Port) 就绪 ..." -ForegroundColor DarkGray
    $deadline = (Get-Date).AddSeconds($timeoutSec)
    while ((Get-Date) -lt $deadline) {
        if (Test-Port $svc.Port) { return $true }
        Start-Sleep -Milliseconds 500
    }
    return $false
}

Write-Host "========================================" -ForegroundColor Magenta
Write-Host "  本地开发链路: getData + simulate + backend (并行) + frontend" -ForegroundColor Magenta
Write-Host "  Ctrl+C 停止全部" -ForegroundColor Magenta
Write-Host "========================================" -ForegroundColor Magenta

# 第一批：并行启动三个后端侧服务
foreach ($svc in $services) {
    Start-ServiceProc $svc $mavenCmd
}

# backend 端口探测替代盲等：就绪即返回，超时给告警但不阻塞
$backend = $services | Where-Object { $_.Port -gt 0 } | Select-Object -First 1
if ($backend) {
    if (Wait-PortReady $backend 90) {
        Write-Host "[$($backend.Name)] 端口 $($backend.Port) 已就绪" -ForegroundColor Green
    } else {
        Write-Host "[$($backend.Name)] 端口 $($backend.Port) 90s 内未就绪，继续等待（请查看该窗口日志）" -ForegroundColor Red
    }
}

# 第二批：backend 就绪（或探测超时）后启动前端
Start-ServiceProc $frontend "npm run dev"
if (Wait-PortReady $frontend 60) {
    Write-Host "[$($frontend.Name)] 端口 $($frontend.Port) 已就绪" -ForegroundColor Green
} else {
    Write-Host "[$($frontend.Name)] 端口 $($frontend.Port) 60s 内未就绪，继续等待（请查看该窗口日志）" -ForegroundColor Red
}

Write-Host ""
Write-Host "----------------------------------------" -ForegroundColor Green
Write-Host "[OK] 启动流程完成:" -ForegroundColor Green
foreach ($svc in $services) { Write-Host "   - $($svc.Name)" -ForegroundColor Gray }
Write-Host "   - $($frontend.Name)" -ForegroundColor Gray
Write-Host "按 Ctrl+C 停止所有服务。" -ForegroundColor White
Write-Host "----------------------------------------" -ForegroundColor Green

# 等待任意一个子进程退出（或服务被 Ctrl+C 终止）
try {
    $ids = @($procs | ForEach-Object { $_.Proc.Id } | Where-Object { $_ })
    if ($ids.Count -gt 0) {
        Wait-Process -Id $ids -ErrorAction SilentlyContinue
    }
} finally {
    Write-Host ""
    Write-Host "=== 收到停止信号，关闭所有服务 ===" -ForegroundColor Red
    foreach ($item in $procs) {
        if (-not $item.Proc) { continue }
        $id = $item.Proc.Id
        # taskkill /T 干掉进程树，连带终止 mvn 启动的 java 子进程
        Start-Process -FilePath "taskkill.exe" -ArgumentList "/PID", $id, "/T", "/F" `
            -NoNewWindow -Wait -ErrorAction SilentlyContinue | Out-Null
        Write-Host "  已停止 $($item.Name)" -ForegroundColor DarkGray
    }
    Write-Host "=== 全部已关闭 ===" -ForegroundColor Red
}
