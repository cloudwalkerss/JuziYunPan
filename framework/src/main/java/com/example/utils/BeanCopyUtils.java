package com.example.utils;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class BeanCopyUtils {
    private BeanCopyUtils(){



    }
    public <V> V copyBean(Object source ,Class<V> clazz){
        V result=null;
        //创建目标对象

        try {
            result=clazz.newInstance();

            BeanUtils.copyProperties(source,result);
        } catch (Exception e) {
            e.printStackTrace();
        }

        //实现属性copy

        //返回结果

        return  result;
    }

    public  <O,V>List<V> copyBeanList(List<O>list,Class<V> clazz){
        //创建链表
        List<V>result=new ArrayList<>();

        //遍历list，每个元素调用copyBean方法，然后添加到链表中
        for(O obj:list){
            V v=copyBean(obj,clazz);
            result.add(v);
        }
        //返回链表
        return result;
    }


}
