package ru.rksp.mironov_consumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
public class MironovConsumerApplication {

	public static void main(String[] args) {
		SpringApplication.run(MironovConsumerApplication.class, args);
	}

}
