package com.hanhome.youtube_comments.member.dao;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Builder
@Getter
public class DeleteTargetMemberDAO {
    private UUID id;
}
