package com.example.sbb.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve files placed in the project root "image" folder (e.g., image/logo.png)
        registry.addResourceHandler("/image/**")
                .addResourceLocations("file:./image/", "classpath:/static/image/");
    }
}
