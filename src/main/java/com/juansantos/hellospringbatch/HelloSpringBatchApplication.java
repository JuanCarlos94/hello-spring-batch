package com.juansantos.hellospringbatch;



import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@EnableBatchProcessing
@SpringBootApplication
public class HelloSpringBatchApplication {

	@Autowired
	private JobBuilderFactory jobBuilderFactory;

	@Autowired
	private StepBuilderFactory stepBuilderFactory;

	@Bean
	public Job job(){
		return this.jobBuilderFactory.get("chunkBasedJob")
			.start(step1())
			.build();
	}

	@Bean
	public Step step1(){
		return this.stepBuilderFactory.get("chunkStep")
			.<String, String>chunk(1000)
			.reader(itemReader())
			.writer(itemWriter())
			.build();
	}

	@Bean
	@StepScope
	public ListItemReader<String> itemReader(){
		List<String> items = new ArrayList<>(100000);
		for(int i=0;i<100000;i++){
			items.add(UUID.randomUUID().toString());
		}
		return new ListItemReader<>(items);
	}

	@Bean
	public ItemWriter<String> itemWriter(){
		return items -> {
			for(String item : items){
				System.out.println(">> current item = " + item);
			}
		};
	}

	public static void main(String[] args) {
		SpringApplication.run(HelloSpringBatchApplication.class, args);
	}

}
