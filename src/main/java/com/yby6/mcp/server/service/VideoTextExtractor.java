package com.yby6.mcp.server.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.yby6.mcp.server.utils.JsonUtil;
import com.yby6.mcp.server.model.VideoInfo;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import org.noear.solon.annotation.Component;
import ws.schild.jave.Encoder;
import ws.schild.jave.EncoderException;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * 视频文本提取服务
 * 通用的视频文本提取功能，支持所有平台
 *
 * @author Yangbuyi
 * @date 2025/07/16
 */
@Component
@Slf4j
public class VideoTextExtractor {

    private static final String USER_AGENT = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) EdgiOS/121.0.2277.107 Version/17.0 Mobile/15E148 Safari/604.1";
    
    // 默认API配置
    private static final String DEFAULT_API_BASE_URL = "https://api.siliconflow.cn/v1/audio/transcriptions";
    private static final String DEFAULT_MODEL = "FunAudioLLM/SenseVoiceSmall";


    private final OkHttpClient httpClient;
    private final Path tempDir;

    public VideoTextExtractor() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.MINUTES)
                .readTimeout(10, TimeUnit.MINUTES)
                .writeTimeout(10, TimeUnit.MINUTES)
                .build();

        // 创建临时目录
        try {
            this.tempDir = Files.createTempDirectory("video_text_extractor_" + IdUtil.fastSimpleUUID());
            log.info("创建临时目录: {}", tempDir);
        } catch (IOException e) {
            throw new RuntimeException("无法创建临时目录", e);
        }
    }

    /**
     * 从视频信息中提取文本内容
     *
     * @param videoInfo  视频信息
     * @param apiKey     语音识别API密钥
     * @param apiBaseUrl API基础URL（可选，默认使用SiliconFlow）
     * @param model      语音识别模型（可选，默认使用SenseVoiceSmall）
     * @return 提取的文本内容
     * @throws Exception 处理失败时抛出异常
     */
    public String extractTextFromVideo(VideoInfo videoInfo, String apiKey, String apiBaseUrl, String model) throws Exception {
        if (StringUtils.isBlank(apiKey)) {
            throw new IllegalArgumentException("API密钥不能为空");
        }

        if (videoInfo == null || StringUtils.isBlank(videoInfo.getVideoUrl())) {
            throw new IllegalArgumentException("视频信息无效或缺少视频下载链接");
        }

        try {
            log.info("开始从视频中提取文本内容: {}", videoInfo.getTitle());

            // 1. 下载视频
            log.info("正在下载视频...");
            Path videoPath = downloadVideo(videoInfo);

            try {
                // 2. 提取音频
                log.info("正在提取音频...");
                Path audioPath = extractAudio(videoPath);

                try {
                    // 3. 提取文本
                    log.info("正在从音频中提取文本...");
                    String textContent = extractTextFromAudio(audioPath, apiKey,
                            StringUtils.isNotBlank(apiBaseUrl) ? apiBaseUrl : DEFAULT_API_BASE_URL,
                            StringUtils.isNotBlank(model) ? model : DEFAULT_MODEL);

                    log.info("文本提取完成!");
                    return textContent;

                } finally {
                    // 清理音频文件
                    cleanupFiles(audioPath);
                }

            } finally {
                // 清理视频文件
                cleanupFiles(videoPath);
            }

        } catch (Exception e) {
            log.error("提取视频文本失败", e);
            throw new Exception("提取视频文本失败: " + e.getMessage(), e);
        }
    }

    /**
     * 下载视频到临时目录
     *
     * @param videoInfo 视频信息
     * @return 下载的视频文件路径
     * @throws Exception 下载失败时抛出异常
     */
    private Path downloadVideo(VideoInfo videoInfo) throws Exception {
        String filename = "video_" + System.currentTimeMillis() + ".mp4";
        Path videoPath = tempDir.resolve(filename);

        log.info("正在下载视频: {} -> {}", videoInfo.getTitle(), videoPath);

        Request request = new Request.Builder()
                .url(videoInfo.getVideoUrl())
                .header("User-Agent", USER_AGENT)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("下载视频失败: " + response);
            }

            ResponseBody body = response.body();
            if (body == null) {
                throw new IOException("响应体为空");
            }

            long totalSize = body.contentLength();
            long downloadedSize = 0;

            try (InputStream inputStream = body.byteStream();
                 FileOutputStream outputStream = new FileOutputStream(videoPath.toFile())) {

                byte[] buffer = new byte[8192];
                int bytesRead;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    downloadedSize += bytesRead;

                    if (totalSize > 0) {
                        double progress = (double) downloadedSize / totalSize * 100;
                        if (downloadedSize % (1024 * 1024) == 0) { // 每MB打印一次进度
                            log.info("下载进度: {:.1f}%", progress);
                        }
                    }
                }
            }

            log.info("视频下载完成: {}", videoPath);
            return videoPath;

        } catch (IOException e) {
            log.error("下载视频时发生错误", e);
            throw new Exception("下载视频失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从视频文件中提取音频
     *
     * @param videoPath 视频文件路径
     * @return 音频文件路径
     * @throws Exception 提取失败时抛出异常
     */
    private Path extractAudio(Path videoPath) throws Exception {
        Path audioPath = videoPath.resolveSibling(
                FileUtil.getPrefix(videoPath.getFileName().toString()) + ".mp3");

        log.info("正在从视频提取音频: {} -> {}", videoPath, audioPath);

        try {
            // 使用JAVE2进行音频提取
            File source = videoPath.toFile();
            File target = audioPath.toFile();

            // 设置音频属性
            AudioAttributes audio = new AudioAttributes();
            audio.setCodec("libmp3lame");
            audio.setBitRate(128000);
            audio.setChannels(1);
            audio.setSamplingRate(16000);

            // 设置编码属性
            EncodingAttributes attrs = new EncodingAttributes();
            attrs.setOutputFormat("mp3");
            attrs.setAudioAttributes(audio);

            // 执行转换
            Encoder encoder = new Encoder();
            encoder.encode(new MultimediaObject(source), target, attrs);

            log.info("音频提取完成: {}", audioPath);
            return audioPath;

        } catch (EncoderException e) {
            log.error("提取音频时发生错误", e);
            throw new Exception("提取音频失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从音频文件中提取文字
     *
     * @param audioPath  音频文件路径
     * @param apiKey     API密钥
     * @param apiBaseUrl API基础URL
     * @param model      语音识别模型
     * @return 提取的文本内容
     * @throws Exception 提取失败时抛出异常
     */
    private String extractTextFromAudio(Path audioPath, String apiKey, String apiBaseUrl, String model) throws Exception {
        log.info("正在调用语音识别API提取文本...");

        try {
            File audioFile = audioPath.toFile();

            // 构建multipart请求
            RequestBody fileBody = RequestBody.create(audioFile, MediaType.parse("audio/mpeg"));
            RequestBody modelBody = RequestBody.create(model, MediaType.parse("text/plain"));

            MultipartBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", audioFile.getName(), fileBody)
                    .addFormDataPart("model", model)
                    .build();

            Request request = new Request.Builder()
                    .url(apiBaseUrl)
                    .header("Authorization", "Bearer " + apiKey)
                    .post(requestBody)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                ResponseBody body = response.body();
                
                if (!response.isSuccessful()) {
                    throw new IOException("API调用失败: " + response.body());
                }

                if (body == null) {
                    throw new IOException("API响应体为空");
                }

                String responseText = body.string();
                log.info("API响应: {}", responseText);

                // 解析响应
                try {
                    JsonNode responseJson = JsonUtil.parseJson(responseText);
                    if (responseJson.has("text")) {
                        String extractedText = responseJson.get("text").asText();
                        log.info("成功提取文本内容: {}", extractedText);
                        return extractedText;
                    } else {
                        log.warn("API响应中没有找到text字段，返回原始响应");
                        return responseText;
                    }
                } catch (Exception e) {
                    log.warn("解析API响应JSON失败，返回原始响应: {}", e.getMessage());
                    return responseText;
                }
            }

        } catch (IOException e) {
            log.error("调用语音识别API时发生错误", e);
            throw new Exception("提取文字失败: " + e.getMessage(), e);
        }
    }
    

    /**
     * 清理指定的文件
     *
     * @param filePaths 要清理的文件路径
     */
    private void cleanupFiles(Path... filePaths) {
        for (Path filePath : filePaths) {
            if (filePath != null && Files.exists(filePath)) {
                try {
                    Files.delete(filePath);
                    log.info("已清理文件: {}", filePath);
                } catch (IOException e) {
                    log.warn("清理文件失败: {}, 错误: {}", filePath, e.getMessage());
                }
            }
        }
    }

    /**
     * 清理临时目录
     */
    public void cleanup() {
        if (tempDir != null && Files.exists(tempDir)) {
            try {
                FileUtil.del(tempDir.toFile());
                log.info("已清理临时目录: {}", tempDir);
            } catch (Exception e) {
                log.warn("清理临时目录失败: {}, 错误: {}", tempDir, e.getMessage());
            }
        }
    }
}
