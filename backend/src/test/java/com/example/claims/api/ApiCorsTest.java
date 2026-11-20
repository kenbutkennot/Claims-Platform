package com.example.claims.api;

import com.example.claims.application.ClaimService;
import com.example.claims.infrastructure.WebConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClaimController.class)
@Import(WebConfig.class)
class ApiCorsTest {
    @Autowired private MockMvc mockMvc;
    @SuppressWarnings("removal")
    @MockBean private ClaimService claimService;

    @Test
    void allowsLocalAngularDevelopmentOriginsToPostClaims() throws Exception {
        for (String origin : new String[] {
                "http://localhost:4200",
                "http://127.0.0.1:4200",
                "http://localhost:4201",
                "http://127.0.0.1:4201"
        }) {
            mockMvc.perform(options("/api/claims")
                            .header("Origin", origin)
                            .header("Access-Control-Request-Method", "POST")
                            .header("Access-Control-Request-Headers", "content-type,x-demo-user"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Access-Control-Allow-Origin", origin));
        }
    }
}
