# DevPilot 阿里云部署指南

本文档用于把 DevPilot 部署到阿里云轻量应用服务器。当前服务器公网 IP：`47.107.151.225`。

## 1. 服务器准备

推荐配置：

- Ubuntu 24.04
- 2 vCPU / 2 GiB 内存 / 40 GiB 系统盘
- 安全组或防火墙仅开放 `22`、`80`
- 后续绑定域名并配置 HTTPS 时再开放 `443`

不要对公网开放：

- `5432` PostgreSQL
- `8080` Spring Boot 后端

## 2. 安装 Docker

使用阿里云控制台“远程连接”登录服务器后执行：

```bash
apt update
apt install -y ca-certificates curl gnupg
install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
chmod a+r /etc/apt/keyrings/docker.gpg
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" \
  > /etc/apt/sources.list.d/docker.list
apt update
apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
docker --version
docker compose version
```

## 3. 配置 Swap

2 GiB 内存机器首次构建 Maven / npm 镜像时可能内存不足，建议配置 2 GiB swap：

```bash
fallocate -l 2G /swapfile
chmod 600 /swapfile
mkswap /swapfile
swapon /swapfile
echo '/swapfile none swap sw 0 0' >> /etc/fstab
free -h
```

## 4. 克隆项目

先在 GitHub 创建空仓库并推送代码，然后服务器执行：

```bash
cd /opt
git clone https://github.com/<你的用户名>/devpilot.git
cd devpilot
```

## 5. 配置环境变量

```bash
cp .env.example .env
nano .env
```

必须修改：

```dotenv
POSTGRES_PASSWORD=数据库强密码
DB_PASSWORD=数据库强密码
JWT_SECRET=至少32位随机字符串
ADMIN_PASSWORD=管理员强密码
CORS_ALLOWED_ORIGINS=http://47.107.151.225
```

说明：

- `POSTGRES_PASSWORD` 和 `DB_PASSWORD` 必须保持一致。
- `ADMIN_PASSWORD` 只给维护者使用，不写入 README。
- `demo / DevPilotDemo2026!` 是公开只读演示账号，可按需修改 `DEMO_PASSWORD`。

## 6. 启动服务

```bash
docker compose up -d --build
```

查看状态：

```bash
docker compose ps
docker compose logs backend --tail=100
```

## 7. 验证

后端健康检查：

```bash
curl http://localhost:8080/actuator/health
```

演示账号登录：

```bash
curl http://localhost/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"demo","password":"DevPilotDemo2026!"}'
```

浏览器访问：

```text
http://47.107.151.225
```

## 8. 常用运维命令

更新部署：

```bash
cd /opt/devpilot
git pull
docker compose up -d --build
```

查看日志：

```bash
docker compose logs -f backend
docker compose logs -f frontend
docker compose logs -f postgres
```

重启：

```bash
docker compose restart
```

停止：

```bash
docker compose down
```

注意：`docker compose down` 不会删除数据库卷。不要执行 `docker compose down -v`，除非确认要清空数据库。

## 9. 后续优化

- 绑定域名后完成 ICP 备案。
- 配置 HTTPS，并开放 `443`。
- 定期备份 PostgreSQL 数据卷。
- 将 Docker 镜像构建切到 GitHub Actions，降低服务器构建压力。
