package ru.pancomanco.todoappsevenproject;

import org.springframework.boot.SpringApplication;
import ru.pancomanco.todoappsevenproject.config.TestcontainersConfiguration;

public class TestToDoAppSevenProjectApplication {

    public static void main(String[] args) {
        SpringApplication.from(ToDoAppSevenProjectApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
