package com.juansantos.hellospringbatch;



import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
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
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.item.file.mapping.PassThroughFieldSetMapper;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;

@EnableBatchProcessing
@SpringBootApplication
public class HelloSpringBatchApplication implements CommandLineRunner {

	@Autowired
	private JobBuilderFactory jobBuilderFactory;

	@Autowired
	private StepBuilderFactory stepBuilderFactory;
	
	@Autowired
	private JobLauncher jobLauncher;

	@Bean
	@StepScope
	public TransactionReader transactionReader(){
		return new TransactionReader(fileItemReader(null));
	}

	@Bean
	@StepScope
	public FlatFileItemReader<FieldSet> fileItemReader(@Value("#{jobParameters['transactionFile']}") Resource inputFile){
		return new FlatFileItemReaderBuilder<FieldSet>()
			.name("fileItemReader")
			.resource(inputFile)
			.lineTokenizer(new DelimitedLineTokenizer())
			.fieldSetMapper(new PassThroughFieldSetMapper())
			.build();
	}

	@Bean
	public JdbcBatchItemWriter<Transaction> transactionWriter(DataSource dataSource){
		return new JdbcBatchItemWriterBuilder<Transaction>()
			.itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>())
			.sql("INSERT INTO TRANSACTION " + 
				"(ACCOUNT_SUMMARY_ID, TIMESTAMP, AMOUNT) " + 
				"VALUES ((SELECT ID FROM ACCOUNT_SUMMARY " +
				" WHERE ACCOUNT_NUMBER = :accountNumber), " +
				":timestamp, :amount)")
			.dataSource(dataSource)
			.build();
	}

	@Bean
	public Step importTransactionFileStep(){
		return this.stepBuilderFactory.get("importTransactionFileStep")
			.allowStartIfComplete(true)
			.<Transaction, Transaction>chunk(100)
			.reader(transactionReader())
			.writer(transactionWriter(null))
			.allowStartIfComplete(true)
			.listener(transactionReader())
			.build();
	}

	@Bean
	@StepScope
	public JdbcCursorItemReader<AccountSummary> accountSummaryReader(DataSource dataSource){
		return new JdbcCursorItemReaderBuilder<AccountSummary>()
			.name("accountSummaryReader")
			.dataSource(dataSource)
			.sql("SELECT A.ACCOUNT_NUMBER, A.CURRENT_BALANCE " +
				"FROM ACCOUNT_SUMMARY A " +
				"WHERE A.ID IN (SELECT DISTINCT T.ACCOUNT_SUMMARY_ID FROM TRANSACTION T) " +
				"ORDER BY A.ACCOUNT_NUMBER")
			.rowMapper((resultSet, rowNumber) -> {
				AccountSummary summary = new AccountSummary();
				summary.setAccountNumber(resultSet.getString("account_number"));
				summary.setCurrentBalance(resultSet.getDouble("current_balance"));
				return summary;
			})
			.build();
	}

	@Bean
	public TransactionDao transactionDao(DataSource dataSource){
		return new TransactionDaoSupport(dataSource);
	}

	@Bean
	public TransactionApplierProcessor transactionApplierProcessor(){
		return new TransactionApplierProcessor(transactionDao(null));
	}

	@Bean
	public JdbcBatchItemWriter<AccountSummary> accountSummaryWriter(DataSource dataSource){
		return new JdbcBatchItemWriterBuilder<AccountSummary>()
			.dataSource(dataSource)
			.itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>())
			.sql("UPDATE ACCOUNT_SUMMARY " +
				"SET CURRENT_BALANCE = :currentBalance " +
				"WHERE ACCOUNT_NUMBER = :accountNumber")
				.build();
	}

	@Bean
	public Step applyTransactionStep(){
		return this.stepBuilderFactory.get("applyTransactionStep")
			.<AccountSummary, AccountSummary> chunk(100)
			.reader(accountSummaryReader(null))
			.processor(transactionApplierProcessor())
			.writer(accountSummaryWriter(null))
			.build();
	}

	@Bean
	@StepScope
	public FlatFileItemWriter<AccountSummary> accountSummaryFileWriter(@Value("#{jobParameters['summaryFile']}") Resource summaryFile){
		DelimitedLineAggregator<AccountSummary> lineAggregator = new DelimitedLineAggregator<>();
		BeanWrapperFieldExtractor<AccountSummary> fieldExtractor = new BeanWrapperFieldExtractor<>();
		fieldExtractor.setNames(new String[]{"accountNumber", "currentBalance"});
		fieldExtractor.afterPropertiesSet();
		lineAggregator.setFieldExtractor(fieldExtractor);
		return new FlatFileItemWriterBuilder<AccountSummary>()
			.name("accountSummaryFileWriter")
			.resource(summaryFile)
			.lineAggregator(lineAggregator)
			.build();
	}

	@Bean
	public Step generateAccountSummaryStep(){
		return this.stepBuilderFactory.get("generateAccountSummaryStep")
			.<AccountSummary, AccountSummary>chunk(100)
			.reader(accountSummaryReader(null))
			.writer(accountSummaryFileWriter(null))
			.build();
	}

	@Bean
	public Job transactionJob(){
		return this.jobBuilderFactory.get("transactionJob")
			.incrementer(new RunIdIncrementer())
			.start(importTransactionFileStep())
			.next(applyTransactionStep())
			.next(generateAccountSummaryStep())
			.build();
	}

	@Autowired
	JdbcTemplate jdbcTemplate;

	public static void main(String[] args) {
		SpringApplication.run(HelloSpringBatchApplication.class, args);
	}

	@Autowired
	private JobExplorer jobExplorer;

	@Override
	public void run(String... args) throws Exception {
		JobParameters jobParameters = new JobParametersBuilder(this.jobExplorer)
			.addString("transactionFile", "transactionFile.csv")
			.addString("summaryFile", "summary.csv")
			.getNextJobParameters(transactionJob())
			.toJobParameters();
		JobExecution jobExecution = jobLauncher.run(transactionJob(), jobParameters);
		System.out.println("STATUS :: " + jobExecution.getStatus());
	}

}
