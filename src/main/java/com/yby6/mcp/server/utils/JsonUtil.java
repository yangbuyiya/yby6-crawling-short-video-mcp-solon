package com.yby6.mcp.server.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

/**
 * JSON工具类
 * 统一管理ObjectMapper实例，避免重复创建
 *
 * @author Yangbuyi
 * @date 2025/07/16
 */
@Slf4j
public class JsonUtil {
    
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    private JsonUtil() {
        // 私有构造函数，防止实例化
    }
    
    /**
     * 将JSON字符串解析为JsonNode
     *
     * @param jsonString JSON字符串
     * @return JsonNode对象
     * @throws Exception 解析异常
     */
    public static JsonNode parseJson(String jsonString) throws Exception {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            throw new IllegalArgumentException("JSON字符串不能为空");
        }
        
        try {
            return OBJECT_MAPPER.readTree(jsonString);
        } catch (Exception e) {
            log.error("JSON解析失败: {}", e.getMessage());
            throw new Exception("JSON解析失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 将对象转换为JSON字符串
     *
     * @param object 要转换的对象
     * @return JSON字符串
     * @throws Exception 转换异常
     */
    public static String toJsonString(Object object) throws Exception {
        if (object == null) {
            return "null";
        }
        
        try {
            return OBJECT_MAPPER.writeValueAsString(object);
        } catch (Exception e) {
            log.error("对象转JSON失败: {}", e.getMessage());
            throw new Exception("对象转JSON失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 将JSON字符串转换为指定类型的对象
     *
     * @param jsonString JSON字符串
     * @param clazz 目标类型
     * @param <T> 泛型类型
     * @return 转换后的对象
     * @throws Exception 转换异常
     */
    public static <T> T parseObject(String jsonString, Class<T> clazz) throws Exception {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            throw new IllegalArgumentException("JSON字符串不能为空");
        }
        
        try {
            return OBJECT_MAPPER.readValue(jsonString, clazz);
        } catch (Exception e) {
            log.error("JSON转对象失败: {}", e.getMessage());
            throw new Exception("JSON转对象失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取ObjectMapper实例（如果需要更复杂的操作）
     *
     * @return ObjectMapper实例
     */
    public static ObjectMapper getObjectMapper() {
        return OBJECT_MAPPER;
    }
} 