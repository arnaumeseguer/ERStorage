package com.erstorage.erstorageapi;

import org.springframework.boot.SpringApplication;

public class TestErStorageApplication {

    public static void main(String[] args) {
        SpringApplication.from(ErStorageApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
