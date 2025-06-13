package com.hanhome.youtube_comments.member.batch;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hanhome.youtube_comments.exception.YoutubeAccessForbiddenException;
import com.hanhome.youtube_comments.member.dao.DeleteTargetMemberDAO;
import com.hanhome.youtube_comments.member.entity.Member;
import com.hanhome.youtube_comments.member.object.MemberRole;
import com.hanhome.youtube_comments.member.repository.MemberRepository;
import com.hanhome.youtube_comments.utils.AESUtil;
import jakarta.persistence.EntityManagerFactory;
import lombok.Getter;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class YoutubeUnwarrantedMemberDetectJob {
    private final EntityManagerFactory entityManagerFactory;
    private final PlatformTransactionManager transactionManager;
    private final JobRepository jobRepository;
    private final MemberRepository memberRepository;
    private final AESUtil aesUtil;
    private final RestTemplate restTemplate;

    private final static String googleOAuthTokenInfoUrl = "https://www.googleapis.com/oauth2/v1/tokeninfo?access_token={accessToken}";
    private final static String googleOAuthRenewAccessTokenUrl = "https://oauth2.googleapis.com/token";
    private final static String youtubeScope = "youtube.force-ssl";

    private final Map<String, Object> baseBody;
    private final static Integer maxChunkPage = 100;

    public YoutubeUnwarrantedMemberDetectJob(
        EntityManagerFactory entityManagerFactory,
        PlatformTransactionManager transactionManager,
        JobRepository jobRepository,
        MemberRepository memberRepository,
        AESUtil aesUtil,
        @Value("${spring.security.oauth2.client.registration.google.client-id}") String googleClientId,
        @Value("${spring.security.oauth2.client.registration.google.client-secret}") String googleClientSecret
    ) {
        this.entityManagerFactory = entityManagerFactory;
        this.transactionManager = transactionManager;
        this.jobRepository = jobRepository;
        this.memberRepository = memberRepository;
        this.aesUtil = aesUtil;
        this.restTemplate = new RestTemplate();

        baseBody = new HashMap<>();
        baseBody.put("grant_type", "refresh_token");
        baseBody.put("client_id", googleClientId);
        baseBody.put("client_secret", googleClientSecret);
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<Member> memberItemReader() {
        Map<String, Object> parameterValues = new HashMap<>();
        parameterValues.put("cutoffTime", Instant.now().atZone(ZoneOffset.UTC).toLocalDateTime().minusMinutes(30));
        parameterValues.put("role", MemberRole.ADMIN);

        return new JpaPagingItemReaderBuilder<Member>()
                .name("memberReader")
                .entityManagerFactory(entityManagerFactory)
                .pageSize(maxChunkPage)
                .queryString("SELECT m FROM Member m WHERE m.createdAt <= :cutoffTime and m.role != :role ORDER BY m.createdAt ASC")
                .parameterValues(parameterValues)
                .build();
    }

    @Bean
    public ItemProcessor<Member, DeleteTargetMemberDAO> memberItemProcessor() {
        return member -> {
            Map<String, Object> requestBody = new HashMap<>(baseBody);
            String googleAccessToken = null;
            try {
                String googleRefreshToken = aesUtil.decrypt(member.getGoogleRefreshToken());
                requestBody.put("refresh_token", googleRefreshToken);
                googleAccessToken = restTemplate.postForObject(
                        googleOAuthRenewAccessTokenUrl,
                        requestBody,
                        RenewAccessTokenResponse.class
                ).getAccessToken();

                OAuthTokenScopesResponse scopeResponse = restTemplate.getForObject(
                        googleOAuthTokenInfoUrl,
                        OAuthTokenScopesResponse.class,
                        googleAccessToken
                );
                assert scopeResponse != null;
                if (!scopeResponse.hasYoutubeAccess()) throw new YoutubeAccessForbiddenException("Youtube Access not Granted - req");
                return null;
            } catch (YoutubeAccessForbiddenException e) {
                restTemplate.postForLocation("https://oauth2.googleapis.com/revoke?token=" + googleAccessToken, null);
                return member.toBatchDao();
            } catch (Exception e) {
                return member.toBatchDao();
            }
        };
    }

    @Bean
    public ItemWriter<DeleteTargetMemberDAO> memberItemWriter() {
        return members -> {
            for (DeleteTargetMemberDAO targetMember : members) {
                memberRepository.deleteById(targetMember.getId());
            }
        };
    }

    @Bean
    public Step checkUnwarrantedMemberStep(
            ItemReader<Member> memberItemReader,
            ItemProcessor<Member, DeleteTargetMemberDAO> memberItemProcessor,
            ItemWriter<DeleteTargetMemberDAO> memberItemWriter
    ) {
        return new StepBuilder("checkUnwarrantedMemberStep", jobRepository)
                .<Member, DeleteTargetMemberDAO>chunk(maxChunkPage, transactionManager)
                .reader(memberItemReader)
                .processor(memberItemProcessor)
                .writer(memberItemWriter)
                .build();
    }

    @Bean
    public Job removeUnlinkedMemberJob(@Qualifier("checkUnwarrantedMemberStep") Step processMemberStep) {
        return new JobBuilder("removeUnlinkedMemberJob", jobRepository)
                .start(processMemberStep)
                .build();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    private static class OAuthTokenScopesResponse {
        private String scope;

        public boolean hasYoutubeAccess() {
            return Arrays.stream(scope.split(" "))
                    .anyMatch(s -> s.endsWith(youtubeScope));
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    private static class RenewAccessTokenResponse {
        @JsonProperty("access_token")
        private String accessToken;
    }
}