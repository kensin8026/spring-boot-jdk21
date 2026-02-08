package com.kiro.jdk21.batch.config;

import com.kiro.jdk21.core.application.port.in.TeamUseCase;
import com.kiro.jdk21.core.domain.Team;
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
@ConditionalOnProperty(name = "spring.batch.job.name", havingValue = "teamReportJob")
@RequiredArgsConstructor
public class TeamReportJobConfig {

	private final TeamUseCase teamUseCase;

	@Bean
	public Job teamReportJob(JobRepository jobRepository, Step teamReportStep) {
		return new JobBuilder("teamReportJob", jobRepository)
				.start(teamReportStep)
				.build();
	}

	@Bean
	public Step teamReportStep(JobRepository jobRepository, 
							   PlatformTransactionManager transactionManager) {
		return new StepBuilder("teamReportStep", jobRepository)
				.<Team, Team>chunk(10, transactionManager)
				.reader(teamItemReader())
				.processor(teamItemProcessor())
				.writer(teamItemWriter())
				.build();
	}

	@Bean
	public ItemReader<Team> teamItemReader() {
		return new ListItemReader<>(teamUseCase.getTeams());
	}

	@Bean
	public ItemProcessor<Team, Team> teamItemProcessor() {
		return team -> {
			log.info("Processing Team: {} - {}", team.name(), team.description());
			return team;
		};
	}

	@Bean
	public ItemWriter<Team> teamItemWriter() {
		return chunk -> {
			log.info("Writing {} teams to report", chunk.size());
			chunk.getItems().forEach(team -> 
				log.info("Team Report: {} - {} (Created: {})", 
						team.name(), team.description(), team.createdAt())
			);
		};
	}
}
