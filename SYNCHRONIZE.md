# 同步本地代码到 GitHub dev-chuan123123456 分支

## 第一步：配置 Git 用户信息（如果尚未配置）
```bash
git config --global user.name "你的GitHub用户名"
git config --global user.email "你的GitHub邮箱"
```

## 第二步：初始化并提交代码
```bash
# 进入项目目录
cd "c:\Users\ROG\OneDrive\桌面\软件工程\EBU6304-TA-Recruitment-System-main\EBU6304-TA-Recruitment-System-main"

# 添加远程仓库
git remote add origin https://github.com/Jhon0213/EBU6304-TA-Recruitment-System.git

# 添加所有项目文件（排除 target 等编译文件）
git add src/ data/ docs/ pom.xml README.md .gitignore

# 创建提交
git commit -m "Initial commit - TA Recruitment System"

# 推送到远程 dev-chuan123123456 分支
git push -u origin master:dev-chuan123123456
```

## 如果远程分支已存在
```bash
# 先获取远程分支信息
git fetch origin

# 创建本地分支并跟踪远程分支
git checkout -b dev-chuan123123456 origin/dev-chuan123123456

# 合并本地更改
git merge master

# 推送合并结果
git push origin dev-chuan123123456
```

## 如果遇到权限问题
1. 确保你已经配置了 GitHub 个人访问令牌 (Personal Access Token)
2. 使用令牌认证：
   ```bash
   git remote set-url origin https://你的令牌@github.com/Jhon0213/EBU6304-TA-Recruitment-System.git
   ```

## 验证推送结果
推送成功后，访问以下链接查看：
https://github.com/Jhon0213/EBU6304-TA-Recruitment-System/tree/dev-chuan123123456
