package com.ai.service.impl;

import com.ai.mapper.ChartMapper;
import com.ai.model.entity.Chart;
import com.ai.service.ChartService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
* @author Administrator
* @description 针对表【chart(图表信息表)】的数据库操作Service实现
* @createDate 2023-11-26 09:25:31
*/
@Service
public class ChartServiceImpl extends ServiceImpl<ChartMapper, Chart>
    implements ChartService {
}




