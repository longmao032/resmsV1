package com.guang.common.aspect;

import cn.hutool.core.util.URLUtil;
import cn.hutool.json.JSONUtil;
import com.guang.common.annotation.Log;
import com.guang.common.security.LogDTO;
import com.guang.common.security.LoginUser;
import com.guang.common.util.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * 操作日志切面处理
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class LogAspect {

    private final ApplicationEventPublisher publisher;

    // 线程本地变量，用于统计耗时
    private static final ThreadLocal<Long> TIME_THREADLOCAL = new ThreadLocal<>();

    @Pointcut("@annotation(com.guang.common.annotation.Log)")
    public void logPointCut() {
    }

    @Before("logPointCut()")
    public void doBefore() {
        TIME_THREADLOCAL.set(System.currentTimeMillis());
    }

    @AfterReturning(pointcut = "logPointCut()", returning = "jsonResult")
    public void doAfterReturning(JoinPoint joinPoint, Object jsonResult) {
        handleLog(joinPoint, null, jsonResult);
    }

    @AfterThrowing(value = "logPointCut()", throwing = "e")
    public void doAfterThrowing(JoinPoint joinPoint, Exception e) {
        handleLog(joinPoint, e, null);
    }

    protected void handleLog(final JoinPoint joinPoint, final Exception e, Object jsonResult) {
        try {
            Log logAnnotation = getAnnotationLog(joinPoint);
            if (logAnnotation == null) {
                return;
            }

            // 获取当前请求
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) return;
            HttpServletRequest request = attributes.getRequest();

            // 封装日志对象
            LogDTO logDTO = new LogDTO();
            logDTO.setStatus(e == null ? (byte) 1 : (byte) 0);
            // 使用工具类获取客户端 IP
            logDTO.setIpAddress(com.guang.common.util.IpUtils.getClientIP(request));
            logDTO.setRequestUrl(URLUtil.getPath(request.getRequestURI()));
            logDTO.setRequestMethod(request.getMethod());
            logDTO.setUserAgent(request.getHeader("User-Agent"));
            logDTO.setOperationTime(LocalDateTime.now());
            
            // 计算耗时
            Long startTime = TIME_THREADLOCAL.get();
            if (startTime != null) {
                logDTO.setExecuteTime(System.currentTimeMillis() - startTime);
            }

            // 获取当前登录用户
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser) {
                logDTO.setUserId(loginUser.getUserId());
                logDTO.setUserName(loginUser.getUsername());
            }

            if (e != null) {
                logDTO.setErrorMsg(e.getMessage());
            }

            // 设置注解上的信息
            logDTO.setModule(logAnnotation.title());
            logDTO.setBusinessType(logAnnotation.businessType());
            logDTO.setOperationType(logAnnotation.operatorType());
            
            // 设置操作描述：优先使用注解定义的 description，若无则使用 title + operatorType 拼接
            String desc = logAnnotation.description();
            if (cn.hutool.core.util.StrUtil.isBlank(desc)) {
                desc = logAnnotation.title() + " " + logAnnotation.operatorType();
            }
            logDTO.setOperationDesc(desc);

            // 保存请求参数
            if (logAnnotation.isSaveRequestData()) {
                logDTO.setRequestParams(JSONUtil.toJsonStr(joinPoint.getArgs()));
            }

            // 保存响应结果
            if (logAnnotation.isSaveResponseData() && jsonResult != null) {
                logDTO.setResponseResult(JSONUtil.toJsonStr(jsonResult));
            }

            // 发布日志事件
            publisher.publishEvent(logDTO);

        } catch (Exception exp) {
            log.error("日志记录异常: {}", exp.getMessage());
        } finally {
            TIME_THREADLOCAL.remove();
        }
    }

    /**
     * 是否存在注解，如果存在就获取
     */
    private Log getAnnotationLog(JoinPoint joinPoint) {
        try {
            java.lang.reflect.Method method = ((org.aspectj.lang.reflect.MethodSignature) joinPoint.getSignature()).getMethod();
            if (method != null) {
                return method.getAnnotation(Log.class);
            }
        } catch (Exception e) {
            log.error("获取注解失败: {}", e.getMessage());
        }
        return null;
    }
}
