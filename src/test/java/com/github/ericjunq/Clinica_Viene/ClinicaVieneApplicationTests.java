package com.github.ericjunq.Clinica_Viene;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ClinicaVieneApplicationTests {

	@Test
	void contextLoads() {
	}

}
