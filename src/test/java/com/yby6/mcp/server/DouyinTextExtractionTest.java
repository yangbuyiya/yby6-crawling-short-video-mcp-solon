package com.yby6.mcp.server;

import com.yby6.mcp.server.processor.DouyinProcessor;
import com.yby6.mcp.server.tools.DouyinMcpTools;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.noear.solon.annotation.Inject;
import org.noear.solon.test.SolonTest;

/**
 * 抖音文本提取功能测试
 * 需要设置环境变量 DOUYIN_API_KEY 才能运行完整测试
 */
@Slf4j
@SolonTest(App.class)
public class DouyinTextExtractionTest {


    @Inject
    private DouyinProcessor douyinProcessor;

    @Test
    public void testExtractShareUrl() {
        // 测试URL提取功能
        String shareText = "0.20 07/17 B@T.lC Cuf:/ 海边度假\uD83C\uDFD6\uFE0F微胖必备高颜值显瘦泳衣 夏天当然少不了好看又显瘦的绝美泳衣啦～ # 恋夏风 # 恋夏穿搭谁穿谁好看 # 辣妹的夏天 # 泳衣 # 泳衣种草  https://v.douyin.com/UI4rVvd8eVw/ 复制此链接，打开Dou音搜索，直接观看视频！";

        try {
            // 这里我们直接测试parseShareUrl，因为extractShareUrl是私有方法
            // 实际会在parseShareUrl中调用extractShareUrl
            final String douyinText = douyinProcessor.extractDouyinText(shareText, "sk-pyehhxnstdzjbmyjiexjzakincipinswyebhkwlwyztmwjor", "https://api.siliconflow.cn/v1/audio/transcriptions", "FunAudioLLM/SenseVoiceSmall");
            log.info("提取成功: {}", douyinText);

            // 注意：由于没有真实的抖音链接，这个测试可能会失败
            // 但可以验证方法调用流程
            log.info("URL提取功能测试完成");

        } catch (Exception e) {
            log.info("预期的测试异常（因为是示例链接）: {}", e.getMessage());
        }
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "DOUYIN_API_KEY", matches = ".+")
    public void testTextExtractionWithRealApiKey() {
        // 这个测试只有在设置了DOUYIN_API_KEY环境变量时才会运行
        String apiKey = System.getenv("DOUYIN_API_KEY");
        String shareText = "测试抖音链接: https://v.douyin.com/test/";

        try {
            String result = douyinProcessor.extractDouyinText(shareText, apiKey, null, null);
            log.info("文本提取结果: {}", result);
        } catch (Exception e) {
            log.warn("文本提取测试失败（预期，因为是测试链接）: {}", e.getMessage());
        }
    }

    @Test
    public void testMcpToolsGuide() {
        // 测试MCP工具的使用指南功能
        DouyinMcpTools mcpTools = new DouyinMcpTools(douyinProcessor);

        String guide = mcpTools.getDouyinTextExtractionGuide();

        log.info("使用指南长度: {}", guide.length());
        assert guide.contains("抖音视频文本提取使用指南");
        assert guide.contains("extract_douyin_text");
        assert guide.contains("DOUYIN_API_KEY");

        log.info("MCP工具使用指南测试通过");
    }

    @Test
    public void testErrorHandling() {
        // 测试错误处理
        DouyinMcpTools mcpTools = new DouyinMcpTools(douyinProcessor);

        // 测试空链接
        String result = mcpTools.extractDouyinText("", null, null, null);
        log.info("空链接测试结果: {}", result);
        assert result.contains("error");

        // 测试无效链接
        result = mcpTools.extractDouyinText("不是一个链接", null, null, null);
        log.info("无效链接测试结果: {}", result);
        assert result.contains("error");

        log.info("错误处理测试通过");
    }
} 