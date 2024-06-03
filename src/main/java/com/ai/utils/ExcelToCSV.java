package com.ai.utils;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.support.ExcelTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * excel文件转csv文件工具类
 */
@Slf4j
public class ExcelToCSV {
    public static String ExcelTocsv(MultipartFile multipartFile) {
        List<Map<Integer, String>> list = null;
        try {
            list = EasyExcel.read(multipartFile.getInputStream())
                    .excelType(ExcelTypeEnum.XLSX)
                    .sheet()
                    .headRowNumber(0)
                    .doReadSync();
        } catch (IOException e) {
            log.error("表格处理错误");
            throw new RuntimeException(e);
        }
        System.out.println(list);
        if (CollUtil.isEmpty(list)){
            return "";
        }

        //读取表头
        //转换为csv
        StringBuilder stringBuilder=new StringBuilder();
        LinkedHashMap<Integer, String> integerStringMap = (LinkedHashMap)list.get(0);
        List<String> headlist=integerStringMap.values().stream().filter(ObjectUtils::isNotEmpty).collect(Collectors.toList());
        stringBuilder.append(StringUtils.join(headlist,",")).append("\n");

        //读取数据
        for (int i = 1; i < list.size() ; i++) {
            LinkedHashMap<Integer, String> dataMap = (LinkedHashMap)list.get(i);
            List<String> datalist=dataMap.values().stream().filter(ObjectUtils::isNotEmpty).collect(Collectors.toList());
            stringBuilder.append(StringUtils.join(datalist,",")).append("\n");
        }
        return stringBuilder.toString();
    }

}
