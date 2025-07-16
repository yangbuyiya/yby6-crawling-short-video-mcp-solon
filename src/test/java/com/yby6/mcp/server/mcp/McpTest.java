package com.yby6.mcp.server.mcp;

import org.junit.jupiter.api.Test;
import org.noear.solon.ai.chat.tool.FunctionTool;
import org.noear.solon.ai.mcp.client.McpClientProperties;
import org.noear.solon.ai.mcp.client.McpClientProvider;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;


public class McpTest {
    
    @Test
    public void getSupportedPlatformsTest() throws Exception {
        McpClientProvider mcpClient = new McpClientProvider("http://localhost:8080/mcp/sse");
        Collection<FunctionTool> tools = mcpClient.getTools();
        for (FunctionTool tool : tools) {
            System.out.println(tool.name());
            System.out.println(tool.description());
            System.out.println("===========================");
        }
    }
    
    /**
     * MCP 调用测试
     *
     * @throws Exception 例外
     */
    @Test
    public void shareUrlParseTest() throws Exception {
        McpClientProperties mcpClientProperties = new McpClientProperties();
        mcpClientProperties.setApiUrl("http://localhost:8080/mcp/sse");
        mcpClientProperties.setRequestTimeout(Duration.ofMinutes(5));
        
        // 调用获取视频
        McpClientProvider mcpClient = new McpClientProvider(mcpClientProperties);
        String rst = mcpClient.callToolAsText("share_url_parse_tool", Map.of("shareUrl", "0.76 05/02 fBT:/ V@l.PK 【艾德宝陪您新说唱第五期上（1）】Vinz-t真的无敌了，又来一首爆单啊这是， 迪椰果也是稳稳接住# 新说唱2025 # vinzt # 音乐就要这么玩  https://v.douyin.com/7nF11zmcGpc/ 复制此链接，打开Dou音搜索，直接观看视频！"))
                .getContent();
        System.out.println(rst);
        System.out.println("=======================================");
        // 测试转文本
        String content = mcpClient.callToolAsText("share_text_parse_tool", Map.of(
                "shareText", "0.76 05/02 fBT:/ V@l.PK 【艾德宝陪您新说唱第五期上（1）】Vinz-t真的无敌了，又来一首爆单啊这是， 迪椰果也是稳稳接住# 新说唱2025 # vinzt # 音乐就要这么玩  https://v.douyin.com/7nF11zmcGpc/ 复制此链接，打开Dou音搜索，直接观看视频！",
                "apiKey", "前往获取API：https://cloud.siliconflow.cn/i/tbvUltCF"
        )).getContent();
        System.out.println(content);
    }
    
}
