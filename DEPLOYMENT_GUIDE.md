# 🚀 抢老师系统 - 部署指南

## 📋 配置合并完成

✅ **已完成的工作：**
1. 将所有 application 配置合并到统一的 `application.properties` 文件
2. 使用环境变量替换所有硬编码配置
3. 创建了开发和生产环境的环境变量文件
4. 提供了启动脚本和环境变量加载工具
5. 添加了 Docker 支持配置

## 🗂️ 文件结构

```
grabTeacher/
├── grabTeacher-backend/
│   ├── src/main/resources/
│   │   └── application.properties          # 统一配置文件
│   ├── .env.example                        # 环境变量示例
│   ├── .env.dev                           # 开发环境变量
│   ├── .env.prod                          # 生产环境变量
│   ├── .env.docker                        # Docker环境变量
│   ├── scripts/
│   │   ├── load-env.sh                    # 环境变量加载脚本
│   │   ├── run-dev.sh                     # 开发环境启动脚本
│   │   └── run-prod.sh                    # 生产环境启动脚本
│   ├── CONFIG_README.md                   # 配置说明文档
│   └── ENVIRONMENT_VARIABLES.md           # 环境变量数据
├── grabTeacher-frontend/
│   ├── .env.development                   # 前端开发环境
│   └── .env.production                    # 前端生产环境
└── docker-compose.example.yml            # Docker Compose示例
```

## 🎯 环境变量数据汇总

### 🗄️ 数据库配置
```bash
# 开发环境
DB_URL=jdbc:mysql://localhost:3306/grabTeacher?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
DB_USERNAME=root
DB_PASSWORD=qhy000916

# 生产环境
DB_URL=jdbc:mysql://127.0.0.1/grabteacher?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=true&serverTimezone=GMT%2B8
DB_USERNAME=root
DB_PASSWORD=your-prod-password
```

### 🔐 JWT 配置
```bash
# 开发环境
JWT_SECRET=mySecretKey123456789012345678901234567890123456789012345678901234567890
JWT_EXPIRATION=86400000

# 生产环境（请使用更安全的密钥）
JWT_SECRET=myProductionSecretKey123456789012345678901234567890123456789012345678901234567890
JWT_EXPIRATION=86400000
```

### 🌐 CORS 配置
```bash
# 开发环境
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173,http://127.0.0.1:3000,http://127.0.0.1:5173

# 生产环境
CORS_ALLOWED_ORIGINS=http://grabteacher.ltd,http://www.grabteacher.ltd
```

### 🎨 前端配置
```bash
# 开发环境
VITE_API_BASE_URL=http://localhost:8080
VITE_APP_TITLE=抢老师系统 - 开发环境

# 生产环境
VITE_API_BASE_URL=http://grabteacher.ltd
VITE_APP_TITLE=抢老师系统
```

## 🚀 快速启动

### 1. 开发环境
```bash
cd grabTeacher-backend
./scripts/run-dev.sh
```

### 2. 生产环境
```bash
cd grabTeacher-backend
# 修改 .env.prod 中的生产环境配置
./scripts/run-prod.sh
```

### 3. Docker 部署
```bash
# 复制并修改 Docker 配置
cp docker-compose.example.yml docker-compose.yml
# 修改配置后启动
docker-compose up -d
```

## 🔧 配置自定义

### 1. 修改数据库配置
编辑对应环境的 `.env` 文件：
```bash
# 修改开发环境
vim grabTeacher-backend/.env.dev

# 修改生产环境
vim grabTeacher-backend/.env.prod
```

### 2. 生成安全的JWT密钥
```bash
# 生成64字符的安全密钥
openssl rand -base64 64
```

### 3. 前端API地址配置
```bash
# 开发环境
echo "VITE_API_BASE_URL=http://localhost:8080" > grabTeacher-frontend/.env.local

# 生产环境
echo "VITE_API_BASE_URL=https://your-api-domain.com" > grabTeacher-frontend/.env.local
```

## 🔒 安全检查清单

- [ ] 生产环境使用强数据库密码
- [ ] JWT密钥至少64字符且随机生成
- [ ] CORS配置明确指定允许的域名
- [ ] 敏感的 `.env` 文件不提交到版本控制
- [ ] 生产环境关闭SQL日志和调试信息
- [ ] 定期轮换密钥和密码

## 📝 注意事项

1. **环境变量优先级**: 环境变量 > 配置文件默认值
2. **文件安全**: `.env.prod` 包含敏感信息，请妥善保管
3. **数据库初始化**: 首次部署需要执行数据库初始化脚本
4. **端口冲突**: 确保配置的端口未被占用
5. **网络访问**: 生产环境需要配置防火墙和网络安全组

## 🆘 故障排除

### 常见问题
1. **环境变量未生效**: 确保使用 `source` 命令加载
2. **数据库连接失败**: 检查数据库服务状态和连接参数
3. **JWT错误**: 确认密钥长度和格式正确
4. **CORS错误**: 检查前端域名是否在允许列表中

### 调试命令
```bash
# 检查环境变量
source scripts/load-env.sh dev
env | grep -E "(DB_|JWT_|CORS_)"

# 测试数据库连接
mysql -h localhost -u root -p grabTeacher

# 查看应用日志
tail -f logs/application.log
```

## 📞 技术支持

如遇到问题，请检查：
1. 环境变量配置是否正确
2. 数据库服务是否正常
3. 网络连接是否畅通
4. 日志文件中的错误信息

配置完成后，系统将更加安全、灵活且易于部署！
