package com.hanhome.youtube_comments.logging;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

@Aspect
@Component
@Slf4j
public class RequestLoggingAspect {
    @Pointcut("execution(* com.hanhome.youtube_comments..service..*(..))")
    public void servicePointcut() {}

    @Pointcut(
            "execution(* com.hanhome.youtube_comments.oauth..*(..)) || " +
            "execution(* com.hanhome.youtube_comments.redis.service..*(..))"
    )
    public void notIncludeExecution() {}

    @Around("servicePointcut() && !notIncludeExecution()")
    public Object aroundLog(ProceedingJoinPoint joinPoint) throws Throwable {
        ServletRequestAttributes requestAttrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        String sessionId = "";
        if (requestAttrs != null) {
            HttpServletRequest request = requestAttrs.getRequest();
            HttpSession session = request.getSession(false);
            if (session != null) {
                sessionId = session.getId();
            }
        }
        sessionId = maskSensitiveData(sessionId);

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        String className = method.getDeclaringClass().getSimpleName();
        String methodName = method.getName();
        String callFunc = className + "." + methodName;

        long start = System.currentTimeMillis();
        log.info("method started - {}, session - {}", callFunc, sessionId);
        try {
            Object returnObj = joinPoint.proceed();
            Object printObj = null;
            if (returnObj instanceof ResponseEntity) {
                ResponseEntity<?> responseEntity = (ResponseEntity<?>) returnObj;
                printObj = responseEntity.getBody();
            } else {
                printObj = returnObj;
            }
            log.info("return value   - {}, session - {}", printObj, sessionId);
            return returnObj;
        } catch (Throwable e) {
            log.error("error  occurred - {} in {}, session - {}", e, callFunc, sessionId);
            throw e;
        } finally {
            long end = System.currentTimeMillis();
            log.info("method ended   - {}, in {}, session - {}", callFunc, (end - start) / (float) 1000, sessionId);
        }
    }

    private String maskSensitiveData(String data) {
        int paddingLen = 3;
        String masked = "*".repeat(4);
        if (data == null || data.length() < paddingLen * 2) {
            return masked + "*".repeat(paddingLen * 2);
        }
        return data.substring(0, paddingLen) + "****" + data.substring(data.length() - paddingLen);
    }
}
