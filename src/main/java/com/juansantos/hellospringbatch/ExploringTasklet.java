package com.juansantos.hellospringbatch;

import java.util.List;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

public class ExploringTasklet implements Tasklet {
    private JobExplorer explorer;

    public ExploringTasklet(JobExplorer explorer){
        this.explorer = explorer;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        String jobName = chunkContext.getStepContext().getJobName();
        List<JobInstance> instances = explorer.getJobInstances(jobName, 0, Integer.MAX_VALUE);
        
        System.out.println(String.format("There are %d job instances for the job %s", instances.size(), jobName));

        System.out.println("They have had the following results");
        System.out.println("***********************************");

        for(JobInstance instance : instances){
            List<JobExecution> jobExecutions = this.explorer.getJobExecutions(instance);
            System.out.println(String.format("Instance %d had %d executions", instance.getInstanceId(), jobExecutions.size()));
        }
        return RepeatStatus.FINISHED;
    }
}
