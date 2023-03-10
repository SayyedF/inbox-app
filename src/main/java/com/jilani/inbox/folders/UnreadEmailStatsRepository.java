package com.jilani.inbox.folders;

import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface UnreadEmailStatsRepository extends CassandraRepository<UnreadEmailStats, String> {

    public UnreadEmailStats findUnreadEmailStatsByUserIdAndLabel(String userId, String label);

    @Query("update unread_email_stats " +
            "set unread_count = unread_count + 1 " +
            "where user_id = ?0 " +
            "and label = ?1")
    public void incrementUnreadCounter(String userId, String label);

    @Query("update unread_email_stats " +
            "set unread_count = unread_count - 1 " +
            "where user_id = ?0 " +
            "and label = ?1")
    public void decrementUnreadCounter(String userId, String label);
}
