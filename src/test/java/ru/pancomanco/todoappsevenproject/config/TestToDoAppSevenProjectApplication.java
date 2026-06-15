package ru.pancomanco.todoappsevenproject.config;

import org.springframework.boot.SpringApplication;
import ru.pancomanco.todoappsevenproject.ToDoAppSevenProjectApplication;

public class TestToDoAppSevenProjectApplication {

    public static void main(String[] args) {
        SpringApplication.from(ToDoAppSevenProjectApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
