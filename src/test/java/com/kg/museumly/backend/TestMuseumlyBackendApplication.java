package com.kg.museumly.backend;

import org.springframework.boot.SpringApplication;

public class TestMuseumlyBackendApplication {

	public static void main(String[] args) {
		SpringApplication.from(MuseumlyBackendApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
