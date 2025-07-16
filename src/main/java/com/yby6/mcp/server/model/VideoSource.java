package com.yby6.mcp.server.model;

import lombok.Getter;

import java.util.List;

/**
 * 视频来源枚举
 * 定义支持的视频平台及其域名信息
 *
 * @author Yangbuyi
 * @date 2025/07/16
 */
@Getter
public enum VideoSource {
    DOUYIN("douyin", List.of("v.douyin.com", "www.iesdouyin.com", "www.douyin.com")),
    KUAISHOU("kuaishou", List.of("v.kuaishou.com")),
    REDBOOK("redbook", List.of("www.xiaohongshu.com", "xhslink.com")),
    WEIBO("weibo", List.of("weibo.com")),
    PIPIXIA("pipixia", List.of("h5.pipix.com")),
    WEISHI("weishi", List.of("isee.weishi.qq.com")),
    LVZHOU("lvzhou", List.of("weibo.cn")),
    ZUIYOU("zuiyou", List.of("share.xiaochuankeji.cn")),
    QUANMIN("quanmin", List.of("xspshare.baidu.com")),
    XIGUA("xigua", List.of("v.ixigua.com", "www.ixigua.com")),
    LISHIPIN("lishipin", List.of("www.pearvideo.com")),
    PIPIGAOXIAO("pipigaoxiao", List.of("h5.pipigx.com")),
    HUYA("huya", List.of("v.huya.com")),
    ACFUN("acfun", List.of("www.acfun.cn")),
    DOUPAI("doupai", List.of("doupai.cc")),
    MEIPAI("meipai", List.of("meipai.com")),
    QUANMINKGE("quanminkge", List.of("kg.qq.com")),
    SIXROOM("sixroom", List.of("6.cn")),
    XINPIANCHANG("xinpianchang", List.of("xinpianchang.com")),
    HAOKAN("haokan", List.of("haokan.baidu.com", "haokan.hao123.com"));
    
    private final String code;
    private final List<String> domains;
    
    VideoSource(String code, List<String> domains) {
        this.code = code;
        this.domains = domains;
    }
    
}
