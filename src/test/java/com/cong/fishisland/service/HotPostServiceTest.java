package com.cong.fishisland.service;

import com.cong.fishisland.common.TestBaseByLogin;
import com.cong.fishisland.model.vo.hot.HotPostVO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;

public class HotPostServiceTest extends TestBaseByLogin {

    @Resource
    private HotPostService hotPostService;

    @Resource
    private FishPetService fishPetService;

    @Test
    void testHotPost() {
        fishPetService.batchUpdateOnlineUserPetExp(Arrays.asList("1916315045496721410"));
    }

    @Test
    void testHotPostList() {
        List<HotPostVO> hotPostList = hotPostService.getHotPostList();

        Assertions.assertFalse(hotPostList.isEmpty());
    }
}
