package ar.edu.eventos.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CoreEventosApplication {
    public static void main(String[] args) {
        SpringApplication.run(CoreEventosApplication.class, args);
    }
}
