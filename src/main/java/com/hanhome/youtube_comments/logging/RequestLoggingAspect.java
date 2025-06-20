package com.hanhome.youtube_comments.logging;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class RequestLoggingAspect {
    private final HttpServletRequest request;

    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
    public void controllerPointcut() {}

    @Pointcut("execution(* com.hanhome.youtube_comments..service..*(..))")
    public void servicePointcut() {}

    @Pointcut(
            "execution(* com.hanhome.youtube_comments.oauth..*(..)) || " +
            "execution(* com.hanhome.youtube_comments.redis.service..*(..))"
    )
    public void notIncludeExecution() {}

    @Before("controllerPointcut()")
    public void logBefore(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodName = signature.getName();
        String className = signature.getDeclaringTypeName();
        String httpMethod = request.getMethod();
        String requestURI = request.getRequestURI();

        log.info("[Request] {} {} - {}.{}", httpMethod, requestURI, className, methodName);
    }

    @Around("servicePointcut() && !notIncludeExecution()")
    public Object aroundLog(ProceedingJoinPoint joinPoint) throws Throwable {
        String sessionId = "";
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servletRequestAttributes) {
            HttpServletRequest request = servletRequestAttributes.getRequest();
            HttpSession session = request.getSession(false);
            if (session != null) sessionId = session.getId();
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
            if (returnObj instanceof ResponseEntity<?> responseEntity) {
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
