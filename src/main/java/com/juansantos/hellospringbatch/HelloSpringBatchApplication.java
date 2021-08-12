package com.juansantos.hellospringbatch;




import java.util.Arrays;
import java.util.List;

import com.juansantos.hellospringbatch.models.Customer;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;

@EnableBatchProcessing
@SpringBootApplication
public class HelloSpringBatchApplication {

	@Autowired
	private StepBuilderFactory stepBuilderFactory;

	@Autowired
	private JobBuilderFactory jobBuilderFactory;

	@Bean
	@StepScope
	public FlatFileItemReader<Customer> customerItemReader(@Value("#{jobParameters['customerFile']}") Resource inputFile){
		return new FlatFileItemReaderBuilder<Customer>()
			.name("customerItemReader")
			.delimited()
			.names(new String[] {"firstName",
				"middleInitial",
				"lastName",
				"addressNumber",
				"street",
				"city",
				"state",
				"zipCode"
			})
			.fieldSetMapper(new CustomerFieldSetMapper())
			.resource(inputFile)
			.build();
	}

	@Bean
	public ItemWriter<Customer> customerItemWriter(){
		return (items) -> items.forEach(System.out::println);
	}

	@Bean
	public Step step(){
		return this.stepBuilderFactory.get("readDelimiterFile")
			.<Customer, Customer>chunk(10)
			.reader(customerItemReader(null))
			.writer(customerItemWriter())
			.build();
	}

	@Bean
	public Job job(){
		return this.jobBuilderFactory.get("jobReadDelimiterFile")
			.start(step())
			.build();
	}
	
	public static void main(String[] args) {
		List<String> realArgs = Arrays.asList("customerFile=customer.csv");
		SpringApplication.run(HelloSpringBatchApplication.class, realArgs.toArray(new String[1]));
	}

}
