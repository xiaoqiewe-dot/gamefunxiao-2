@echo off
chcp 65001 >nul
setlocal

cd /d "%~dp0"
title GameFunXiao 插件文档站

echo.
echo ================================
echo   GameFunXiao 插件文档站启动器
echo ================================
echo.

where node >nul 2>nul
if errorlevel 1 (
    echo [错误] 没有找到 Node.js，请先安装 Node.js 后再启动。
    echo.
    pause
    exit /b 1
)

where npm >nul 2>nul
if errorlevel 1 (
    echo [错误] 没有找到 npm，请检查 Node.js 是否安装完整。
    echo.
    pause
    exit /b 1
)

if not exist "node_modules\" (
    echo [准备] 第一次启动，正在安装依赖...
    call npm install
    if errorlevel 1 (
        echo.
        echo [错误] npm install 失败，请检查网络或 npm 配置。
        pause
        exit /b 1
    )
)

echo [启动] 正在打开浏览器：http://localhost:80
start "" "http://localhost:80"

echo [启动] 正在启动本地文档服务器...
echo [提示] 关闭这个窗口会停止文档站。
echo.
call npm run dev -- --host 0.0.0.0 --port 80

echo.
echo [结束] 文档站已停止。
pause
