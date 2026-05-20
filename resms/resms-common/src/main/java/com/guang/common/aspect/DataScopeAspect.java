package com.guang.common.aspect;

import com.guang.common.annotation.DataScope;
import com.guang.common.security.DataScopeContext;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 数据权限过滤切面
 *
 * @author blackDuck
 */
@Aspect
@Component
@Slf4j
public class DataScopeAspect {

    @Pointcut("@annotation(com.guang.common.annotation.DataScope)")
    public void dataScopePointCut() {}

    @Before("dataScopePointCut()")
    public void doBefore(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        DataScope dataScope = method.getAnnotation(DataScope.class);
        if (dataScope != null) {
            DataScopeContext.set(dataScope);
        }
    }

    @After("dataScopePointCut()")
    public void doAfter() {
        DataScopeContext.clear();
    }
}
