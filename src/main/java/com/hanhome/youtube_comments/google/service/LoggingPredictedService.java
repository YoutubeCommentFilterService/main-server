package com.hanhome.youtube_comments.google.service;

import com.hanhome.youtube_comments.google.object.DeleteCommentObject;
import com.hanhome.youtube_comments.google.object.predict.PredictionCombinedResource;
import com.hanhome.youtube_comments.mongodb.entity.CommentDocument;
import com.hanhome.youtube_comments.mongodb.entity.DeletedClassDocument;
import com.hanhome.youtube_comments.mongodb.entity.NicknameDocument;
import com.hanhome.youtube_comments.mongodb.entity.PredictedClassDocument;
import com.hanhome.youtube_comments.mongodb.repository.CommentRepository;
import com.hanhome.youtube_comments.mongodb.repository.DeletedClassRepository;
import com.hanhome.youtube_comments.mongodb.repository.NicknameRepository;
import com.hanhome.youtube_comments.mongodb.repository.PredictedClassRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LoggingPredictedService {
    private final CommentRepository commentRepository;
    private final NicknameRepository nicknameRepository;
    private final PredictedClassRepository predictedClassRepository;
    private final DeletedClassRepository deletedClassRepository;

    public void savePredictLogging(String videoId, List<PredictionCombinedResource> predictResults) {
        List<CommentDocument> commentDocuments = new ArrayList<>();
        Set<NicknameDocument> nicknameDocuments = new HashSet<>();
        Set<String> commentIds = new HashSet<>();

        predictResults.forEach(predictResult -> {
            commentDocuments.add(new CommentDocument(
                    predictResult.getId(),
                    predictResult.getChannelId(),
                    videoId,
                    predictResult.getCommentPredict(),
                    predictResult.getCommentProb(),
                    predictResult.getComment()
            ));
            nicknameDocuments.add(new NicknameDocument(
                    predictResult.getChannelId(),
                    predictResult.getNickname(),
                    predictResult.getNicknamePredict(),
                    predictResult.getNicknameProb(),
                    predictResult.getProfileImage()
            ));
            commentIds.add(predictResult.getId());
        });

        Optional<PredictedClassDocument> optPcd = predictedClassRepository.findById(videoId);
        PredictedClassDocument pcd;
        if (optPcd.isPresent()) { // update
            pcd = optPcd.get();
            pcd.getPredictedCommentIds().addAll(commentIds);
        } else {
            pcd = new PredictedClassDocument(videoId, commentIds);
        }

        predictedClassRepository.save(pcd);

        commentRepository.saveAll(commentDocuments);
        nicknameRepository.saveAll(nicknameDocuments);
    }

    @SuppressWarnings("unchecked")
    public void saveDeletedLogging(String videoId, List<?> deleteTargets) {
        if (deleteTargets.isEmpty()) return;
        Set<String> deleteTargetIds;
        if (deleteTargets.stream().allMatch(o -> o instanceof DeleteCommentObject)) {
            deleteTargetIds = ((List<DeleteCommentObject>) deleteTargets).stream()
                    .map(DeleteCommentObject::getCommentId)
                    .collect(Collectors.toSet());
        } else if (deleteTargets.stream().allMatch(o -> o instanceof String)) {
            deleteTargetIds = new HashSet<>((List<String>) deleteTargets);
        } else {
            return;
        }

        Optional<DeletedClassDocument> optDcd = deletedClassRepository.findById(videoId);
        DeletedClassDocument dcd;
        if (optDcd.isPresent()) {
            dcd = optDcd.get();
            dcd.getDeletedTargetCommentIds().addAll(deleteTargetIds);
        } else {
            dcd = new DeletedClassDocument(videoId, deleteTargetIds);
        }

        deletedClassRepository.save(dcd);
    }
}
