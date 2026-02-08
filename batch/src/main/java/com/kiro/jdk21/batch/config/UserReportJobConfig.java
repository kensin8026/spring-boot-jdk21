package com.kiro.jdk21.batch.config;

import com.kiro.jdk21.core.application.port.in.UserUseCase;
import com.kiro.jdk21.core.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "spring.batch.job.name", havingValue = "userReportJob")
@RequiredArgsConstructor
public class UserReportJobConfig {

	private final UserUseCase userUseCase;

	@Bean
	public Job userReportJob(JobRepository jobRepository, Step userReportStep) {
		return new JobBuilder("userReportJob", jobRepository)
				.start(userReportStep)
				.build();
	}

	@Bean
	public Step userReportStep(JobRepository jobRepository, 
							   PlatformTransactionManager transactionManager) {
		return new StepBuilder("userReportStep", jobRepository)
				.<User, User>chunk(10, transactionManager)
				.reader(userItemReader())
				.processor(userItemProcessor())
				.writer(userItemWriter())
				.build();
	}

	@Bean
	public ItemReader<User> userItemReader() {
		return new ListItemReader<>(userUseCase.getUsers());
	}

	@Bean
	public ItemProcessor<User, User> userItemProcessor() {
		return user -> {
			log.info("Processing User: {} - {} (Team ID: {})", 
					user.name(), user.email(), user.teamId());
			return user;
		};
	}

	@Bean
	public ItemWriter<User> userItemWriter() {
		return chunk -> {
			log.info("Writing {} users to report", chunk.size());
			chunk.getItems().forEach(user -> 
				log.info("User Report: {} - {} (Team ID: {}, Created: {})", 
						user.name(), user.email(), user.teamId(), user.createdAt())
			);
		};
	}
}
