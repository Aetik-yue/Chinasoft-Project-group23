# 智慧宠物烟感系统 - 配置工具 (config.ps1)
# 双击 config.bat 启动;或在 PowerShell 里: powershell -ExecutionPolicy Bypass -File config.ps1
#
# 改两类东西:
#   后端 -> Windows 用户环境变量(永久保存,所有新开的终端都能读到;Spring 用 ${VAR:} 读取)
#   前端 -> frontend/.env.local 文件(Vite 启动时读取)

$ErrorActionPreference = 'Stop'
try { [Console]::OutputEncoding = [Text.Encoding]::UTF8 } catch {}

$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
$EnvLocal = Join-Path $Root 'frontend\.env.local'

# ---- 变量清单 ----
$backendVars = @(
    @{ Name='DEEPSEEK_API_KEY';    Label='DeepSeek API Key (QQ 智能体)';        Secret=$true  }
    @{ Name='QWEN_API_KEY';        Label='Qwen/通义千问 API Key (鹦鹉视觉)';     Secret=$true  }
    @{ Name='MAXKB_API_KEY';       Label='MaxKB 应用 API Key';                  Secret=$true  }
    @{ Name='MAXKB_APP_ID';        Label='MaxKB 应用 ID';                       Secret=$false }
    @{ Name='MAXKB_BASE_URL';      Label='MaxKB 服务地址(必须带 /chat)';       Secret=$false }
    @{ Name='ONEBOT_ACCESS_TOKEN'; Label='NapCat OneBot Token';                 Secret=$true  }
    @{ Name='API_KEY_SECRET';      Label='Key 加密主密钥(建议改成随机值)';      Secret=$true  }
    @{ Name='MYSQL_PASSWORD';      Label='MySQL 密码(device 模块用)';          Secret=$true  }
    @{ Name='MYSQL_URL';           Label='MySQL 连接 URL(device 模块)';         Secret=$false }
    @{ Name='MYSQL_USERNAME';      Label='MySQL 用户名(device 模块)';          Secret=$false }
    @{ Name='MQTT_HOST_URL';       Label='MQTT Broker 地址';                    Secret=$false }
    @{ Name='MQTT_USERNAME';       Label='MQTT 用户名';                         Secret=$false }
    @{ Name='MQTT_PASSWORD';      Label='MQTT 密码';                           Secret=$true  }
)
$frontendVars = @(
    @{ Name='VITE_BACKEND_HOST';         Label='前端连的后端主机(IP/localhost)'; Secret=$false }
    @{ Name='VITE_FALLBACK_HOST';        Label='备用后端主机';                  Secret=$false }
    @{ Name='VITE_MAXKB_HOST';           Label='MaxKB 主机';                    Secret=$false }
    @{ Name='VITE_MAXKB_PORT';           Label='MaxKB 端口';                    Secret=$false }
    @{ Name='VITE_AMAP_KEY';             Label='高德地图 Key';                  Secret=$true  }
    @{ Name='VITE_AMAP_SECURITY_CODE';  Label='高德安全密钥';                  Secret=$true  }
)

# 拼成带序号的扁平列表
$flat = @()
$i = 1
foreach ($v in $backendVars)  { $flat += [PSCustomObject]@{ No=$i++; Group='后端'; Name=$v.Name; Label=$v.Label; Secret=$v.Secret } }
foreach ($v in $frontendVars) { $flat += [PSCustomObject]@{ No=$i++; Group='前端'; Name=$v.Name; Label=$v.Label; Secret=$v.Secret } }

# ---- 工具函数 ----
function Mask([string]$v) {
    if ([string]::IsNullOrWhiteSpace($v)) { return '(未设置)' }
    if ($v.Length -le 8) { return '****' }
    return $v.Substring(0,3) + '****' + $v.Substring($v.Length - 4)
}
function Get-Backend([string]$name) { [Environment]::GetEnvironmentVariable($name, 'User') }
function Set-Backend([string]$name, [string]$value) {
    if ([string]::IsNullOrWhiteSpace($value)) {
        [Environment]::SetEnvironmentVariable($name, $null, 'User')   # 传 null = 删除该变量
    } else {
        [Environment]::SetEnvironmentVariable($name, $value, 'User')  # 'User' 作用域 = 永久,写注册表
    }
}
function Get-Frontend([string]$name) {
    if (-not (Test-Path $EnvLocal)) { return $null }
    foreach ($l in Get-Content -LiteralPath $EnvLocal -Encoding UTF8) {
        if ($l -match ('^\s*' + [regex]::Escape($name) + '\s*=\s*(.*)$')) { return $matches[1].Trim() }
    }
    return $null
}
function Set-Frontend([string]$name, [string]$value) {
    # 只替换/追加目标行,保留注释和其他行
    $lines = if (Test-Path $EnvLocal) { Get-Content -LiteralPath $EnvLocal -Encoding UTF8 } else { @() }
    $found = $false
    $out = @()
    foreach ($l in $lines) {
        if ($l -match ('^\s*' + [regex]::Escape($name) + '\s*=')) {
            if (-not [string]::IsNullOrWhiteSpace($value)) { $out += "$name=$value" }
            $found = $true
        } else { $out += $l }
    }
    if (-not $found -and -not [string]::IsNullOrWhiteSpace($value)) { $out += "$name=$value" }
    $dir = Split-Path -Parent $EnvLocal
    if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }
    [IO.File]::WriteAllLines($EnvLocal, $out, [Text.UTF8Encoding]::new($false))
}

function Show-Current {
    Clear-Host
    Write-Host "================ 配置工具 ================" -ForegroundColor Cyan
    Write-Host "后端=Windows用户环境变量(永久)   前端=frontend/.env.local" -ForegroundColor DarkGray
    Write-Host "==========================================" -ForegroundColor Cyan
    Write-Host ""
    Write-Host '[后端] Spring 用 ${VAR:} 语法读这些(改完要重开终端 + 重启后端)' -ForegroundColor Yellow
    foreach ($f in ($flat | Where-Object Group -eq '后端')) {
        $cur = Get-Backend $f.Name
        $show = if ($f.Secret) { Mask $cur } else { $cur }
        Write-Host ("  {0,2}. {1,-24} {2}" -f $f.No, $f.Name, $(if($show){$show}else{'(未设置)'}))
    }
    Write-Host ""
    Write-Host '[前端] Vite 启动时读 .env.local(改完要重启 Vite)' -ForegroundColor Yellow
    foreach ($f in ($flat | Where-Object Group -eq '前端')) {
        $cur = Get-Frontend $f.Name
        $show = if ($f.Secret) { Mask $cur } else { $cur }
        Write-Host ("  {0,2}. {1,-26} {2}" -f $f.No, $f.Name, $(if($show){$show}else{'(未设置)'}))
    }
    Write-Host ""
    Write-Host "  c = 清除某项    v = 刷新    q = 退出" -ForegroundColor Green
}

# ---- 主循环 ----
while ($true) {
    Show-Current
    $choice = Read-Host "输入编号修改(或 c/v/q)"
    if ($choice -eq 'q') { break }
    if ($choice -eq 'v') { continue }
    if ($choice -eq 'c') {
        $n = Read-Host "要清除哪一项的编号"
        $nn = 0
        if (-not [int]::TryParse($n, [ref]$nn)) { Write-Host "编号无效" -ForegroundColor Red; Start-Sleep 1; continue }
        $f = $flat | Where-Object No -eq $nn
        if (-not $f) { Write-Host "编号无效" -ForegroundColor Red; Start-Sleep 1; continue }
        if ($f.Group -eq '后端') { Set-Backend $f.Name '' } else { Set-Frontend $f.Name '' }
        Write-Host "已清除 $($f.Name)" -ForegroundColor Green
        Start-Sleep 1
        continue
    }
    $no = 0
    if (-not [int]::TryParse($choice, [ref]$no)) { Write-Host "看不懂这个输入" -ForegroundColor Red; Start-Sleep 1; continue }
    $f = $flat | Where-Object No -eq $no
    if (-not $f) { Write-Host "编号无效" -ForegroundColor Red; Start-Sleep 1; continue }

    Write-Host ""
    Write-Host ("修改: {0}  ({1})" -f $f.Name, $f.Label) -ForegroundColor Cyan
    $cur = if ($f.Group -eq '后端') { Get-Backend $f.Name } else { Get-Frontend $f.Name }
    $curShow = if ($f.Secret) { Mask $cur } else { $cur }
    Write-Host ("当前: {0}" -f $(if($curShow){$curShow}else{'(未设置)'})) -ForegroundColor DarkGray

    if ($f.Secret) {
        Write-Host "(输入时屏幕不显示;直接回车=不改)" -ForegroundColor DarkGray
        $ss = Read-Host "新值" -AsSecureString
        $plain = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($ss))
    } else {
        $plain = Read-Host "新值(直接回车=不改)"
    }

    if ([string]::IsNullOrWhiteSpace($plain)) {
        Write-Host "未输入,已跳过" -ForegroundColor DarkYellow
    } else {
        if ($f.Group -eq '后端') { Set-Backend $f.Name $plain } else { Set-Frontend $f.Name $plain }
        Write-Host "已保存 $($f.Name)" -ForegroundColor Green
        if ($f.Group -eq '后端') {
            Write-Host "⚠ 已写入 Windows 用户环境变量,需【重新打开终端】再启动后端才生效。" -ForegroundColor Yellow
        } else {
            Write-Host "⚠ 已写入 .env.local,需【重启 Vite】(Ctrl+C 再 npm run dev)才生效。" -ForegroundColor Yellow
        }
    }
    Start-Sleep -Seconds 2
}
Write-Host "再见。别忘了:后端改动要重开终端,前端改动要重启 Vite。" -ForegroundColor Cyan
