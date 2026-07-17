package ru.pancomanco.authservice;

import org.springframework.boot.SpringApplication;
import ru.pancomanco.authservice.config.TestcontainersConfiguration;

public class TestAuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.from(TestAuthServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
