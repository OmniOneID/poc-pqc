/*
 * Copyright 2025 OmniOne.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.omnione.did.base.aop;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.omnione.did.base.observer.ObserverClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.text.MessageFormat;
import java.time.OffsetDateTime;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.TimeZone;

/**
 * The ControllerLogAspects class provides methods for logging controller requests and responses.
 * This class is intended to help in debugging and monitoring the flow of data in and out of the controllers.
 */
@Slf4j
@Aspect
@Component
public class ControllerLogAspects {

    private final ObserverClient observerClient;

    public ControllerLogAspects(ObserverClient observerClient) {
        this.observerClient = observerClient;
    }

    /**
     * This method logs the controller requests and responses.
     * It is designed to log controller requests and responses to ensure that the application is secure and protected from unauthorized access.
     * @param joinPoint the ProceedingJoinPoint object
     * @return the result of the controller request
     * @throws Throwable the exception
     */
    @Around(value = "execution(* org.omnione.did..*Controller.*(..))")
    public Object requestChecker(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String httpMethod = "";
        String uri = "";

        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            httpMethod = request.getMethod();
            uri = request.getRequestURI();
        }

        debugControllerLog("{0}.{1} - Start", new Object[]{className, methodName});
        String requestData = convertToJsonString(joinPoint.getArgs());
        debugControllerLog("{0}.{1} - requestData : {2}", new Object[]{className, methodName, requestData});
        observerClient.send("req", httpMethod, uri, requestData, OffsetDateTime.now());

        try {
            Object result = joinPoint.proceed();
            Object body = result instanceof ResponseEntity<?> responseEntity ? responseEntity.getBody() : result;
            String responseData = convertToJsonString(body);
            debugControllerLog("{0}.{1} - responseData: {2}", new Object[]{className, methodName, responseData});
            debugControllerLog("{0}.{1} - finish", new Object[]{className, methodName});
            observerClient.send("res", httpMethod, uri, responseData, OffsetDateTime.now());
            return result;
        } catch (Throwable throwable) {
            String errorMessage = throwable.getMessage() == null ? throwable.getClass().getSimpleName() : throwable.getMessage();
            observerClient.send("err", httpMethod, uri, errorMessage, OffsetDateTime.now());
            throw throwable;
        }
    }

    /**
     * Configures an ObjectMapper instance for JSON processing.
     *
     * This includes settings for sorting properties alphabetically,
     * excluding null values, handling Java time types, and formatting dates in UTC.
     */
    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
            .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
            .build()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
            .registerModule(new JavaTimeModule())
            .setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'"))
            .setTimeZone(TimeZone.getTimeZone(ZoneId.of("UTC")));

    /**
     * Converts the provided object to a JSON string.
     * If the object is an array, it checks for MultipartFile instances and replaces them
     * with a string representation including the file name and size.
     *
     * @param msg the object to convert
     * @return the JSON string representation of the object
     * @throws JsonProcessingException if there is a problem processing the object to JSON
     */
    public String convertToJsonString(final Object msg) throws JsonProcessingException {
        if (msg instanceof Object[]) {
            Object[] args = (Object[]) msg;
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof MultipartFile) {
                    MultipartFile file = (MultipartFile) args[i];
                    args[i] = String.format("MultipartFile[name=%s, size=%d]", file.getOriginalFilename(), file.getSize());
                }
            }
        }
        return OBJECT_MAPPER.writeValueAsString(msg).trim();
    }

    /**
     * Logs the controller requests and responses using the specified pattern and arguments.
     * This method formats the log message according to the given pattern and logs it at the debug level.
     *
     * @param pattern the log pattern
     * @param args the log arguments
     */
    public static void debugControllerLog(String pattern, Object[] args) {
        try {
            String formattedMessage = MessageFormat.format(pattern, args);
            log.debug(formattedMessage);
        } catch (IllegalArgumentException | NullPointerException e) {
            log.error("IllegalArgumentException | NullPointerException : {}", e.getMessage());
        }
    }

}
