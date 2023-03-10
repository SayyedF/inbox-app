package com.jilani.inbox.controllers;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.jilani.inbox.email.Email;
import com.jilani.inbox.email.EmailRepository;
import com.jilani.inbox.email.EmailService;
import com.jilani.inbox.emaillist.EmailListItemKey;
import com.jilani.inbox.emaillist.EmailListItemRepository;
import com.jilani.inbox.folders.*;
import com.jilani.inbox.emaillist.EmailListItem;
import com.jilani.inbox.security.AuthService;
import org.ocpsoft.prettytime.PrettyTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Controller
public class EmailViewController {
    @Autowired
    private FolderRepository folderRepository;

    @Autowired
    private FolderService folderService;

    @Autowired
    private EmailRepository emailRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private EmailListItemRepository emailListItemRepository;

    @Autowired
    private UnreadEmailStatsRepository unreadEmailStatsRepository;

    @Autowired
    private AuthService authService;

    @GetMapping(value = "/emails/{id}")
    public String emailView(@AuthenticationPrincipal OAuth2User principal, Model model,
                           @PathVariable UUID id) {

        String userId = authService.getLoggedInUserId(principal);

        if(userId == null) {
            return "index";
        }
        else {
            String userName = principal.getAttribute("name");

            Optional<Email> optionalEmail = emailRepository.findById(id);
            if(!optionalEmail.isPresent()) {
                return "inbox-page";
            }

            Email email = optionalEmail.get();

            if(!emailService.canAccessEmail(userId, email)) {
                return "redirect:/";
            }

            unreadEmailStatsRepository.decrementUnreadCounter(userId, "Inbox");

            List<Folder> userFolders = folderRepository.findAllByUserId(userId);
            userFolders.forEach(f -> {
                UnreadEmailStats unreadEmailStats = unreadEmailStatsRepository.findUnreadEmailStatsByUserIdAndLabel(f.getUserId(), f.getLabel());
                if(unreadEmailStats == null) {
                    f.setUnreadCount(0);
                } else {
                    f.setUnreadCount(unreadEmailStats.getUnreadCount());
                }
            });

            List<Folder> defaultFolders = folderService.fetchDefaultFolders(userId);
            defaultFolders.forEach(f -> {
                UnreadEmailStats unreadEmailStats = unreadEmailStatsRepository.findUnreadEmailStatsByUserIdAndLabel(f.getUserId(), f.getLabel());
                if(unreadEmailStats == null) {
                    f.setUnreadCount(0);
                } else {
                    f.setUnreadCount(unreadEmailStats.getUnreadCount());
                }
            });

            model.addAttribute("userFolders", userFolders);
            model.addAttribute("defaultFolders", defaultFolders);
            model.addAttribute("name", userName);

            String toIds = "";
            if(email.getTo() != null){
             toIds = String.join(", ", email.getTo());
            }
            EmailListItemKey key = new EmailListItemKey();
            key.setUserId(userId);
            key.setLabel("Inbox");
            key.setTimeUUID(email.getId());
            Optional<EmailListItem> optionalEmailListItem = emailListItemRepository.findById(key);
            if(optionalEmailListItem.isPresent()) {
                EmailListItem emailListItem = optionalEmailListItem.get();
                emailListItem.setUnread(false);
                emailListItemRepository.save(emailListItem);
            }

            Date emailDate = new Date(Uuids.unixTimestamp(email.getId()));
            email.setEmailDate(emailDate);

            /*PrettyTime p = new PrettyTime();
            email.setAgoTimeString(p.format(emailDate));*/

            model.addAttribute("email",email);
            model.addAttribute("toIds", toIds);

            return "email-page";
        }
    }
}
