package com.github.ericjunq.Clinica_Viene;

import org.springframework.boot.SpringApplication;

public class TestClinicaVieneApplication {

	public static void main(String[] args) {
		SpringApplication.from(ClinicaVieneApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
