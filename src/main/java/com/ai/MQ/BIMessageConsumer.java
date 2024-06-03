package com.yupi.springbootinit.MQ;

import com.rabbitmq.client.Channel;
import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.constant.ChartStatusConstant;
import com.yupi.springbootinit.exception.BusinessException;
import com.yupi.springbootinit.manager.AIManager;
import com.yupi.springbootinit.model.entity.Chart;
import com.yupi.springbootinit.service.ChartService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 消费者方
 */
@Component
@Slf4j
public class BIMessageConsumer {

    @Resource
    private AIManager aiManager;

    @Resource
    private ChartService chartService;

    @SneakyThrows  //忽略代码报的异常
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name=BI_CONSTANT.BI_QUEUE,durable = "true"),
            exchange = @Exchange(name=BI_CONSTANT.BI_EXCHANGE,type = ExchangeTypes.FANOUT)
    ),ackMode = "MANUAL")  //监听队列，并绑定交换机（没有则创建）
    public void recieveMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliverTag){
        //如果参数为空，则拒收消息，并抛出异常
        if (StringUtils.isBlank(message)){
            channel.basicNack(deliverTag,false,false);
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"消息为空...");
        }
        long chartId=Long.parseLong(message);
        Chart chart = chartService.getById(chartId);
        if (chart==null){
            channel.basicNack(deliverTag,false,false);
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"图表为空...");
        }

        //AI的模型ID，调用的AI的类别
        long modelId = 1659171950288818178L;

        //当接收请求之后，先修改执行状态为running,并修改数据库中的默认状态
        chart.setStatus(ChartStatusConstant.RUNNING);
        boolean b = chartService.updateById(chart);
        if (!b){
            channel.basicNack(deliverTag,false,false);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }

        //调用AI
        String userInput=buildInput(chart);
        System.out.println("构造的用户输入的信息为："+userInput);
        String s = aiManager.doChat(modelId,userInput);

        //将生成的结果拆分，分别为给出的结论，以及图表代码块
        String[] split = s.split("【【【【【");
        if (split.length<3){
            channel.basicNack(deliverTag,false,false);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"AI生成错误...");
        }

        //分别取出代码和结论
        String genChart = split[1].trim();
        String genResult = split[2].trim();
        Chart chart2=new Chart();
        chart2.setId(chart.getId());
        chart2.setGenChart(genChart);
        chart2.setGenResult(genResult);
        chart2.setStatus(ChartStatusConstant.SUCCESS);
        boolean b1 = chartService.updateById(chart2);
        if (!b1){
            channel.basicNack(deliverTag,false,false);
            //更新数据库数据失败
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }

        channel.basicAck(deliverTag,false);  //手动应答消息
    }

    /**
     * 构造用户的输入
     * @param chart
     * @return
     */
    private String buildInput(Chart chart){
        String goal=chart.getGoal();
        String chartType= chart.getChartType();
        String cvsData=chart.getChartData();

        StringBuilder userInput = new StringBuilder();
        String userGoal = goal;
        if (StringUtils.isNotBlank(chartType)) {
            userGoal += ",请使用" + chartType;

        }
        userInput.append("分析需求:").append(userGoal).append("\n");
        userInput.append("原始数据:").append("\n");
        userInput.append(cvsData).append("\n");

        return userInput.toString();
    }

}
