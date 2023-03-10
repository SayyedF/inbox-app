package com.jilani.inbox.folders;

import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class FolderService {

    public List<Folder> fetchDefaultFolders(String userId) {
        return Arrays.asList(
                new Folder(userId,"Inbox","white"),
                new Folder(userId,"Sent Items","blue"),
                new Folder(userId,"Important","green")
        );
    }
}
