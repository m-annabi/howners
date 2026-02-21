package com.howners.gestion.repository;

import com.howners.gestion.domain.message.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    @Query("SELECT m FROM Message m WHERE " +
           "(m.sender.id = :userId AND m.recipient.id = :otherUserId) OR " +
           "(m.sender.id = :otherUserId AND m.recipient.id = :userId) " +
           "ORDER BY m.createdAt ASC")
    List<Message> findConversation(@Param("userId") UUID userId, @Param("otherUserId") UUID otherUserId);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.recipient.id = :userId AND m.isRead = false")
    long countUnreadByRecipientId(@Param("userId") UUID userId);

    @Query(value = "SELECT DISTINCT ON (partner_id) * FROM (" +
           "SELECT m.*, CASE WHEN m.sender_id = :userId THEN m.recipient_id ELSE m.sender_id END AS partner_id " +
           "FROM messages m WHERE m.sender_id = :userId OR m.recipient_id = :userId" +
           ") sub ORDER BY partner_id, created_at DESC", nativeQuery = true)
    List<Message> findLatestMessagePerConversation(@Param("userId") UUID userId);
}
