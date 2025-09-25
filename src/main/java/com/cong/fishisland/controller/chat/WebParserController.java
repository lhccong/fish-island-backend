package com.cong.fishisland.controller.chat;

import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.model.vo.WebParseVO;
import com.cong.fishisland.service.WebParserService;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/web")
@Slf4j
@RequiredArgsConstructor
//@Api(tags = "网页解析接口")
public class WebParserController {

    private final WebParserService webParserService;

    @GetMapping("/parse")
    @ApiOperation(value = "解析网页信息")
    public BaseResponse<WebParseVO> parseWebPage(@RequestParam String url) {
        return ResultUtils.success(webParserService.parseWebPage(url));
    }
} 