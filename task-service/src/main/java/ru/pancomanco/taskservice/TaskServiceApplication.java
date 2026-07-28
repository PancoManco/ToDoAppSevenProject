package ru.pancomanco.taskservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import ru.pancomanco.taskservice.properties.InternalProperties;
import ru.pancomanco.taskservice.properties.JwtProperties;

@SpringBootApplication
@EnableConfigurationProperties({InternalProperties.class, JwtProperties.class})
public class TaskServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaskServiceApplication.class, args);
    }

}
