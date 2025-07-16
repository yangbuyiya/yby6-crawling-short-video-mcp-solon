package com.yby6.mcp.server;

import com.yby6.mcp.server.model.VideoInfo;
import com.yby6.mcp.server.service.VideoParseService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.noear.solon.annotation.Inject;
import org.noear.solon.test.SolonTest;

@SolonTest(App.class)
@Slf4j
public class VideoParseIntegrationTest {
    
    @Inject
    private VideoParseService videoParseService;
    
    @Test
    public void testDouyinVideo() throws Exception {
        // 视频
        VideoInfo videoInfo = videoParseService.parseShareUrl("0.76 05/02 fBT:/ V@l.PK 【艾德宝陪您新说唱第五期上（1）】Vinz-t真的无敌了，又来一首爆单啊这是， 迪椰果也是稳稳接住# 新说唱2025 # vinzt # 音乐就要这么玩  https://v.douyin.com/7nF11zmcGpc/ 复制此链接，打开Dou音搜索，直接观看视频！");
        log.info("抖音：{}", videoInfo);
        
        // 图集
        VideoInfo videoInfo1 = videoParseService.parseShareUrl("2.51 08/26 n@Q.xS jCu:/ 第一眼 就看见 你的出现。# 分享照片 # 甜妹 # 开学穿搭 # ccd # 图文伙伴计划2024  https://v.douyin.com/U5AEc2n2QNM/ 复制此链接，打开Dou音搜索，直接观看视频！");
        log.info("图集：{}", videoInfo1);
        // 图集 & 实况
        VideoInfo videoInfo2 = videoParseService.parseShareUrl("5.84 h@b.aA aaN:/ 05/25 实况可以发多张耶！# 抖音可以发多张live图啦  https://v.douyin.com/G65exBCPc2s/ 复制此链接，打开Dou音搜索，直接观看视频！");
        log.info("图集 & 实况：{}", videoInfo2);
    }
    
    @Test
    public void testRedBookVideo() throws Exception {
        // 图集 & 实况
        VideoInfo videoInfo = videoParseService.parseShareUrl("94 【88张爆款图集｜高清未使用 - 七月-原创素材 | 小红书 - 你的生活兴趣社区】 \uD83D\uDE06 z1gwFdHs88CS3ik \uD83D\uDE06 https://www.xiaohongshu.com/discovery/item/67f4fbe6000000001a0074db?source=webshare&xhsshare=pc_web&xsec_token=ABOwYBWuIuUdegg-P4q4rc58B0mEMGnOPxIrYNrQ1P5F0=&xsec_source=pc_share");
        log.info("小红书：{}", videoInfo);
    }
    
    @Test
    public void testPipixiaVideo() throws Exception {
        VideoInfo videoInfo = videoParseService.parseShareUrl("皮皮虾，我们走！ https://h5.pipix.com/s/123456 快来皮皮虾看精彩短视频～");
        log.info("皮皮虾：{}", videoInfo);
    }
    
    @Test
    public void testWeishiVideo() throws Exception {
        VideoInfo videoInfo = videoParseService.parseShareUrl("【微视】分享精彩短视频 https://isee.weishi.qq.com/ws/app-pages/share/index.html?id=v_123456&spid=123 快来微视看精彩内容～");
        log.info("微视：{}", videoInfo);
    }
    
    @Test
    public void testLvzhouVideo() throws Exception {
        VideoInfo videoInfo = videoParseService.parseShareUrl("绿洲分享 https://weibo.cn/sinaurl?toasturl=123456&u=https%3A//m.weibo.cn/123456 快来绿洲看精彩视频～");
        log.info("绿洲：{}", videoInfo);
    }
    
    @Test
    public void testZuiyouVideo() throws Exception {
        VideoInfo videoInfo = videoParseService.parseShareUrl("【最右】哈哈哈这个太逗了 https://share.xiaochuankeji.cn/hybrid/share/post?pid=123456&zy_to=applink&share_count=1 快来最右看精彩内容！");
        log.info("最右：{}", videoInfo);
    }
    
    @Test
    public void testQuanminVideo() throws Exception {
        VideoInfo videoInfo = videoParseService.parseShareUrl("全民小视频分享 https://xspshare.baidu.com/video/123456?fr=share 快来全民小视频看精彩内容！");
        log.info("全民小视频：{}", videoInfo);
    }
    
    @Test
    public void testXiguaVideo() throws Exception {
        VideoInfo videoInfo = videoParseService.parseShareUrl("西瓜视频分享 https://v.ixigua.com/123456/ 快来西瓜视频看精彩内容！");
        log.info("西瓜视频：{}", videoInfo);
    }
    
    @Test
    public void testLishipinVideo() throws Exception {
        VideoInfo videoInfo = videoParseService.parseShareUrl("梨视频分享 https://www.pearvideo.com/video_123456 快来梨视频看精彩内容！");
        log.info("梨视频：{}", videoInfo);
    }
    
    @Test
    public void testPipigaoxiaoVideo() throws Exception {
        VideoInfo videoInfo = videoParseService.parseShareUrl("皮皮搞笑分享 https://h5.pipigx.com/detail/123456 快来皮皮搞笑看精彩内容！");
        log.info("皮皮搞笑：{}", videoInfo);
    }
    
    @Test
    public void testHuyaVideo() throws Exception {
        VideoInfo videoInfo = videoParseService.parseShareUrl("虎牙短视频分享 https://v.huya.com/play/123456.html 快来虎牙看精彩内容！");
        log.info("虎牙：{}", videoInfo);
    }
    
    @Test
    public void testAcfunVideo() throws Exception {
        VideoInfo videoInfo = videoParseService.parseShareUrl("AcFun弹幕视频网分享 https://www.acfun.cn/v/ac123456 快来AcFun看精彩内容！");
        log.info("AcFun：{}", videoInfo);
    }
    
    @Test
    public void testDoupaiVideo() throws Exception {
        VideoInfo videoInfo = videoParseService.parseShareUrl("逗拍分享 https://doupai.cc/video/123456 快来逗拍看精彩内容！");
        log.info("逗拍：{}", videoInfo);
    }
    
    @Test
    public void testMeipaiVideo() throws Exception {
        VideoInfo videoInfo = videoParseService.parseShareUrl("美拍分享 https://meipai.com/media/123456 快来美拍看精彩内容！");
        log.info("美拍：{}", videoInfo);
    }
    
    @Test
    public void testQuanminKgeVideo() throws Exception {
        VideoInfo videoInfo = videoParseService.parseShareUrl("全民K歌分享 https://kg.qq.com/node/play?s=123456 快来全民K歌听精彩歌曲！");
        log.info("全民K歌：{}", videoInfo);
    }
    
    @Test
    public void testSixroomVideo() throws Exception {
        VideoInfo videoInfo = videoParseService.parseShareUrl("六间房分享 https://6.cn/watch/123456.shtml 快来六间房看精彩内容！");
        log.info("六间房：{}", videoInfo);
    }
    
    @Test
    public void testXinpianchangVideo() throws Exception {
        VideoInfo videoInfo = videoParseService.parseShareUrl("新片场分享 https://xinpianchang.com/a123456 快来新片场看精彩内容！");
        log.info("新片场：{}", videoInfo);
    }
    
    @Test
    public void testHaokanVideo() throws Exception {
        VideoInfo videoInfo = videoParseService.parseShareUrl("好看视频分享 https://haokan.baidu.com/v?pd=wisenatural&vid=123456 快来好看视频看精彩内容！");
        log.info("好看视频：{}", videoInfo);
    }
    
}
