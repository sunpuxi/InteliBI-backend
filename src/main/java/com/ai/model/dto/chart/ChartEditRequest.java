package com.yupi.springbootinit.model.dto.chart;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 编辑请求
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
@Data
public class ChartEditRequest implements Serializable {


    private Long id;

    /**
     *
     */
    private String goal;

    /**
     *
     */
    private String chartData;

    /**
     *
     */
    private String chartType;

    /**
     *名称
     */
    private String name;



    private static final long serialVersionUID = 1L;
}