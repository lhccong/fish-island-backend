package com.cong.fishisland.service.impl.post;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.common.exception.ThrowUtils;
import com.cong.fishisland.constant.CommonConstant;
import com.cong.fishisland.constant.RedisKey;
import com.cong.fishisland.mapper.post.PostFavourMapper;
import com.cong.fishisland.mapper.post.PostMapper;
import com.cong.fishisland.mapper.post.PostThumbMapper;
import com.cong.fishisland.model.dto.post.PostFeaturedRequest;
import com.cong.fishisland.model.dto.post.PostQueryRequest;
import com.cong.fishisland.model.dto.post.PostRandomThumbRequest;
import com.cong.fishisland.model.entity.post.Post;
import com.cong.fishisland.model.entity.post.PostFavour;
import com.cong.fishisland.model.entity.post.PostThumb;
import com.cong.fishisland.model.entity.user.User;
import com.cong.fishisland.model.vo.post.PostRewardTokenVO;
import com.cong.fishisland.model.vo.post.PostVO;
import com.cong.fishisland.model.vo.user.UserRewardVO;
import com.cong.fishisland.model.vo.user.UserVO;
import com.cong.fishisland.service.CommentService;
import com.cong.fishisland.service.PostService;
import com.cong.fishisland.service.UserService;
import com.cong.fishisland.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import toolgood.words.StringSearch;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.cong.fishisland.constant.PostConstant.POST_ID;
import static com.cong.fishisland.constant.RedisKey.POST_REWARD_USER_KEY;
import static com.cong.fishisland.constant.SystemConstants.SALT;

/**
 * 帖子服务实现
 * # @author <a href="https://github.com/lhccong">程序员聪</a>
 */
@Service
@Slf4j
public class PostServiceImpl extends ServiceImpl<PostMapper, Post> implements PostService {

    /**
     * 兑奖加密盐值
     */
    private static final String REWARD_SALT = SALT + "_REWARD";

    @Resource
    private UserService userService;

    @Resource
    private PostThumbMapper postThumbMapper;

    @Resource
    private PostFavourMapper postFavourMapper;

    @Resource
    private StringSearch wordsUtil;

    @Resource
    private CommentService commentService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 帖子加精
     */
    private static final Integer POST_FEATURED_STATUS = 1;

    /**
     * 帖子取消加精
     */
    private static final Integer POST_UNFEATURED_STATUS = 0;

    @Override
    @Async
    public void incrementViewCountAsync(Long postId) {
        // 使用原子操作更新数据库
        update(new UpdateWrapper<Post>()
                .setSql("viewNum = viewNum + 1")
                .eq("id", postId));
    }

    @Override
    public Boolean setFeaturedStatus(PostFeaturedRequest request) {
        validPostFeaturedRequest(request);
        Long id = request.getId();
        Integer isFeatured = request.getIsFeatured();
        // 检查帖子是否存在
        Post post = getById(id);
        ThrowUtils.throwIf(post == null, ErrorCode.NOT_FOUND_ERROR, "帖子不存在");
        // 如果要修改加精状态与原来一致，则不进行更新
        ThrowUtils.throwIf(isFeatured.equals(POST_FEATURED_STATUS) && isFeatured.equals(post.getIsFeatured()), ErrorCode.OPERATION_ERROR, "帖子已加精，请勿重复操作");
        ThrowUtils.throwIf(isFeatured.equals(POST_UNFEATURED_STATUS) && isFeatured.equals(post.getIsFeatured()), ErrorCode.OPERATION_ERROR, "帖子已取消加精，请勿重复操作");
        // 使用Lambda更新器高效更新状态
        LambdaUpdateWrapper<Post> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Post::getId, id)
                .set(Post::getIsFeatured, isFeatured);

        return update(updateWrapper);

    }

    @Override
    public Page<PostVO> listFavourPostByPage(PostQueryRequest postQueryRequest, Long userId) {
        ThrowUtils.throwIf(postQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long current = postQueryRequest.getCurrent();
        long size = postQueryRequest.getPageSize();

        // 1. 分页查询收藏记录
        Page<PostFavour> favourPage = new Page<>(current, size);
        QueryWrapper<PostFavour> favourQueryWrapper = new QueryWrapper<>();
        favourQueryWrapper.eq("userId", userId);
        Page<PostFavour> postFavourPage = postFavourMapper.selectPage(favourPage, favourQueryWrapper);

        // 2. 提取帖子ID并查询帖子
        List<Long> postIdList = postFavourPage.getRecords().stream()
                .map(PostFavour::getPostId)
                .collect(Collectors.toList());

        if (CollUtil.isEmpty(postIdList)) {
            return new Page<>(current, size, 0);
        }

        // 3. 查询帖子并转换VO
        QueryWrapper<Post> postQueryWrapper = new QueryWrapper<>();
        postQueryWrapper.in("id", postIdList);
        Page<Post> postPage = this.page(new Page<>(current, size), postQueryWrapper);
        return getPostVOPage(postPage);
    }

    private void validPostFeaturedRequest(PostFeaturedRequest request){
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR, "参数为空");
        Long id = request.getId();
        Integer isFeatured = request.getIsFeatured();
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR, "帖子ID不合法");
        ThrowUtils.throwIf(isFeatured == null || (!POST_UNFEATURED_STATUS.equals(isFeatured) && !POST_FEATURED_STATUS.equals(isFeatured)), ErrorCode.PARAMS_ERROR, "加精状态参数不合法");
    }

    @Override
    public void validPost(Post post, boolean add) {
        if (post == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String title = post.getTitle();
        String content = post.getContent();
        String tags = post.getTags();

        ThrowUtils.throwIf(StringUtils.isBlank(title), ErrorCode.PARAMS_ERROR, "标题不能为空");
        ThrowUtils.throwIf(StringUtils.isBlank(content), ErrorCode.PARAMS_ERROR, "内容不能为空");
        ThrowUtils.throwIf(StringUtils.isBlank(tags), ErrorCode.PARAMS_ERROR, "标签不能为空");
        // 敏感词校验（标题和内容）
        String titleSensitiveWord = wordsUtil.FindFirst(title);
        String contentSensitiveWord = wordsUtil.FindFirst(content);
        // 构建敏感词提示信息
        StringBuilder sensitiveWords = new StringBuilder();
        if (StringUtils.isNotBlank(titleSensitiveWord)) {
            sensitiveWords.append("标题包含敏感词: ").append(titleSensitiveWord);
        }
        if (StringUtils.isNotBlank(contentSensitiveWord)) {
            if (sensitiveWords.length() > 0) sensitiveWords.append("; ");
            sensitiveWords.append("内容包含敏感词: ").append(contentSensitiveWord);
        }
        // 如果有敏感词，抛出异常并提示具体敏感词
        if (sensitiveWords.length() > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, sensitiveWords.toString());
        }
        // 有参数则校验
        if (StringUtils.isNotBlank(title) && title.length() > 80) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标题过长");
        }

    }

    /**
     * 获取查询包装类
     *
     * @param postQueryRequest 发布查询请求
     * @return {@link QueryWrapper}<{@link Post}>
     */
    @Override
    public QueryWrapper<Post> getQueryWrapper(PostQueryRequest postQueryRequest) {
        QueryWrapper<Post> queryWrapper = new QueryWrapper<>();
        if (postQueryRequest == null) {
            return queryWrapper;
        }
        String sortField = postQueryRequest.getSortField();
        String sortOrder = postQueryRequest.getSortOrder();
        String title = postQueryRequest.getTitle();
        String content = postQueryRequest.getContent();
        List<String> tagList = postQueryRequest.getTags();
        Long userId = postQueryRequest.getUserId();
        Integer isFeatured = postQueryRequest.getIsFeatured();
        String searchText = postQueryRequest.getSearchText();
        // 拼接查询条件
        if (StringUtils.isNotBlank(searchText)) {
            queryWrapper.and(qw -> qw.like("title", searchText).or().like("content", searchText));
        }
        queryWrapper.like(StringUtils.isNotBlank(title), "title", title);
        queryWrapper.like(StringUtils.isNotBlank(content), "content", content);
        if (CollUtil.isNotEmpty(tagList)) {
            for (String tag : tagList) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(isFeatured), "isFeatured", isFeatured);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    @Override
    public Page<Post> searchFromEs(PostQueryRequest postQueryRequest) {
        return null;
    }


    @Override
    public PostVO getPostVO(Post post) {
        PostVO postVO = PostVO.objToVo(post);
        long postId = post.getId();
        // 1. 关联查询用户信息
        Long userId = post.getUserId();
        User user = null;
        if (userId != null && userId > 0) {
            user = userService.getById(userId);
        }
        UserVO userVO = userService.getUserVO(user);
        postVO.setUser(userVO);
        // 2. 已登录，获取用户点赞、收藏状态
        User loginUser = userService.getLoginUserPermitNull();
        if (loginUser != null) {
            // 获取点赞
            QueryWrapper<PostThumb> postThumbQueryWrapper = new QueryWrapper<>();
            postThumbQueryWrapper.in(POST_ID, postId);
            postThumbQueryWrapper.eq("userId", loginUser.getId());
            PostThumb postThumb = postThumbMapper.selectOne(postThumbQueryWrapper);
            postVO.setHasThumb(postThumb != null);
            // 获取收藏
            QueryWrapper<PostFavour> postFavourQueryWrapper = new QueryWrapper<>();
            postFavourQueryWrapper.in(POST_ID, postId);
            postFavourQueryWrapper.eq("userId", loginUser.getId());
            PostFavour postFavour = postFavourMapper.selectOne(postFavourQueryWrapper);
            postVO.setHasFavour(postFavour != null);
        }
        // 获取评论数
        postVO.setCommentNum(commentService.getCommentNum(postId));
        return postVO;
    }

    @Override
    public Page<PostVO> getPostVOPage(Page<Post> postPage) {
        List<Post> postList = postPage.getRecords();
        Page<PostVO> postVoPage = new Page<>(postPage.getCurrent(), postPage.getSize(), postPage.getTotal());
        if (CollUtil.isEmpty(postList)) {
            return postVoPage;
        }
        // 1. 关联查询用户信息
        Set<Long> userIdSet = postList.stream().map(Post::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 2. 已登录，获取用户点赞、收藏状态
        Map<Long, Boolean> postIdHasThumbMap = new HashMap<>();
        Map<Long, Boolean> postIdHasFavourMap = new HashMap<>();
        User loginUser = userService.getLoginUserPermitNull();
        if (loginUser != null) {
            Set<Long> postIdSet = postList.stream().map(Post::getId).collect(Collectors.toSet());
            loginUser = userService.getLoginUser();
            // 获取点赞
            QueryWrapper<PostThumb> postThumbQueryWrapper = new QueryWrapper<>();
            postThumbQueryWrapper.in(POST_ID, postIdSet);
            postThumbQueryWrapper.eq("userId", loginUser.getId());
            List<PostThumb> postPostThumbList = postThumbMapper.selectList(postThumbQueryWrapper);
            postPostThumbList.forEach(postPostThumb -> postIdHasThumbMap.put(postPostThumb.getPostId(), true));
            // 获取收藏
            QueryWrapper<PostFavour> postFavourQueryWrapper = new QueryWrapper<>();
            postFavourQueryWrapper.in(POST_ID, postIdSet);
            postFavourQueryWrapper.eq("userId", loginUser.getId());
            List<PostFavour> postFavourList = postFavourMapper.selectList(postFavourQueryWrapper);
            postFavourList.forEach(postFavour -> postIdHasFavourMap.put(postFavour.getPostId(), true));
        }
        // 填充信息
        List<PostVO> postVOList = postList.stream().map(post -> {
            PostVO postVO = PostVO.objToVo(post);
            Long userId = post.getUserId();
            User user = null;
            Long postId = post.getId();
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            postVO.setUser(userService.getUserVO(user));
            postVO.setHasThumb(postIdHasThumbMap.getOrDefault(postId, false));
            postVO.setHasFavour(postIdHasFavourMap.getOrDefault(postId, false));
            // 获取评论数
            postVO.setCommentNum(commentService.getCommentNum(postId));
            // 获取点赞最高一条评论
            postVO.setThumbComment(commentService.getThumbComment(postId));
            return postVO;
        }).collect(Collectors.toList());
        postVoPage.setRecords(postVOList);
        return postVoPage;
    }

    @Override
    public UserRewardVO randomThumbUser(PostRandomThumbRequest request) {
        // 1. 参数校验
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR, "参数为空");
        Long postId = request.getPostId();
        Integer randomIndex = request.getRandomIndex();
        ThrowUtils.throwIf(postId == null || postId <= 0, ErrorCode.PARAMS_ERROR, "帖子ID不合法");
        ThrowUtils.throwIf(randomIndex == null || randomIndex < 0, ErrorCode.PARAMS_ERROR, "随机数不合法");

        // 2. 检查帖子是否存在
        Post post = getById(postId);
        ThrowUtils.throwIf(post == null, ErrorCode.NOT_FOUND_ERROR, "帖子不存在");

        // 3. 检查今天是否已经生成过中奖用户
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String cacheKey = RedisKey.getKey(POST_REWARD_USER_KEY, postId, today);
        String cachedResult = stringRedisTemplate.opsForValue().get(cacheKey);
        if (StringUtils.isNotBlank(cachedResult)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "今天已经生成过中奖用户，请明天再试");
        }

        // 4. 获取帖子的点赞列表
        QueryWrapper<PostThumb> postThumbQueryWrapper = new QueryWrapper<>();
        postThumbQueryWrapper.eq(POST_ID, postId);
        List<PostThumb> postThumbList = postThumbMapper.selectList(postThumbQueryWrapper);

        // 5. 检查点赞列表是否为空
        ThrowUtils.throwIf(CollUtil.isEmpty(postThumbList), ErrorCode.OPERATION_ERROR, "该帖子暂无点赞用户");

        // 6. 验证随机数不能大于点赞列表数量
        int thumbCount = postThumbList.size();
        ThrowUtils.throwIf(randomIndex > thumbCount, ErrorCode.PARAMS_ERROR,
                String.format("随机数不能大于点赞列表数量，当前点赞数：%d，随机数：%d", thumbCount, randomIndex));

        // 7. 打乱点赞列表
        Collections.shuffle(postThumbList);

        // 8. 根据随机数取一个用户
        PostThumb selectedThumb = postThumbList.get(randomIndex-1);
        Long userId = selectedThumb.getUserId();

        // 9. 查询用户信息
        User user = userService.getById(userId);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");

        // 10. 封装用户信息（包含加密字段）
        UserRewardVO userRewardVO = buildUserRewardVO(user, postId, userId);

        // 11. 将结果存入Redis缓存，设置过期时间为1天（到明天0点）
        String userRewardJson = JSONUtil.toJsonStr(userRewardVO);
        // 计算到明天0点的秒数
        long secondsUntilMidnight = getSecondsUntilMidnight();
        stringRedisTemplate.opsForValue().set(cacheKey, userRewardJson, secondsUntilMidnight, TimeUnit.SECONDS);

        return userRewardVO;
    }

    /**
     * 构建用户兑奖VO（包含加密字段）
     *
     * @param user 用户实体
     * @param postId 帖子id
     * @param currentUserId 当前用户id（被选中的用户）
     * @return {@link UserRewardVO}
     */
    private UserRewardVO buildUserRewardVO(User user, Long postId, Long currentUserId) {
        UserVO userVO = userService.getUserVO(user);
        UserRewardVO userRewardVO = new UserRewardVO();
        // 复制UserVO的所有字段
        BeanUtils.copyProperties(userVO, userRewardVO);
        // 生成兑奖加密token
        String rewardToken = generateRewardToken(postId, currentUserId);
        userRewardVO.setRewardToken(rewardToken);
        return userRewardVO;
    }

    @Override
    public PostRewardTokenVO getPostRewardToken(Long postId, Long userId) {
        // 1. 参数校验
        ThrowUtils.throwIf(postId == null || postId <= 0, ErrorCode.PARAMS_ERROR, "帖子ID不合法");
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "用户ID不合法");

        // 2. 检查帖子是否存在
        Post post = getById(postId);
        ThrowUtils.throwIf(post == null, ErrorCode.NOT_FOUND_ERROR, "帖子不存在");

        // 3. 生成兑奖加密token
        String rewardToken = generateRewardToken(postId, userId);
        
        // 4. 封装返回
        PostRewardTokenVO postRewardTokenVO = new PostRewardTokenVO();
        postRewardTokenVO.setRewardToken(rewardToken);
        return postRewardTokenVO;
    }

    /**
     * 生成兑奖加密token
     *
     * @param postId 帖子id
     * @param userId 用户id
     * @return 加密token
     */
    private String generateRewardToken(Long postId, Long userId) {
        // 生成兑奖加密token（帖子id + 当前用户id + 盐值加密）
        String encryptContent = postId + "_" + userId + "_" + REWARD_SALT;
        return DigestUtils.md5DigestAsHex(encryptContent.getBytes());
    }

    @Override
    public UserRewardVO getCurrentRewardUser(Long postId) {
        // 1. 参数校验
        ThrowUtils.throwIf(postId == null || postId <= 0, ErrorCode.PARAMS_ERROR, "帖子ID不合法");

        // 2. 检查帖子是否存在
        Post post = getById(postId);
        ThrowUtils.throwIf(post == null, ErrorCode.NOT_FOUND_ERROR, "帖子不存在");

        // 3. 从Redis获取当前中奖用户
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String cacheKey = RedisKey.getKey(POST_REWARD_USER_KEY, postId, today);
        String cachedResult = stringRedisTemplate.opsForValue().get(cacheKey);

        // 4. 如果没有缓存，返回null或抛出异常
        if (StringUtils.isBlank(cachedResult)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "今天还未生成中奖用户");
        }

        // 5. 解析JSON并返回（兑奖token设为null）
        UserRewardVO userRewardVO = JSONUtil.toBean(cachedResult, UserRewardVO.class);
        userRewardVO.setRewardToken(null);
        return userRewardVO;
    }

    /**
     * 计算到明天0点的秒数
     *
     * @return 秒数
     */
    private long getSecondsUntilMidnight() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        long secondsUntilMidnight = java.time.Duration.between(
                java.time.LocalDateTime.now(),
                tomorrow.atStartOfDay()
        ).getSeconds();
        return secondsUntilMidnight;
    }

}




