package com.juansantos.hellospringbatch;


import java.util.Collections;

import com.juansantos.hellospringbatch.models.Customer;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Sort;

@EnableBatchProcessing
@SpringBootApplication
public class HelloSpringBatchApplication implements CommandLineRunner {

	@Autowired
	private JobBuilderFactory jobBuilderFactory;

	@Autowired
	private StepBuilderFactory stepBuilderFactory;

	@Bean
	@StepScope
	public RepositoryItemReader<Customer> customerItemReader(CustomerRepository repository, @Value("#{jobParameters['city']}") String city){
		return new RepositoryItemReaderBuilder<Customer>()
			.name("customerItemReader")
			.arguments(Collections.singletonList(city))
			.methodName("findByCity")
			.repository(repository)
			.sorts(Collections.singletonMap("lastName", Sort.Direction.ASC))
			.build();
	}

	@Bean
	public Step step(){
		return this.stepBuilderFactory.get("step")
			.<Customer, Customer>chunk(10)
			.reader(customerItemReader(null, null))
			.writer(itemWriter())
			.build();
	}

	@Bean
	public Job job(){
		return this.jobBuilderFactory.get("job")
			.incrementer(new RunIdIncrementer())
			.start(step())
			.build();
	}

	@Bean
	public ItemWriter<Customer> itemWriter(){
		return (items) -> items.forEach(System.out::println);
	}
	
	public static void main(String[] args) {
		SpringApplication.run(HelloSpringBatchApplication.class, args);
	}

	@Autowired
	private JobLauncher jobLauncher;

	@Autowired
	private JobExplorer jobExplorer;

	@Override
	public void run(String... args) throws Exception {
		JobParameters jobParameters = new JobParametersBuilder(jobExplorer)
			.addString("city", "Springfield")
			.getNextJobParameters(job())
			.toJobParameters();
		jobLauncher.run(job(), jobParameters);
	}

}
