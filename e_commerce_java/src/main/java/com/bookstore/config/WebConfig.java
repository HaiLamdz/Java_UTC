package com.bookstore.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    public static final String UPLOAD_DIR_NAME = "uploads";

    /**
     * Lấy đường dẫn thư mục lưu ảnh upload an toàn
     */
    public static Path getUploadPath() {
        Path userDir = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        Path uploadPath;
        if (userDir.getFileName() != null && userDir.getFileName().toString().equals("e_commerce_java")) {
            uploadPath = userDir.resolve(UPLOAD_DIR_NAME);
        } else if (Files.exists(userDir.resolve("e_commerce_java"))) {
            uploadPath = userDir.resolve("e_commerce_java").resolve(UPLOAD_DIR_NAME);
        } else {
            uploadPath = userDir.resolve(UPLOAD_DIR_NAME);
        }

        try {
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return uploadPath;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadPath = getUploadPath();
        String uploadUri = uploadPath.toUri().toString();
        if (!uploadUri.endsWith("/")) {
            uploadUri += "/";
        }

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadUri, "classpath:/static/uploads/");
    }
}
