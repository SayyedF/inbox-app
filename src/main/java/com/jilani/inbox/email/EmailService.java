package com.jilani.inbox.email;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.jilani.inbox.emaillist.EmailListItem;
import com.jilani.inbox.emaillist.EmailListItemKey;
import com.jilani.inbox.emaillist.EmailListItemRepository;
import com.jilani.inbox.folders.UnreadEmailStatsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class EmailService {
    @Autowired
    private EmailRepository emailRepository;

    @Autowired
    private EmailListItemRepository emailListItemRepository;

    @Autowired
    private UnreadEmailStatsRepository unreadEmailStatsRepository;

    public Email loadEmail(UUID emailId) {
        Optional<Email> optionalEmail = emailRepository.findById(emailId);
        if(optionalEmail.isPresent())
            return optionalEmail.get();

        return null;
    }

    public String getReplySubject(Email email) {
        return "Re: " + email.getSubject();
    }

    public String getReplyBody(Email email) {
        return "\n\n---------------------------------------\n" +
                "From: " + email.getFrom() + "\n" +
                "To: " + email.getTo() + "\n" +
                "Date: " + new Date(Uuids.unixTimestamp(email.getId())).toString() + "\n" +
                "Message: \n" + email.getBody();
    }

    public boolean canAccessEmail(String userId, Email email) {
        if( !(email.getFrom().equals(userId)) && !(email.getTo().contains(userId)) )
            return false;
        return true;
    }

    public void send(Email email) {
        //Save into sender's Sent Items
        EmailListItemKey senderKey = new EmailListItemKey();
        senderKey.setLabel("Sent Items");
        senderKey.setUserId(email.getFrom());
        senderKey.setTimeUUID(email.getId());

        EmailListItem senderEmailItem = new EmailListItem();
        senderEmailItem.setKey(senderKey);
        senderEmailItem.setUnread(false);
        senderEmailItem.setTo(email.getTo());
        senderEmailItem.setFrom(email.getFrom());
        senderEmailItem.setSubject(email.getSubject());

        emailRepository.save(email);
        emailListItemRepository.save(senderEmailItem);

        //Save into receiver's Inbox
        for (String receiverId: email.getTo()) {
            EmailListItemKey receiverKey = new EmailListItemKey();
            receiverKey.setLabel("Inbox");

            receiverKey.setUserId(receiverId);
            receiverKey.setTimeUUID(email.getId());

            EmailListItem receiverEmailItem = new EmailListItem();
            receiverEmailItem.setKey(receiverKey);
            receiverEmailItem.setUnread(true);
            receiverEmailItem.setTo(email.getTo());
            receiverEmailItem.setFrom(email.getFrom());
            receiverEmailItem.setSubject(email.getSubject());
            emailListItemRepository.save(receiverEmailItem);

            unreadEmailStatsRepository.incrementUnreadCounter(receiverId, "Inbox");
        }
    }
}
