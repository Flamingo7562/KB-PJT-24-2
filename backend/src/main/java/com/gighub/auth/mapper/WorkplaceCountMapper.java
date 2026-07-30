package com.gighub.auth.mapper;

public interface WorkplaceCountMapper {

    int countActiveByOwnerUserId(Long ownerUserId);
}
