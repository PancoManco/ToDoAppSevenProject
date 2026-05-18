package ru.pancomanco.todoappsevenproject;

import org.springframework.boot.SpringApplication;

public class TestToDoAppSevenProjectApplication {

    public static void main(String[] args) {
        SpringApplication.from(ToDoAppSevenProjectApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
