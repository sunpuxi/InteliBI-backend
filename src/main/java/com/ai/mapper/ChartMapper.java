package com.yupi.springbootinit.mapper;

import com.yupi.springbootinit.model.entity.Chart;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;
import java.util.Map;

/**
* @author Administrator
* @description 针对表【chart(图表信息表)】的数据库操作Mapper
* @createDate 2023-11-26 09:25:31
* @Entity com.yupi.springbootinit.model.entity.Chart
*/
public interface ChartMapper extends BaseMapper<Chart> {
    // TO DO实现分库分表，将用户的数据单独创建一个数据库表（读取上传的数据中的字段信息创建表，以便提高查询效率）

}




