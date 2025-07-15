# 抖音无水印视频下载链接提取 MCP 服务器 (Java版)

基于 Solon 框架的 Model Context Protocol (MCP) 服务器，可以从抖音分享链接获取无水印视频下载链接，并支持从视频中提取文本内容。

## 🎯 功能特性

- 🔗 从抖音分享链接获取无水印视频下载链接
- 🎤 从抖音视频中提取文本内容（语音识别）
- 📱 模拟移动端访问，绕过水印限制
- 🎯 解析视频基本信息（ID、标题、下载地址）
- 🎬 自动下载视频并提取音频
- 🗣️ 集成语音识别API（SiliconFlow）
- 🧹 自动清理临时文件
- 🛠️ 基于 Solon AI MCP 框架实现
- ⚡ 高性能异步处理
- 🔧 支持 Claude Desktop 集成

## 🏗️ 技术架构

- **框架**: Solon 3.4.0
- **Java版本**: 17+
- **MCP协议**: solon-ai-mcp
- **HTTP客户端**: solon-net-httputils + OkHttp3
- **音视频处理**: JAVE2 (FFmpeg Java包装器)
- **文件工具**: Hutool
- **JSON处理**: Jackson
- **构建工具**: Maven

## 📦 项目结构

```
src/main/java/com/yby6/mcp/server/
├── App.java                    # 应用启动类
├── DemoController.java         # 测试控制器
├── model/
│   └── DouyinVideoInfo.java   # 视频信息模型
├── processor/
│   └── DouyinProcessor.java   # 抖音处理器核心类
└── tools/
    └── DouyinMcpTools.java    # MCP工具类
```

## 🚀 快速开始

### 1. 环境要求

- Java 17+
- Maven 3.6+

### 2. 编译运行

```bash
# 克隆项目
cd /Users/yangbuyi/Documents/projectDemo/ai/yby6-crawling-short-video-mcp-solon

# 编译项目
mvn clean package

# 运行服务
mvn solon:run
# 或者
java -jar target/yby6-crawling-short-video-mcp-solon.jar
```

### 3. 测试接口

服务启动后，可以通过以下接口测试功能：

```bash
# 测试获取下载链接
curl "http://localhost:8080/test/douyin?shareLink=https://v.douyin.com/xxx"

# 测试解析视频信息
curl "http://localhost:8080/test/douyin/info?shareLink=https://v.douyin.com/xxx"

# 测试文本提取功能（需要API密钥）
curl "http://localhost:8080/test/douyin/text?shareLink=https://v.douyin.com/xxx&apiKey=your-api-key"

# 获取使用指南
curl "http://localhost:8080/guide/douyin"

# 健康检查
curl "http://localhost:8080/hello"
```

## 🛠️ MCP 工具

项目提供了四个 MCP 工具：

### 1. `get_douyin_download_link`

获取抖音视频的无水印下载链接

**参数：**
- `shareLink`: 抖音分享链接或包含链接的文本

**返回：**
```json
{
  "status": "success",
  "video_id": "7xxx",
  "title": "视频标题",
  "download_url": "https://...",
  "description": "视频标题: xxx",
  "usage_tip": "可以直接使用此链接下载无水印视频"
}
```

### 2. `parse_douyin_video_info`

解析抖音分享链接，获取视频基本信息

**参数：**
- `shareLink`: 抖音分享链接

**返回：**
```json
{
  "video_id": "7xxx",
  "title": "视频标题",
  "download_url": "https://...",
  "status": "success"
}
```

### 3. `extract_douyin_text` ⭐

从抖音分享链接提取视频中的文本内容（语音识别）

**参数：**
- `shareLink`: 抖音分享链接或包含链接的文本（必需）
- `apiKey`: 语音识别API密钥（可选，如不提供则从环境变量 `DOUYIN_API_KEY` 获取）
- `apiBaseUrl`: API基础URL（可选，默认使用SiliconFlow）
- `model`: 语音识别模型（可选，默认使用SenseVoiceSmall）

**返回：**
```json
{
  "status": "success",
  "text_content": "提取的文本内容",
  "message": "文本提取完成"
}
```

**功能流程：**
1. 解析抖音分享链接
2. 下载无水印视频
3. 从视频中提取音频
4. 调用语音识别API提取文本
5. 自动清理临时文件

### 4. `douyin_text_extraction_guide`

获取抖音视频文本提取功能的使用指南

**参数：** 无

**返回：** 详细的使用指南文档

## 🔧 Claude Desktop 配置

在 `claude_desktop_config.json` 中添加：

### 基础配置（仅支持视频下载链接获取）

```json
{
  "mcpServers": {
    "douyin-java-mcp": {
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
    "douyin-java-mcp": {
      "command": "java",
      "args": [
        "-jar",
        "/path/to/yby6-crawling-short-video-mcp-solon.jar"
      ],
      "env": {
        "DOUYIN_API_KEY": "your-siliconflow-api-key-here"
      }
    }
  }
}
```

### 环境变量说明

- `DOUYIN_API_KEY`: SiliconFlow 或其他兼容OpenAI格式的语音识别API密钥
  - 获取地址：https://cloud.siliconflow.cn/
  - 用于调用语音识别服务，将视频音频转换为文本

## 📝 核心实现

### DouyinProcessor 核心逻辑

1. **链接解析**: 使用正则表达式提取分享链接
2. **重定向处理**: 获取真实的视频页面URL
3. **页面解析**: 提取页面中的 `window._ROUTER_DATA`
4. **JSON解析**: 使用Jackson解析视频信息
5. **链接转换**: 将有水印链接转换为无水印链接

### 关键代码片段

```java
// 解析视频信息的核心逻辑
public DouyinVideoInfo parseShareUrl(String shareText) throws Exception {
    // 1. 提取分享链接
    String shareUrl = extractShareUrl(shareText);
    
    // 2. 获取重定向后的真实链接
    String realUrl = getRealVideoUrl(shareUrl);
    
    // 3. 提取视频ID
    String videoId = extractVideoId(realUrl);
    
    // 4. 获取视频页面内容并解析
    String pageContent = getPageContent(standardShareUrl);
    return parseVideoInfo(pageContent, videoId);
}
```

## 🔍 错误处理

项目包含完善的错误处理机制：

- **网络异常**: 自动重试和优雅降级
- **解析失败**: 详细的错误信息返回
- **参数验证**: 输入参数的合法性检查
- **日志记录**: 完整的操作日志记录

## 📊 性能特点

- **内存优化**: 流式处理，避免大文件加载
- **连接复用**: HTTP连接池管理
- **异步处理**: 非阻塞操作提升性能
- **缓存机制**: 减少重复请求

## 🛡️ 安全考虑

- **请求头伪造**: 模拟真实移动设备访问
- **频率限制**: 避免过度请求被封禁
- **异常处理**: 避免敏感信息泄露
- **输入验证**: 防止恶意输入攻击

## ⚠️ 使用须知

1. **合法使用**: 请遵守相关法律法规，仅用于个人学习研究
2. **版权尊重**: 不得用于侵犯他人知识产权的行为
3. **频率控制**: 避免过于频繁的请求，防止被限制访问
4. **免责声明**: 使用者需自行承担使用风险和责任

## 🔨 开发相关

### 本地开发

```bash
# 开发模式运行
mvn solon:run

# 代码检查
mvn checkstyle:check

# 单元测试
mvn test
```

### 自定义配置

可以通过修改 `application.yml` 来自定义配置：

- 服务端口
- 日志级别
- HTTP超时时间
- MCP工具包扫描路径

## 📄 许可证

MIT License

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

---

**注意**: 本项目仅供学习交流使用，请遵守相关法律法规。 