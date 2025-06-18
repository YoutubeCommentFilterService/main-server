package com.hanhome.youtube_comments.member.batch;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hanhome.youtube_comments.google.object.youtube_data_api.channel.ChannelListResponse;
import com.hanhome.youtube_comments.google.object.youtube_data_api.channel.ChannelResource;
import com.hanhome.youtube_comments.google.service.GoogleAPIService;
import com.hanhome.youtube_comments.member.dao.MemberChannelIdDAO;
import com.hanhome.youtube_comments.member.entity.Member;
import com.hanhome.youtube_comments.member.repository.MemberRepository;
import jakarta.persistence.EntityManagerFactory;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.*;
import java.util.stream.Collectors;

@Configuration
public class MemberProfileUpdateJob {
    private final MemberRepository memberRepository;
    private final EntityManagerFactory entityManagerFactory;
    private final PlatformTransactionManager transactionManager;
    private final JobRepository jobRepository;
    private final GoogleAPIService googleAPIService;
    private final Map<String, Object> baseParam;
    private final ObjectMapper objectMapper;

    @Value("${data.keys.update-profile-job}")
    private String HASH_KEY;

    private static final int maxChunkPage = 50;

    public MemberProfileUpdateJob(
            MemberRepository memberRepository,
            EntityManagerFactory entityManagerFactory,
            PlatformTransactionManager transactionManager,
            GoogleAPIService googleAPIService,
            JobRepository jobRepository,
            ObjectMapper objectMapper,
            @Value("${spring.security.oauth2.client.registration.google.client-id}") String googleClientId,
            @Value("${spring.security.oauth2.client.registration.google.client-secret}") String googleClientSecret
    ) {
        this.memberRepository = memberRepository;
        this.entityManagerFactory = entityManagerFactory;
        this.transactionManager = transactionManager;
        this.googleAPIService = googleAPIService;
        this.jobRepository = jobRepository;
        this.objectMapper = objectMapper;

        baseParam = new HashMap<>();
        baseParam.put("part", "snippet");
    }

    @Bean
    public JpaPagingItemReader<Member> channelIdExistsMemberItemReader() {
        return new JpaPagingItemReaderBuilder<Member>()
                .name("memberReader")
                .entityManagerFactory(entityManagerFactory)
                .pageSize(maxChunkPage)
                .queryString("SELECT m FROM Member m where m.channelId IS NOT NULL AND m.channelId != '' ORDER BY m.createdAt ASC")
                .build();
    }

    @Bean
    public ItemProcessor<Member, MemberChannelIdDAO> channelIdExtractor() {
        return member -> MemberChannelIdDAO.builder()
                    .id(member.getId())
                    .channelId(member.getChannelId())
                    .build();
    }

    @Bean
    public ItemWriter<MemberChannelIdDAO> redisAccessTokenHashWriter(
            @Qualifier("googleAccessTokenStringRedisTemplate") RedisTemplate<String, String> redisTemplate
    ) {
        return items -> {
            HashOperations<String, String, String> hashOperations = redisTemplate.opsForHash();
            redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                String channelIds = items.getItems().stream()
                        .map(MemberChannelIdDAO::getChannelId)
                        .collect(Collectors.joining(","));
                String batchHashField = "batch_" + System.nanoTime();

                hashOperations.put(HASH_KEY, batchHashField, channelIds);
                return null;
            });
        };
    }

    @Bean
    public ItemProcessor<String, String> channelProfileFetcher() {
        return item -> {
            Map<String, Object> queries = new HashMap<>(baseParam);
            queries.put("id", item);
            ChannelListResponse channelResource = googleAPIService.getObjectFromYoutubeDataAPI(
                    HttpMethod.GET,
                    "/channels",
                    queries,
                    ChannelListResponse.class
            );
            List<ChannelResourceFlatted> flattedList = channelResource.getItems().stream()
                    .map(ChannelResourceFlatted::new)
                    .toList();

            return objectMapper.writeValueAsString(flattedList);
        };
    }

    @Bean
    public ItemWriter<String> dbChannelProfileWriter() {
        return profiles -> {
            for (String serializedProfile : profiles.getItems()) {
                JavaType type = objectMapper.getTypeFactory().constructCollectionType(List.class, ChannelResourceFlatted.class);
                List<ChannelResourceFlatted> profileList = objectMapper.readValue(serializedProfile, type);
                for (ChannelResourceFlatted profile : profileList) {
                    memberRepository.findByChannelId(profile.getChannelId()).ifPresent(member -> {
                        member.setProfileImage(profile.getProfileImage());
                        member.setChannelName(profile.getChannelName());
                        member.setChannelHandler(profile.getChannelHandler());
                    });
                }
            }
        };
    }

    @Bean
    public Tasklet redisHashClearTasklet(
            @Qualifier("googleAccessTokenStringRedisTemplate") RedisTemplate<String, String> redisTemplate
    ) {
        return (StepContribution contribution, ChunkContext chunkContext) -> {
            redisTemplate.delete(HASH_KEY);
            return RepeatStatus.FINISHED;
        };
    }

    @Bean
    public Step filteringUpdateTargetAndGetChannelId(
            @Qualifier("channelIdExistsMemberItemReader") ItemReader<Member> itemReader,
            @Qualifier("channelIdExtractor") ItemProcessor<Member, MemberChannelIdDAO> itemProcessor,
            @Qualifier("redisAccessTokenHashWriter") ItemWriter<MemberChannelIdDAO> itemWriter
    ) {
        return new StepBuilder("filteringUpdateTargetAndGetChannelId", jobRepository)
                .<Member, MemberChannelIdDAO>chunk(maxChunkPage, transactionManager)
                .reader(itemReader)
                .processor(itemProcessor)
                .writer(itemWriter)
                .build();
    }

    @Bean
    public Step updateTargetChannelInfo(
            @Qualifier("channelIdItemReader") ItemReader<String> itemReader,
            @Qualifier("channelProfileFetcher") ItemProcessor<String, String> itemProcessor,
            @Qualifier("dbChannelProfileWriter") ItemWriter<String> itemWriter
    ) {
        return new StepBuilder("updateTargetChannelInfo", jobRepository)
                .<String, String>chunk(1, transactionManager) // redis에서 읽는건 하나씩
                .reader(itemReader)
                .processor(itemProcessor)
                .writer(itemWriter)
                .build();
    }

    @Bean
    public Step redisClearStep(
            @Qualifier("redisHashClearTasklet") Tasklet tasklet
    ) {
        return new StepBuilder("redisClearStep", jobRepository)
                .tasklet(tasklet, transactionManager)
                .build();
    }

    @Bean
    public Job updateMemberProfileJob(
            @Qualifier("filteringUpdateTargetAndGetChannelId") Step firstStep,
            @Qualifier("updateTargetChannelInfo") Step secondStep,
            @Qualifier("redisClearStep") Step clearStep
    ) {
        return new JobBuilder("updateMemberProfileJob", jobRepository)
                .start(firstStep)
                .next(secondStep)
                .next(clearStep)
                .build();
    }


    @Getter
    @ToString
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChannelResourceFlatted {
        private String channelId;
        private String profileImage;
        private String channelName;
        private String channelHandler;

        public ChannelResourceFlatted(ChannelResource resource) {
            channelId = resource.getId();
            profileImage = resource.getSnippet().getThumbnails().getDefaultThumbnail().getUrl();
            channelName = resource.getSnippet().getTitle();
            channelHandler = resource.getSnippet().getCustomUrl();
        }
    }
}