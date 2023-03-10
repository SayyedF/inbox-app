package com.jilani.inbox.controllers;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.jilani.inbox.emaillist.EmailListItem;
import com.jilani.inbox.emaillist.EmailListItemRepository;
import com.jilani.inbox.folders.*;
import com.jilani.inbox.security.AuthService;
import org.ocpsoft.prettytime.PrettyTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Controller
public class InboxController {

    @Autowired
    private FolderRepository folderRepository;

    @Autowired
    private FolderService folderService;

    @Autowired
    private EmailListItemRepository emailListItemRepository;

    @Autowired
    private UnreadEmailStatsRepository unreadEmailStatsRepository;

    @Autowired
    private AuthService authService;

    @GetMapping(value = "/")
    public String homePage(
            @RequestParam(required = false) String folder,
            @AuthenticationPrincipal OAuth2User principal,
            Model model) {

        String userId = authService.getLoggedInUserId(principal);

        if(userId == null) {
            return "index";
        }
        else {
            String userName = principal.getAttribute("name");
            List<Folder> userFolders = folderRepository.findAllByUserId(userId);
            userFolders.forEach(f -> {
                UnreadEmailStats unreadEmailStats = unreadEmailStatsRepository.findUnreadEmailStatsByUserIdAndLabel(f.getUserId(), f.getLabel());
                if(unreadEmailStats == null) {
                    f.setUnreadCount(0);
                } else {
                    f.setUnreadCount(unreadEmailStats.getUnreadCount());
                }
            });

            if(!StringUtils.hasText(folder)) {
                folder = "Inbox";
            }
            List<EmailListItem> emailList = emailListItemRepository.findAllByKey_UserIdAndKey_Label(userId, folder);

            PrettyTime p = new PrettyTime();
            emailList.forEach(emailItem -> {
                UUID timeUuid = emailItem.getKey().getTimeUUID();
                Date emailDateTime = new Date(Uuids.unixTimestamp(timeUuid));
                emailItem.setAgoTimeString(p.format(emailDateTime));
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
            model.addAttribute("folderName", folder);


            model.addAttribute("emailList", emailList);
            return "inbox-page";
        }
    }
}
