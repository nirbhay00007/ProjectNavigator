package com.nirbhay.repo_arc_navigator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RepoArcNavigatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(RepoArcNavigatorApplication.class, args);
    }

}
