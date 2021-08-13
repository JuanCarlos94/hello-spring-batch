package com.juansantos.hellospringbatch;





import com.juansantos.hellospringbatch.models.Customer;

import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;

@EnableBatchProcessing
@SpringBootApplication
public class HelloSpringBatchApplication implements CommandLineRunner {

	@Bean
	@StepScope
	public FlatFileItemReader<Customer> customerItemReader(@Value("#{jobParameters['customerFile']}") Resource inputFile){
		return new FlatFileItemReaderBuilder<Customer>()
			.name("customerItemReader")
			.lineTokenizer(new CustomerFileLineTokenizer())
			.fieldSetMapper(new CustomerFieldSetMapper())
			.resource(inputFile)
			.build();
	}
	
	public static void main(String[] args) {
		SpringApplication.run(HelloSpringBatchApplication.class, args);
	}

	@Autowired
	private JobLauncher jobLauncher;

	@Override
	public void run(String... args){
		JobParameters jobParameters = new JobParametersBuilder()
			.addString("inputFile", "customers.csv")
			.toJobParameters();
		jobLauncher.run(job(), jobParameters);
	}

}
