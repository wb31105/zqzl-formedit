.PHONY: help install backend frontend clean-backend clean-frontend clean

help:
	@echo "可用命令:"
	@echo "  make install       安装前后端所有依赖"
	@echo "  make backend       启动后端服务 (端口 8080)"
	@echo "  make frontend      启动前端服务 (端口 3000)"
	@echo "  make clean-backend 清理后端构建产物"
	@echo "  make clean-frontend 清理前端构建产物"
	@echo "  make clean         清理所有构建产物"

install:
	@echo "===== 安装后端依赖 ====="
	cd backend && mvn dependency:resolve
	@echo ""
	@echo "===== 安装前端依赖 ====="
	cd frontend && npm install

backend:
	@echo "===== 启动后端服务 (http://localhost:8080) ====="
	cd backend && mvn spring-boot:run

frontend:
	@echo "===== 启动前端服务 (http://localhost:3000) ====="
	cd frontend && npm start

clean-backend:
	@echo "===== 清理后端构建产物 ====="
	cd backend && mvn clean

clean-frontend:
	@echo "===== 清理前端构建产物 ====="
	cd frontend && rm -rf build node_modules

clean: clean-backend clean-frontend
	@echo "===== 清理完成 ====="
