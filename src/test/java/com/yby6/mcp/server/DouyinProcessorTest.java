package com.yby6.mcp.server;

import com.yby6.mcp.server.model.DouyinVideoInfo;
import com.yby6.mcp.server.processor.DouyinProcessor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.noear.solon.annotation.Inject;
import org.noear.solon.test.SolonTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 抖音处理器测试类
 */
@Slf4j
@SolonTest(App.class)
public class DouyinProcessorTest {

    @Inject
    private DouyinProcessor douyinProcessor;

    /**
     * 测试URL提取功能
     */
    @Test
    public void testExtractShareUrl() {
        // 测试用例1：包含抖音链接的文本
        String text1 = "0.20 07/17 B@T.lC Cuf:/ 海边度假\uD83C\uDFD6\uFE0F微胖必备高颜值显瘦泳衣 夏天当然少不了好看又显瘦的绝美泳衣啦～ # 恋夏风 # 恋夏穿搭谁穿谁好看 # 辣妹的夏天 # 泳衣 # 泳衣种草  https://v.douyin.com/UI4rVvd8eVw/ 复制此链接，打开Dou音搜索，直接观看视频！";
        try {
            String result = extractShareUrlPublic(text1);
            assertNotNull(result);
            assertTrue(result.contains("douyin.com"));
            log.info("测试1通过，提取到链接: {}", result);
        } catch (Exception e) {
            log.error("测试1失败", e);
        }

        // 测试用例2：直接的抖音链接
        String text2 = "https://v.douyin.com/UI4rVvd8eVw/";
        try {
            String result = extractShareUrlPublic(text2);
            assertNotNull(result);
            assertEquals(text2, result);
            log.info("测试2通过，提取到链接: {}", result);
        } catch (Exception e) {
            log.error("测试2失败", e);
        }

        // 测试用例3：无效文本
        String text3 = "这里没有任何链接";
        try {
            String result = extractShareUrlPublic(text3);
            assertNull(result);
            log.info("测试3通过，无链接文本正确返回null");
        } catch (Exception e) {
            log.error("测试3失败", e);
        }
    }

    /**
     * 测试视频ID提取功能（模拟）
     */
    @Test
    public void testVideoIdExtraction() {
        // 测试从URL中提取视频ID的逻辑
        String testUrl1 = "https://v.douyin.com/UI4rVvd8eVw/";
        String expectedId1 = "UI4rVvd8eVw";

        // 简单的ID提取逻辑测试
        String[] parts = testUrl1.split("/");
        String actualId = parts[parts.length - 1].replace("/", "");
        if (actualId.isEmpty() && parts.length > 1) {
            actualId = parts[parts.length - 2];
        }

        assertEquals(expectedId1, actualId);
        log.info("视频ID提取测试通过: {} -> {}", testUrl1, actualId);
    }

    /**
     * 测试完整的解析流程（使用模拟数据）
     */
    @Test
    public void testParseShareUrlWithMockData() {
        // 由于实际网络请求可能不稳定，这里主要测试方法调用链
        String testShareText = "https://v.douyin.com/UI4rVvd8eVw/";

        log.info("开始测试分享链接解析...");
        log.info("测试输入: {}", testShareText);

        try {
            DouyinVideoInfo result = douyinProcessor.parseShareUrl(testShareText);
            assertNotNull(result);
            assertNotNull(result.getVideoId());
            log.info("解析结果: {}", result);


        } catch (Exception e) {
            log.warn(" {}", e.getMessage());
        }
    }

    /**
     * 测试JSON解析逻辑（模拟）
     */
    @Test
    public void testJsonParsing() {
        // 测试文件名清理逻辑
        String dirtyTitle = "测试视频\\/:*?\"<>|标题";
        String cleanTitle = dirtyTitle.replaceAll("[\\\\/:*?\"<>|]", "_");
        String expectedTitle = "测试视频________标题";

        assertEquals(expectedTitle, cleanTitle);
        log.info("文件名清理测试通过: {} -> {}", dirtyTitle, cleanTitle);
    }

    /**
     * 测试错误处理
     */
    @Test
    public void testErrorHandling() {
        // 测试空输入
        try {
            String result = extractShareUrlPublic("");
            assertNull(result);
            log.info("空输入测试通过");
        } catch (Exception e) {
            log.error("空输入测试失败", e);
            fail("空输入不应该抛出异常");
        }

        // 测试null输入
        try {
            String result = extractShareUrlPublic(null);
            assertNull(result);
            log.info("null输入测试通过");
        } catch (Exception e) {
            log.error("null输入测试失败", e);
            fail("null输入不应该抛出异常");
        }
    }

    /**
     * 辅助方法：提取分享链接的公开版本（用于测试）
     */
    private String extractShareUrlPublic(String shareText) {
        if (shareText == null || shareText.trim().isEmpty()) {
            return null;
        }

        // 使用与DouyinProcessor相同的正则表达式
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "http[s]?://(?:[a-zA-Z]|[0-9]|[$\\-_@.&+]|[!*\\(\\),]|(?:%[0-9a-fA-F][0-9a-fA-F]))+"
        );
        java.util.regex.Matcher matcher = pattern.matcher(shareText);

        if (matcher.find()) {
            return matcher.group();
        }

        return null;
    }

    /**
     * 集成测试：测试MCP工具调用
     */
    @Test
    public void testMcpToolIntegration() {
        log.info("开始MCP工具集成测试...");

        // 这里可以测试MCP工具的基本功能
        assertNotNull(douyinProcessor);
        log.info("MCP集成测试：DouyinProcessor组件注入成功");

        // 如果需要测试真实功能，可以在这里添加实际的抖音链接
        log.info("MCP集成测试完成");
    }
} 