package ru.pancomanco.taskservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import ru.pancomanco.taskservice.properties.InternalProperties;

@SpringBootApplication
@EnableConfigurationProperties(InternalProperties.class)
public class TaskServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaskServiceApplication.class, args);
    }

}
