package ru.pancomanco.todoappsevenproject;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ToDoAppSevenProjectApplication {

    public static void main(String[] args) {
        SpringApplication.run(ToDoAppSevenProjectApplication.class, args);
    }
    // before restructure
}
