package com.jilani.inbox;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.jilani.inbox.email.Email;
import com.jilani.inbox.email.EmailRepository;
import com.jilani.inbox.email.EmailService;
import com.jilani.inbox.emaillist.EmailListItem;
import com.jilani.inbox.emaillist.EmailListItemKey;
import com.jilani.inbox.emaillist.EmailListItemRepository;
import com.jilani.inbox.folders.FolderRepository;
import com.jilani.inbox.folders.DataStaxAstraProperties;
import com.jilani.inbox.folders.Folder;
import com.jilani.inbox.folders.UnreadEmailStatsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.cassandra.CqlSessionBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;

@SpringBootApplication
@RestController
public class InboxApp {

	@Autowired
	private FolderRepository folderRepository;

	@Autowired
	private EmailListItemRepository emailListItemRepository;

	@Autowired
	private EmailRepository emailRepository;

	@Autowired
	private EmailService emailService;

	@Autowired
	private UnreadEmailStatsRepository unreadEmailStatsRepository;

	public static void main(String[] args) {
		SpringApplication.run(InboxApp.class, args);
	}

	@RequestMapping("/user")
	public String user(@AuthenticationPrincipal OAuth2User principal) {
		System.out.println(principal);
		return principal.getAttribute("name");
	}

	@Bean
    public CqlSessionBuilderCustomizer sessionBuilderCustomizer(DataStaxAstraProperties astraProperties) {
        Path bundle = astraProperties.getSecureConnectBundle().toPath();
        return builder -> builder.withCloudSecureConnectBundle(bundle);
    }

	@PostConstruct
	private void init() {
		folderRepository.save(new Folder("SayyedF","Inbox","black"));
		folderRepository.save(new Folder("SayyedF","Sent Items","green"));
		folderRepository.save(new Folder("SayyedF","Important","purple"));

		for (int i = 0; i < 10; i++) {
			UUID uuid = Uuids.timeBased();
			Email email = new Email();
			email.setId(uuid);
			email.setFrom("SayyedF");
			email.setTo(Arrays.asList("SayyedF"));
			email.setSubject("Subject " + i);
			email.setBody("Body " + i);
			emailService.send(email);
		}
		Email email = new Email();
		email.setId(Uuids.timeBased());
		email.setFrom("abc");
		email.setTo(Arrays.asList("def"));
		email.setSubject("Test Validation");
		email.setBody("Body");
		emailService.send(email);
	}

}
