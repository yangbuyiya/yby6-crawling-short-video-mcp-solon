# 短视频/图集提取 MCP 服务器

基于 Solon 框架的 Model Context Protocol (MCP) 服务器，支持从多个短视频平台获取无水印视频下载链接，并支持从视频中提取文本内容。

## 🎯 功能特性

- 🔗 从分享链接获取无水印视频/图集下载链接
- 🎤 从视频中提取文本内容（语音识别）
- 📱 模拟移动端访问，绕过水印限制
- 🎯 解析视频基本信息（ID、标题、下载地址、作者信息）
- 🖼️ 支持图集解析（小红书图文内容）
- 🎬 自动下载视频并提取音频
- 🗣️ 集成语音识别API（SiliconFlow）
- 🧹 自动清理临时文件
- 🛠️ 基于 Solon AI MCP 框架实现
- ⚡ 高性能异步处理
- 🔧 支持 Claude Desktop 集成

## 📱 支持平台

### ✅ 已支持平台

| 平台 | 支持状态 | 功能 |
|------|---------|------|
| 🎵 抖音 | ✅ 完整支持 | 视频解析、文本提取 |
| 📝 小红书 | ✅ 完整支持 | 视频/图集解析 |

### 🚧 即将支持平台

| 平台 | 开发状态 | 预计上线 |
|------|---------|----------|
| 🚀 快手 | 🔨 开发中 | v2.0 |
| 📺 微博视频 | 📋 计划中 | v2.1 |
| 🎪 皮皮虾 | 📋 计划中 | v2.1 |
| 🎭 微视 | 📋 计划中 | v2.2 |
| 🍉 西瓜视频 | 📋 计划中 | v2.2 |
| 🎬 更多平台 | 📋 规划中 | 持续更新 |

> 💡 **提示**: 当前版本专注于抖音和小红书的稳定支持，其他平台正在积极开发中！

## 🏗️ 技术架构

- **框架**: Solon 3.4.0
- **Java版本**: 17+
- **MCP协议**: solon-ai-mcp
- **HTTP客户端**: solon-net-httputils + OkHttp3
- **音视频处理**: JAVE2 (FFmpeg Java包装器)
- **文件工具**: Hutool
- **JSON处理**: Jackson
- **构建工具**: Maven

## 🚀 快速开始

### 1. 环境要求

- Java 17+
- Maven 3.6+

### 2. 编译运行

```bash
# 克隆项目
cd /f:/knowledge/yby6-study/yby6-crawling-short-video-mcp-solon

# 编译项目
mvn clean package

# 运行服务
mvn solon:run
# 或者
java -jar target/yby6-crawling-short-video-mcp-solon.jar
```

## 🛠️ MCP 工具

项目提供了四个核心 MCP 工具：

### 1. `share_url_parse_tool` ⭐

解析视频分享链接，获取视频信息

**参数：**
- `shareUrl`: 视频分享链接或包含链接的文本

**返回：**
```json
{
  "code": 200,
  "msg": "解析成功",
  "data": {
    "videoUrl": "https://...",
    "coverUrl": "https://...",
    "title": "视频标题",
    "author": {
      "uid": "用户ID",
      "name": "用户昵称",
      "avatar": "头像URL"
    },
    "images": [
      {
        "url": "图片URL",
        "livePhotoUrl": "实况照片URL（如果有）"
      }
    ],
    "status": "success",
    "description": "视频描述",
    "usageTip": "使用提示"
  }
}
```

### 2. `video_id_parse_tool`

根据平台和视频ID解析视频信息

**参数：**
- `source`: 视频来源平台 (douyin/redbook)
- `videoId`: 视频ID

**返回：** 同上格式

### 3. `share_text_parse_tool` ⭐

从视频分享链接提取视频中的文本内容 API：https://cloud.siliconflow.cn/i/tbvUltCF

**参数：**
- `shareUrl`: 视频分享链接或包含链接的文本（必需）
- `apiKey`: 语音识别API密钥（可选，如不提供则从环境变量 `YBY6_API_KEY` 获取）
- `apiBaseUrl`: API基础URL（可选，默认使用SiliconFlow）
- `model`: 语音识别模型（可选，默认使用SenseVoiceSmall）

**返回：**
```json
{
  "code": 200,
  "msg": "提取成功",
  "data": {
    "text_content": "提取的文本内容",
    "video_info": {
      "title": "视频标题",
      "author": "作者信息"
    }
  }
}
```

## 🔧 Claude Desktop 配置

在 `claude_desktop_config.json` 中添加：

### 基础配置（仅支持视频解析）

```json
{
  "mcpServers": {
    "video-mcp-server": {
      "command": "java",
      "args": [
        "-jar",
        "/path/to/yby6-crawling-short-video-mcp-solon.jar"
      ],
      "env": {}
    }
  }
}
```

### 完整配置（支持文本提取功能）

```json
{
  "mcpServers": {
    "video-mcp-server": {
      "command": "java",
      "args": [
        "-jar",
        "/path/to/yby6-crawling-short-video-mcp-solon.jar"
      ],
      "env": {
        "YBY6_API_KEY": "your-siliconflow-api-key-here"
      }
    }
  }
}
```

### 环境变量说明

- `YBY6_API_KEY`: SiliconFlow 或其他兼容OpenAI格式的语音识别API密钥
  - 获取地址：https://cloud.siliconflow.cn/i/tbvUltCF
  - 用于调用语音识别服务，将视频音频转换为文本

### 添加新平台

要添加新平台支持，需要：

1. 在 `VideoSource` 枚举中添加新平台
2. 创建对应的 `Parser` 实现类
3. 在 `ParserFactory` 中注册新解析器
4. 编写对应的单元测试

### 自定义配置

可以通过修改 `app.yml` 来自定义配置：

- 服务端口
- 日志级别
- HTTP超时时间
- MCP工具包扫描路径

## 📄 许可证

MIT License

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

### 贡献指南

1. Fork 本仓库
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开 Pull Request

---

# **注意**: 本项目仅供学习交流使用，请遵守相关法律法规。
# ⚠️ 使用须知

1. **合法使用**: 请遵守相关法律法规，仅用于个人学习研究
2. **版权尊重**: 不得用于侵犯他人知识产权的行为
3. **频率控制**: 避免过于频繁的请求，防止被限制访问
4. **免责声明**: 使用者需自行承担使用风险和责任
5. **平台限制**: 不同平台可能有不同的访问限制和反爬策略
