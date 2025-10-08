package com.sysgepecole.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@SpringBootApplication
public class SysgespecoleApplication {

	public static void main(String[] args) {
		SpringApplication.run(SysgespecoleApplication.class, args);
	}
	
	@Bean
	public WebMvcConfigurer corsConfigurer() {
	return new WebMvcConfigurer() {
		@Override
		 public void addCorsMappings(CorsRegistry registry) {
            registry.addMapping("/**")
                .allowedOrigins("*")  
                .allowedMethods("HEAD", "OPTIONS", "GET", "POST", "PUT", "PATCH", "DELETE")
                .allowCredentials(false)
                .maxAge(3600);
        }
	};
}
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
	        registry.addResourceHandler("/logos/**")
	        .addResourceLocations("file:///C:/logos/");
	        
	        registry.addResourceHandler("/uploads/**")
	        .addResourceLocations("file:///C:/uploads/");
	    }

}
