package com.eci.iagen.jplag_service.controller;

import com.eci.iagen.jplag_service.service.JPlagService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PlagiarismController.class)
class PlagiarismControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JPlagService jplagService;

    @Test
    void health_ReturnsSuccess() throws Exception {
        mockMvc.perform(get("/api/plagiarism/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("jplag-service"));
    }
}
