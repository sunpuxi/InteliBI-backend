package com.ai.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONUtil;
import com.ai.MQ.BIMessageProducer;
import com.ai.common.ErrorCode;
import com.ai.manager.RedisLimiterManager;
import com.ai.model.dto.chart.*;
import com.ai.model.entity.Chart;
import com.ai.model.entity.User;
import com.ai.model.vo.BIResponseVO;
import com.ai.service.UserService;
import com.ai.utils.ExcelToCSV;
import com.ai.utils.SqlUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ai.annotation.AuthCheck;
import com.ai.common.BaseResponse;
import com.ai.common.DeleteRequest;
import com.ai.common.ResultUtils;
import com.ai.constant.ChartStatusConstant;
import com.ai.constant.CommonConstant;
import com.ai.constant.UserConstant;
import com.ai.exception.BusinessException;
import com.ai.exception.ThrowUtils;
import com.ai.manager.AIManager;
import com.ai.service.ChartService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 帖子接口
 *
 */
@RestController
@RequestMapping("/chart")
@Slf4j
public class ChartController {

    //文件后缀的白名单
    final static List<String> valid = Arrays.asList("xls", "xlsx");

    final String prefix = "chart_";
    @Resource
    private ChartService chartService;

    @Resource
    private UserService userService;
    @Resource
    private AIManager aiManager;

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;
    @Resource
    private BIMessageProducer biMessageProducer;

    @Resource
    private RedisLimiterManager redisLimiterManager;

    @Resource
    private StringRedisTemplate stringRedisTemplate;




    // region 增删改查

    /**
     * 创建
     *
     * @param chartAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addChart(@RequestBody ChartAddRequest chartAddRequest, HttpServletRequest request) {
        if (chartAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartAddRequest, chart);
        User loginUser = userService.getLoginUser(request);
        chart.setUserId(loginUser.getId());
        boolean result = chartService.save(chart);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        long newChartId = chart.getId();
        return ResultUtils.success(newChartId);
    }

    /**
     * 删除
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteChart(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldChart.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = chartService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 更新（仅管理员）
     *
     * @param chartUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateChart(@RequestBody ChartUpdateRequest chartUpdateRequest) {
        if (chartUpdateRequest == null || chartUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartUpdateRequest, chart);
        long id = chartUpdateRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取
     *
     * @param id
     * @return
     */
    @GetMapping("/get")
    public BaseResponse<Chart> getChartVOById(long id, HttpServletRequest request) {
        if (id <= 0 || StringUtils.isBlank(String.valueOf(id))) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR, "未登录...");
        Long loginUserId = loginUser.getId();
        String cache_id = prefix + loginUserId + "_" + id;

        String chart_json = stringRedisTemplate.opsForValue().get(cache_id);

        //如果在缓存中，则转换后返回
        if (!StringUtils.isEmpty(chart_json)) {
            String chartJson= stringRedisTemplate.opsForValue().get(cache_id);
            Chart chart = JSONUtil.toBean(chartJson, Chart.class);
            return ResultUtils.success(chart);
        }

        //如果查询缓存为空，则存入缓存
        Chart chart = chartService.getById(id);
        if (chart == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        stringRedisTemplate.opsForValue().set(cache_id,JSONUtil.toJsonStr(chart));
        return ResultUtils.success(chart);
    }

    /**
     * 分页获取列表（封装类）
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<Chart>> listChartByPage(@RequestBody ChartQueryRequest chartQueryRequest,
                                                     HttpServletRequest request) {
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }

    /**
     * 分页获取当前用户创建的资源列表
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page")
    public BaseResponse<Page<Chart>> listMyChartByPage(@RequestBody ChartQueryRequest chartQueryRequest,
                                                       HttpServletRequest request) {

        if (chartQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        chartQueryRequest.setUserId(loginUser.getId());
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }

    // endregion


    /**
     * 编辑（用户）
     *
     * @param chartEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editChart(@RequestBody ChartEditRequest chartEditRequest, HttpServletRequest request) {
        if (chartEditRequest == null || chartEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartEditRequest, chart);
        User loginUser = userService.getLoginUser(request);
        long id = chartEditRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldChart.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }

    private QueryWrapper<Chart> getQueryWrapper(ChartQueryRequest chartQueryRequest) {
        QueryWrapper<Chart> queryWrapper = new QueryWrapper<>();
        if (chartQueryRequest == null) {
            return queryWrapper;
        }
        Long id = chartQueryRequest.getId();
        String goal = chartQueryRequest.getGoal();
        String name = chartQueryRequest.getName();
        String chartType = chartQueryRequest.getChartType();
        long userId = chartQueryRequest.getUserId();

        String sortField = chartQueryRequest.getSortField();
        String sortOrder = chartQueryRequest.getSortOrder();

        // 拼接查询条件
        queryWrapper.eq(id != null && id > 0, "id", id);
        queryWrapper.eq(!StringUtils.isBlank(goal), "goal", goal);
        queryWrapper.like(!StringUtils.isBlank(name), "name", name);
        queryWrapper.eq(!StringUtils.isBlank(chartType), "chartType", chartType);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);

        queryWrapper.eq("isDelete", false);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    /**
     * 智能分析（同步）
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */
    @PostMapping("/gen")
    public BaseResponse<BIResponseVO> genChartByAi(@RequestPart("file") MultipartFile multipartFile,
                                                   genChartRequest genChartByAiRequest, HttpServletRequest request) {
        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();
        // 校验
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "目标为空");
        ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length() > 100, ErrorCode.PARAMS_ERROR, "名称过长");
        // 校验文件
        long size = multipartFile.getSize();
        String originalFilename = multipartFile.getOriginalFilename();
        // 校验文件大小
        final long ONE_MB = 1024 * 1024L;
        ThrowUtils.throwIf(size > ONE_MB, ErrorCode.PARAMS_ERROR, "文件超过 1M");
        // 校验文件后缀 aaa.png
        String suffix = FileUtil.getSuffix(originalFilename);
        final List<String> validFileSuffixList = Arrays.asList("xlsx");
        ThrowUtils.throwIf(!validFileSuffixList.contains(suffix), ErrorCode.PARAMS_ERROR, "文件后缀非法");

        User loginUser = userService.getLoginUser(request);
        // 限流判断，每个用户一个限流器
        redisLimiterManager.doRateLimit("genChartByAi_" + loginUser.getId());

        long biModelId = 1659171950288818178L;


        // 构造用户输入
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求：").append("\n");

        // 拼接分析目标
        String userGoal = goal;
        if (StringUtils.isNotBlank(chartType)) {
            userGoal += "，请使用" + chartType;
        }
        userInput.append(userGoal).append("\n");
        userInput.append("原始数据：").append("\n");
        // 压缩后的数据
        String csvData = ExcelToCSV.ExcelTocsv(multipartFile);
        userInput.append(csvData).append("\n");

        String result = aiManager.doChat(biModelId, userInput.toString());
        String[] splits = result.split("【【【【【");
        if (splits.length < 3) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 生成错误");
        }
        String genChart = splits[1].trim();
        String genResult = splits[2].trim();
        // 插入到数据库
        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setChartData(csvData);
        chart.setChartType(chartType);
        chart.setGenChart(genChart);
        chart.setGenResult(genResult);
        chart.setUserId(loginUser.getId());
        chart.setStatus(ChartStatusConstant.SUCCESS);
        boolean saveResult = chartService.save(chart);
        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "图表保存失败");
        BIResponseVO biResponse = new BIResponseVO();
        biResponse.setGenChart(genChart);
        biResponse.setGenResult(genResult);
        biResponse.setChartId(chart.getId());
        return ResultUtils.success(biResponse);
    }

    /**
     * 接收上传的excel文件
     * 异步接收请求处理
     *
     * @param multipartFile
     * @param genChartRequest
     * @param request
     * @return
     */
    @PostMapping("/gen/asyc")
    public BaseResponse<BIResponseVO> genChartByAIAsyc(@RequestPart("file") MultipartFile multipartFile,
                                                       genChartRequest genChartRequest, HttpServletRequest request) {
        //校验文件的大小，防止对服务器造成压力
        long size = multipartFile.getSize();
        final long ONE_MB = 1024 * 1024;
        ThrowUtils.throwIf(size > ONE_MB, ErrorCode.PARAMS_ERROR, "文件超过1MB...");

        //校验文件后缀
        String originalFilename = multipartFile.getOriginalFilename();
        String suffix = FileUtil.getSuffix(originalFilename);
        final List<String> valid = Arrays.asList("xls", "xlsx");
        ThrowUtils.throwIf(!valid.contains(suffix), ErrorCode.PARAMS_ERROR, "文件格式错误...");

        //获取用户信息，只有当登录状态才可使用功能(不需要再做校验)
        User loginUser = userService.getLoginUser(request);
        //接收用户输入的信息
        String name = genChartRequest.getName();
        String goal = genChartRequest.getGoal();
        String chartType = genChartRequest.getChartType();
        //校验
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "目标为空！");
        ThrowUtils.throwIf(StringUtils.isBlank(name) && name.length() > 100, ErrorCode.PARAMS_ERROR, "名称不合法！");

        //限流
        redisLimiterManager.doRateLimit("genChartByAI_" + loginUser.getId());

        //AI的模型ID，调用的AI的类别
        long modelId = 1659171950288818178L;

        //用户输入
        StringBuilder userInput = new StringBuilder();
        String userGoal = goal;
        if (StringUtils.isNotBlank(chartType)) {
            userGoal += ",请使用" + chartType;
        }
        userInput.append("分析需求:").append(userGoal).append("\n");
        userInput.append("原始数据:").append("\n");

        //压缩之后的数据
        String cvsData = ExcelToCSV.ExcelTocsv(multipartFile);
        userInput.append(cvsData).append("\n");


        //将图表信息插入数据库中
        Chart chart = new Chart();
        chart.setGoal(goal);
        chart.setName(name);
        chart.setChartData(cvsData);
        chart.setChartType(chartType);
        chart.setUserId(loginUser.getId());
        chart.setStatus("wait");
        BIResponseVO biResponseVO = new BIResponseVO();
        biResponseVO.setChartId(chart.getId());
        boolean save = chartService.save(chart);
        //System.out.println("已经将数据存储至数据库。。。状态为等待中......");
        if (!save) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "数据库错误");
        }

        //利用定义的线程池处理任务并提交
        try {
            CompletableFuture.runAsync(() -> {
                //当接收请求之后，先修改执行状态为running,并修改数据库中的默认状态
                Chart chart1 = new Chart();
                chart1.setId(chart.getId());
                chart1.setStatus("running");
                boolean b = chartService.updateById(chart1);
                //System.out.println("已经将数据存储至数据库。。。状态为执行中......");

                if (!b) {
                    //更新数据库数据失败
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR);
                }

                //调用AI
                String s = aiManager.doChat(modelId, userInput.toString());

                //将生成的结果拆分，分别为给出的结论，以及图表代码块
                String[] split = s.split("【【【【【");
                if (split.length < 3) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI生成错误...");
                }

                //分别取出代码和结论
                String genChart = split[1].trim();
                String genResult = split[2].trim();
                Chart chart2 = new Chart();
                chart2.setId(chart.getId());
                chart2.setGenChart(genChart);
                chart2.setGenResult(genResult);
                chart2.setStatus("succeed");
                boolean b1 = chartService.updateById(chart2);
                if (!b1) {
                    //更新数据库数据失败
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR);
                }
            }, threadPoolExecutor);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return ResultUtils.success(biResponseVO);
    }

    /**
     * 接收上传的excel文件
     * 异步接收请求处理(消息队列实现)
     *
     * @param multipartFile
     * @param genChartRequest
     * @param request
     * @return
     */
    @PostMapping("/gen/asyc/mq")
    public BaseResponse<BIResponseVO> genChartByAIAsycMQ(@RequestPart("file") MultipartFile multipartFile,
                                                         genChartRequest genChartRequest, HttpServletRequest request) {

        //校验文件的大小，防止对服务器造成压力
        checkFile(multipartFile);

        //获取用户信息，只有当登录状态才可使用功能(不需要再做校验)
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "未登录无法操作...");
        }

        //接收用户输入的信息，并做校验
        String name = genChartRequest.getName();
        String goal = genChartRequest.getGoal();
        String chartType = genChartRequest.getChartType();
        String csvData = ExcelToCSV.ExcelTocsv(multipartFile);
        ThrowUtils.throwIf(StringUtils.isBlank(csvData), ErrorCode.PARAMS_ERROR, "请求参数错误");
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "分析需求为空！");
        ThrowUtils.throwIf(StringUtils.isBlank(chartType), ErrorCode.PARAMS_ERROR, "图表类型为空！");
        ThrowUtils.throwIf(StringUtils.isBlank(name) && name.length() > 100, ErrorCode.PARAMS_ERROR, "名称不合法！");

        //为用户分配令牌进行限流
        redisLimiterManager.doRateLimit("genChartByAI_User_" + loginUser.getId());

        //将初始化的图表信息插入数据库中，将图表状态设置为等待生成；
        Chart chart = new Chart();
        chart.setGoal(goal);
        chart.setName(name);
        chart.setChartData(csvData);
        chart.setChartType(chartType);
        chart.setUserId(loginUser.getId());
        chart.setStatus(ChartStatusConstant.WAIT);
        boolean b = chartService.save(chart);
        if (!b) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "数据库错误");
        }

        //将消息发送至消息队列，等待处理
        biMessageProducer.sendMessage(String.valueOf(chart.getId()));

        //封装返回信息
        BIResponseVO biResponseVO = new BIResponseVO();
        biResponseVO.setChartId(chart.getId());
        biResponseVO.setGenChart(chart.getGenChart());
        biResponseVO.setGenResult(chart.getGenResult());
        return ResultUtils.success(biResponseVO);
    }


    /**
     * 校验文件是否符合要求
     *
     * @param multipartFile
     */
    public static void checkFile(MultipartFile multipartFile) {
        long size = multipartFile.getSize();
        final long ONE_MB = 1024 * 1024;
        ThrowUtils.throwIf(size > ONE_MB, ErrorCode.PARAMS_ERROR, "文件超过1MB...");
        //校验文件后缀
        String originalFilename = multipartFile.getOriginalFilename();
        String suffix = FileUtil.getSuffix(originalFilename);
        ThrowUtils.throwIf(!valid.contains(suffix), ErrorCode.PARAMS_ERROR, "文件格式错误...");
    }

}
