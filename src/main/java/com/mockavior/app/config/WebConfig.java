package com.mockavior.app.config;

import com.mockavior.app.http.RuntimeInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final RuntimeInterceptor runtimeInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        log.info("Initializing InterceptorRegistry");
        registry.addInterceptor(runtimeInterceptor)
                .addPathPatterns("/**");
    }
}
