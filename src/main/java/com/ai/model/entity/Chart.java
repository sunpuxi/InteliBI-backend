package com.yupi.springbootinit.model.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 图表信息表
 * @TableName chart
 */
@TableName(value ="chart")
@Data
public class Chart implements Serializable {
    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 
     */
    private String goal;

    private String name;

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
    private String status;

    /**
     *
     */
    private String message;

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

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}