package com.yupi.springbootinit.model.dto.chart;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 更新请求
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
@Data
public class ChartUpdateRequest implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
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
     *
     */
    private String genChart;

    /**
     *
     */
    private String genResult;

    /**
     *
     */
    private Long userId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;
    /**
     *名称
     */
    private String name;

    private static final long serialVersionUID = 1L;
}