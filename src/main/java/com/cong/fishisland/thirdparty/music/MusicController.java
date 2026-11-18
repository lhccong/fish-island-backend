package com.cong.fishisland.thirdparty.music;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.ResultUtils;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SaCheckLogin
@RequestMapping("/third-party/music")
public class MusicController {
    private final MusicUpstream upstream;

    public MusicController(MusicUpstream upstream) {
        this.upstream = upstream;
    }

    @GetMapping("/search")
    public BaseResponse<JsonNode> search(@RequestParam String source,
                                        @RequestParam String keyword,
                                        @RequestParam(defaultValue = "1") int page) {
        return ResultUtils.success(upstream.search(source, keyword, page));
    }

    @GetMapping("/url")
    public BaseResponse<String> url(@RequestParam String source, @RequestParam String id) {
        return ResultUtils.success(upstream.resolve(source, id));
    }

    @GetMapping("/recommendations")
    public BaseResponse<JsonNode> recommendations(@RequestParam String source) {
        return ResultUtils.success(upstream.recommendations(source));
    }
}
