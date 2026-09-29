package com.futureboundtech.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    /** Publicly renderable asset folders served straight from disk (no ownership needed). */
    private static final String[] PUBLIC_UPLOAD_FOLDERS = {
            "branding", "courses", "trainers", "avatars"
    };

    @Value("${app.upload-dir:./uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Only the public sub-folders are exposed. Private uploads (lessons,
        // assignments, submissions) live in a separate directory that is never
        // mapped here, so they cannot be fetched by guessing a URL.
        Path location = Paths.get(uploadDir).toAbsolutePath().normalize();
        for (String folder : PUBLIC_UPLOAD_FOLDERS) {
            registry.addResourceHandler("/uploads/" + folder + "/**")
                    .addResourceLocations("file:" + location.resolve(folder) + "/");
        }
    }
}
