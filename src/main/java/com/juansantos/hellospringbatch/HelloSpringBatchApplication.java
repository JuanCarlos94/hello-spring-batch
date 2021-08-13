package com.juansantos.hellospringbatch;


import com.juansantos.hellospringbatch.models.Customer;
import com.juansantos.hellospringbatch.models.Transaction;

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
import org.springframework.batch.item.xml.StaxEventItemReader;
import org.springframework.batch.item.xml.builder.StaxEventItemReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

@EnableBatchProcessing
@SpringBootApplication
public class HelloSpringBatchApplication implements CommandLineRunner{

	@Autowired
	private JobBuilderFactory jobBuilderFactory;

	@Autowired
	private StepBuilderFactory stepBuilderFactory;

	@Bean
	public Step copyFileStep(){
		return this.stepBuilderFactory.get("copyFileStep")
			.<Customer, Customer>chunk(10)
			.reader(costomerFileReader(null))
			.writer(itemWriter())
			.build();
	}

	@Bean
	public Job job(){
		return this.jobBuilderFactory.get("job")
			.incrementer(new RunIdIncrementer())
			.start(copyFileStep())
			.build();
	}

	@Bean
	@StepScope
	public StaxEventItemReader<Customer> costomerFileReader(@Value("#{jobParameters['customerFile']}") Resource inputFile){
		System.out.println("Resource: " + inputFile);
		return new StaxEventItemReaderBuilder<Customer>()
			.name("customerFileReader")
			.resource(inputFile)
			.addFragmentRootElements("customer")
			.unmarshaller(customerMarshaller())
			.build();
	}

	@Bean
	public Jaxb2Marshaller customerMarshaller(){
		Jaxb2Marshaller jaxb2Marshaller = new Jaxb2Marshaller();
		jaxb2Marshaller.setClassesToBeBound(Customer.class, Transaction.class);
		return jaxb2Marshaller;
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
		JobParameters params = new JobParametersBuilder(jobExplorer)
			.addString("customerFile", "customer.xml")
			.getNextJobParameters(job())
			.toJobParameters();
		jobLauncher.run(job(), params);
	}

}
