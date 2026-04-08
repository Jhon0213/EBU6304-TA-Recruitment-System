# TA Recruitment System - 同步到 GitHub 脚本

# 脚本说明：
# 此脚本将帮助你将本地代码同步到 GitHub 的 dev-chuan123123456 分支

# 配置 GitHub 用户信息（请替换为你的信息）
$gitUserName = "Jhon0213"
$gitEmail = "your-email@example.com"  # 请替换为你的 GitHub 邮箱

# 项目路径
$projectPath = "C:\Users\ROG\OneDrive\桌面\软件工程\EBU6304-TA-Recruitment-System-main\EBU6304-TA-Recruitment-System-main"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "TA Recruitment System - GitHub 同步脚本" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# 进入项目目录
Set-Location $projectPath

# 配置 Git 用户信息
Write-Host "`n[1/6] 配置 Git 用户信息..." -ForegroundColor Yellow
git config user.name $gitUserName
git config user.email $gitEmail

# 检查远程仓库
Write-Host "`n[2/6] 检查远程仓库配置..." -ForegroundColor Yellow
$remoteCheck = git remote -v
if ($remoteCheck -eq $null) {
    Write-Host "添加远程仓库..." -ForegroundColor Green
    git remote add origin https://github.com/Jhon0213/EBU6304-TA-Recruitment-System.git
} else {
    Write-Host "远程仓库已存在: $remoteCheck" -ForegroundColor Green
}

# 添加文件到暂存区
Write-Host "`n[3/6] 添加项目文件到暂存区..." -ForegroundColor Yellow
git add src/ data/ docs/ pom.xml README.md .gitignore

# 检查是否有文件被添加
$status = git status --short
if ($status -eq $null -or $status.Length -eq 0) {
    Write-Host "没有新文件需要添加" -ForegroundColor Green
} else {
    Write-Host "已添加以下文件:" -ForegroundColor Green
    git status --short
}

# 创建提交
Write-Host "`n[4/6] 创建提交..." -ForegroundColor Yellow
$commitMessage = "feat: Initial commit - TA Recruitment System with full implementation"
git commit -m $commitMessage

# 推送代码
Write-Host "`n[5/6] 推送到 GitHub..." -ForegroundColor Yellow
Write-Host "正在推送到 dev-chuan123123456 分支..." -ForegroundColor Green

try {
    git push -u origin master:dev-chuan123123456
    Write-Host "`n[6/6] 推送成功!" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "代码已成功同步到 GitHub!" -ForegroundColor Cyan
    Write-Host "查看地址: https://github.com/Jhon0213/EBU6304-TA-Recruitment-System/tree/dev-chuan123123456" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
} catch {
    Write-Host "`n推送失败! 请检查:" -ForegroundColor Red
    Write-Host "1. 网络连接是否正常" -ForegroundColor Red
    Write-Host "2. GitHub 仓库地址是否正确" -ForegroundColor Red
    Write-Host "3. 是否拥有该仓库的推送权限" -ForegroundColor Red
}

Write-Host "`n按任意键退出..." -ForegroundColor Gray
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
