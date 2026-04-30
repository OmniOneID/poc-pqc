package org.omnione.did.poc.pqc.server.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.omnione.did.poc.pqc.server.util.LogStreamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class ControllerLoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(ControllerLoggingAspect.class);
    private final ObjectMapper objectMapper;
    private final LogStreamService logStreamService;

    public ControllerLoggingAspect(ObjectMapper objectMapper, LogStreamService logStreamService) {
        this.objectMapper = objectMapper;
        this.logStreamService = logStreamService;
    }

    @Around("execution(* org.omnione.did.poc.pqc.server.controller..*(..)) " +
            "&& !within(org.omnione.did.poc.pqc.server.controller.GlobalExceptionHandler)")
    public Object logInputOutput(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();

        String httpMethod = "";
        String uri = "";
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            httpMethod = request.getMethod();
            uri = request.getRequestURI();
        }

        if (uri.equals("/poc") || uri.startsWith("/api/log-stream") || uri.startsWith("/api/log-blacklist")
                || logStreamService.isBlacklisted(uri)) {
            return joinPoint.proceed();
        }

        Object[] args = joinPoint.getArgs();
        StringBuilder params = new StringBuilder();
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                if (i > 0) params.append(", ");
                params.append(toJson(args[i]));
            }
        }

        String reqLog = "[REQ] " + httpMethod + " " + uri + " | " + params;
        log.info(reqLog);
        logStreamService.broadcast("req", reqLog);

        try {
            Object result = joinPoint.proceed();
            Object body = (result instanceof ResponseEntity) ? ((ResponseEntity<?>) result).getBody() : result;
            String resBody = toJson(body);
            String resLog = "[RES] " + httpMethod + " " + uri + " | " + resBody;
            log.info(resLog);
            logStreamService.broadcast("res", resLog);
            return result;
        } catch (Throwable ex) {
            String errLog = "[ERR] " + httpMethod + " " + uri + " | " + ex.getMessage();
            log.error(errLog);
            logStreamService.broadcast("err", errLog);
            throw ex;
        }
    }

    private String toJson(Object obj) {
        if (obj == null) return "null";
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return obj.toString();
        }
    }
}
