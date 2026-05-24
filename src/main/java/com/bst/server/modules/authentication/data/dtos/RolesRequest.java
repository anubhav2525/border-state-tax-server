package com.bst.server.modules.authentication.data.dtos;

import lombok.*;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RolesRequest {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Create {
        private String name;
        private String displayName;
        private String description;
    }


    public static class Update {
    }


    public static class Search {
    }


    public static class BulkToggle {
    }


}
