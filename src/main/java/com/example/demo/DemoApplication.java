package com.example.demo;

import com.example.demo.Filter.AuthFilter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @Bean
    public FilterRegistrationBean< AuthFilter> filterRegistrationBean() {

        System.out.println("com.example.demo.configuration.AppConfig.filterRegistrationBean()");
        System.out.println("APP CONFIG JAVA ");
        FilterRegistrationBean< AuthFilter> registrationBean = new FilterRegistrationBean();
        AuthFilter authFilter = new AuthFilter();

        registrationBean.setFilter(authFilter);
        registrationBean.addUrlPatterns("*");
        return registrationBean;
    }

}
