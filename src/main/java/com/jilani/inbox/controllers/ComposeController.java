package com.jilani.inbox.controllers;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.jilani.inbox.email.Email;
import com.jilani.inbox.email.EmailRepository;
import com.jilani.inbox.email.EmailService;
import com.jilani.inbox.emaillist.EmailListItem;
import com.jilani.inbox.emaillist.EmailListItemKey;
import com.jilani.inbox.emaillist.EmailListItemRepository;
import com.jilani.inbox.folders.*;
import com.jilani.inbox.security.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.websocket.server.PathParam;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
public class ComposeController {

    @Autowired
    private FolderRepository folderRepository;

    @Autowired
    private FolderService folderService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private UnreadEmailStatsRepository unreadEmailStatsRepository;

    @Autowired
    private AuthService authService;

    @GetMapping(value = "/compose")
    public String getComposePage(@AuthenticationPrincipal OAuth2User principal,
                                 Model model,
                                 @RequestParam(required = false) String to,
                                 @RequestParam(required = false) UUID emailId
                                 ) {

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

            Email email = new Email();
            email.setFrom(userId);

            if(emailId!=null && StringUtils.hasText(to)) {
                List<String> idsList = splitIds(to);
                model.addAttribute("toIds",String.join(", ", idsList));

                Email previousEmail = emailService.loadEmail(emailId);
                email.setSubject(emailService.getReplySubject(previousEmail));
                email.setBody(emailService.getReplyBody(previousEmail));
            }

            model.addAttribute("email", email);

            return "compose-page";
        }
    }

    @PostMapping(value = "/compose/send")
    public ModelAndView sendEmail(
            @AuthenticationPrincipal OAuth2User principal,
            @ModelAttribute Email email,
            Model model,
            @RequestParam String toIds,
            RedirectAttributes redirectAttributes
            ) {

        String userId = authService.getLoggedInUserId(principal);

        if(userId == null) {
            System.out.println("User is not logged in");
            return new ModelAndView("redirect:/");
        }

        if(email != null) {

            List<String> to = null;
            if(StringUtils.hasText("toIds")) {
                System.out.println(toIds);
                to = splitIds(toIds);

            } else {
                System.out.println("toIds not found in model");
            }

            UUID messageId = Uuids.timeBased();
            email.setId(messageId);
            email.setFrom(userId);
            email.setTo(to);
            emailService.send(email);
            redirectAttributes.addFlashAttribute("message", "Message has been sent");
        }
        return new ModelAndView("redirect:/");
    }

    private List<String> splitIds(String toIds) {
        if(!StringUtils.hasText(toIds))
            return new ArrayList<String>();
        return Arrays.stream(toIds.split(","))
                .map(StringUtils::trimWhitespace)
                .filter(id -> StringUtils.hasText(id))
                .distinct()
                .collect(Collectors.toList());
    }
}
