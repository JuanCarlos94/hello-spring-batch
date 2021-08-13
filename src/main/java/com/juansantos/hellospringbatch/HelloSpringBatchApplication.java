package com.juansantos.hellospringbatch;


import javax.sql.DataSource;

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
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.ArgumentPreparedStatementSetter;

@EnableBatchProcessing
@SpringBootApplication
public class HelloSpringBatchApplication implements CommandLineRunner {

	@Autowired
	private JobBuilderFactory jobBuilderFactory;

	@Autowired
	private StepBuilderFactory stepBuilderFactory;

	@Bean
	public JdbcCursorItemReader<Customer> customerItemReader(DataSource dataSource){
		return new JdbcCursorItemReaderBuilder<Customer>()
			.name("customerItemReader")
			.dataSource(dataSource)
			.sql("select * from customer where city = ?")
			.rowMapper(new CustomerRowMapper())
			.preparedStatementSetter(citySetter(null))
			.build();
	}

	@Bean
	@StepScope
	public ArgumentPreparedStatementSetter citySetter(@Value("#{jobParameters['city']}") String city){
		return new ArgumentPreparedStatementSetter(new Object[]{city});
	}

	@Bean
	public Step step(){
		return this.stepBuilderFactory.get("step")
			.<Customer, Customer>chunk(10)
			.reader(customerItemReader(null))
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
