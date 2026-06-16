# BW 流程表单台 (flowform)

基于 Spring Boot + React 的前后端分离表单和工作流编辑器应用。品牌名：BW 流程表单台，工程名 flowform。

## 环境要求

| 组件 | 版本要求 | 说明 |
|------|----------|------|
| JDK | 11+ | 后端运行环境 |
| Maven | 3.6+ | 后端构建工具 |
| Node.js | 14+ | 前端运行环境（推荐 16.x / 18.x） |
| npm | 6+ | 随 Node.js 安装 |

## 项目结构

```
zqzl-formedit/
├── backend/                 # 后端 Spring Boot 项目（flowform-backend）
│   ├── src/
│   │   └── main/
│   │       ├── java/        # Java 源代码，包名 com.bw.flowform
│   │       └── resources/   # 配置文件（application.yml）
│   └── pom.xml
├── frontend/                # 前端 React 项目（flowform-frontend）
│   ├── public/
│   ├── src/
│   ├── package.json
│   └── .env.example         # 环境变量模板（复制为 .env 使用）
├── .gitignore
├── Makefile
└── README.md
```

## 快速开始

### 方式一：使用 Makefile（推荐）

```bash
# 1. 安装所有依赖
make install

# 2. 启动后端（另开一个终端）
make backend

# 3. 启动前端（另开一个终端）
make frontend
```

### 方式二：手动分步启动

#### 1. 启动后端服务

```bash
cd backend
mvn spring-boot:run
```

后端启动后访问：http://localhost:8080

#### 2. 启动前端服务

```bash
# 安装依赖（首次运行）
cd frontend
npm install

# 配置环境变量
cp .env.example .env

# 启动开发服务器
npm start
```

前端启动后访问：http://localhost:3000

前端已配置代理，API 请求会自动转发到 `http://localhost:8080`。

## 环境变量说明

前端使用 `.env` 文件管理环境变量。**`.env` 文件包含敏感配置，禁止提交到 Git**。

复制模板文件进行配置：

```bash
cd frontend
cp .env.example .env
```

可用变量：

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `REACT_APP_API_BASE_URL` | 后端 API 地址 | `http://localhost:8080` |
| `PORT` | 前端开发服务器端口 | `3000` |

## 常用命令

### 后端

```bash
cd backend

# 启动开发服务
mvn spring-boot:run

# 打包
mvn clean package

# 跳过测试打包
mvn clean package -DskipTests
```

### 前端

```bash
cd frontend

# 安装依赖
npm install

# 启动开发服务器
npm start

# 构建生产版本
npm run build

# 运行测试
npm test
```

### Makefile 命令

```bash
# 查看所有可用命令
make help

# 安装前后端所有依赖
make install

# 启动后端
make backend

# 启动前端
make frontend

# 清理后端构建产物
make clean-backend

# 清理前端构建产物
make clean-frontend

# 清理所有构建产物
make clean
```

## 访问地址

| 服务 | 地址 |
|------|------|
| 前端 | http://localhost:3000 |
| 后端 API | http://localhost:8080 |

## 注意事项

1. **不要提交 `.env` 文件**：环境变量文件已加入 `.gitignore`，请使用 `.env.example` 作为模板
2. **前端依赖安装慢**：可配置 npm 镜像源 `npm config set registry https://registry.npmmirror.com`
3. **后端首次启动**：会自动初始化 H2 数据库（flowformdb），无需手动建表
