package com.ai.manager;


import com.ai.common.ErrorCode;
import com.ai.exception.BusinessException;
import com.yupi.yucongming.dev.client.YuCongMingClient;


import com.yupi.yucongming.dev.common.BaseResponse;
import com.yupi.yucongming.dev.model.DevChatRequest;
import com.yupi.yucongming.dev.model.DevChatResponse;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * ai调用类
 */
@Service
public class AIManager {
    @Resource
    private YuCongMingClient yuCongMingClient;

    /**
     * 调用AI的方法
     * @param modelID AI模型ID
     * @param message 用户输入的信息
     * @return  返回生成的内容
     */
    public String doChat(Long modelID,String message){
        DevChatRequest devChatRequest = new DevChatRequest();
        devChatRequest.setModelId(modelID);
        devChatRequest.setMessage(message);
        BaseResponse<DevChatResponse> response = yuCongMingClient.doChat(devChatRequest);
        if (response == null){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"AI响应错误");
        }
        String res=response.getData().getContent();
        System.out.println(res);
        return response.getData().getContent();
    }
}
