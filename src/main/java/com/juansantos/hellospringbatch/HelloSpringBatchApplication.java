package com.juansantos.hellospringbatch;


import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.listener.ExecutionContextPromotionListener;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

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
	public Step stepPrintParams(){
		return this.stepBuilderFactory.get("stepPrintParams")
			.tasklet(this.printCommandLineParams(null, null))
			.build();
	}

	@Bean
	public Step stepPrintHelloWorld() {
		return this.stepBuilderFactory.get("step1").tasklet(new Tasklet() {
			@Override
			public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
				System.out.println("Hello, World!");
				return RepeatStatus.FINISHED;
			}
		}).build();
	}

	@Bean
	public Step stepHelloContext(){
		return this.stepBuilderFactory.get("stepManipulatintExecutionContext")
			.tasklet(new HelloTasklet())
			.build();
	}

	@Bean
	public Step stepPassingPromotionKeys(){
		return this.stepBuilderFactory.get("stepPassingPromotionKeys")
			.tasklet(new HelloTasklet())
			.build();
	}

	@Bean
	public Step stepPassingPromotionKeys2(){
		return this.stepBuilderFactory.get("stepPassingPromotionKeys2")
			.tasklet(new GoodByeTasklet())
			.build();
	}

	@Bean 
	@StepScope
	public Tasklet printCommandLineParams(@Value("#{jobParameters['filename']}") String filename, @Value("#{jobParameters['name']}") String name){
		return (contribution, context) -> {
			System.out.println(String.format("Hello, %s!", name));
			System.out.println(String.format("fileName = %s", filename));
			return RepeatStatus.FINISHED;
		};
	}

	@Bean
	public Job job(){
		return this.jobBuilderFactory.get("job")
			.start(this.stepPassingPromotionKeys())
			.next(this.stepPassingPromotionKeys2())
			.listener(this.promotionListener())
			.build();
	}

	@Bean
	public StepExecutionListener promotionListener(){
		ExecutionContextPromotionListener listener = new ExecutionContextPromotionListener();
		listener.setKeys(new String[]{"nam"});
		return listener;
	}

	public static void main(String[] args) {
		SpringApplication.run(HelloSpringBatchApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		
		 // Pass the required Job Parameters from here to read it anywhere within Spring Batch infrastructure
		JobParameters jobParameters = new JobParametersBuilder().addString("filename", "teste.csv")
                .addString("name", "Juan").toJobParameters();

		JobExecution execution = jobLauncher.run(job(), jobParameters);
		System.out.println("STATUS :: "+execution.getStatus());
	}

}
