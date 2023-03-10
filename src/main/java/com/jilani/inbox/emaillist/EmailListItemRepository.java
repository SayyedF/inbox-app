package com.jilani.inbox.emaillist;

import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EmailListItemRepository extends CassandraRepository<EmailListItem, EmailListItemKey> {

    public List<EmailListItem> findAllByKey_UserIdAndKey_Label(String userId, String label);

    public EmailListItem findByKey_TimeUUID(UUID timeUUID);
}
