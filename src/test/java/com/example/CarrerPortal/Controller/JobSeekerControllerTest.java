package com.example.CarrerPortal.Controller;

import com.example.CarrerPortal.Config.JwtUtils;
import com.example.CarrerPortal.Dto.ApplicationRequest;
import com.example.CarrerPortal.Model.Application;
import com.example.CarrerPortal.Model.Job;
import com.example.CarrerPortal.Service.JobSeekerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * JobSeekerController pulls the caller's email straight from
 * SecurityContextHolder rather than a Principal argument. @WithMockUser
 * populates that same context (via TestSecurityContextHolder, which the
 * thread-local SecurityContextHolder delegates to), so it works here even
 * with the security filter chain itself disabled.
 */
@WebMvcTest(JobSeekerController.class)
@AutoConfigureMockMvc(addFilters = false)
class JobSeekerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // Built locally rather than @Autowired: in Spring Boot 4.1,
    // spring-boot-starter-webmvc puts Jackson on the classpath but
    // @WebMvcTest doesn't trigger JacksonAutoConfiguration to actually
    // register an ObjectMapper bean in the sliced context (a known gap —
    // see spring-projects/spring-boot#47864).
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @MockitoBean
    private JobSeekerService jobSeekerService;

    // JwtAuthenticationFilter is a Filter bean, and Filter beans ARE picked up
    // by @WebMvcTest's component scan (unlike plain @Component/@Service beans).
    // It @Autowireds JwtUtils, so without this mock the ApplicationContext
    // fails to load with NoSuchBeanDefinitionException.
    @MockitoBean
    private JwtUtils jwtUtils;

    @Test
    void getAllActiveJobs_returnsJobList() throws Exception {
        Job job = new Job();
        job.setId(1L);
        when(jobSeekerService.getAllActiveJobs()).thenReturn(List.of(job));

        mockMvc.perform(get("/api/seeker/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void searchJobs_passesQueryParamsThrough() throws Exception {
        when(jobSeekerService.searchJobs("engineer", "remote")).thenReturn(List.of());

        mockMvc.perform(get("/api/seeker/jobs/search")
                        .param("keyword", "engineer")
                        .param("location", "remote"))
                .andExpect(status().isOk());
    }

    @Test
    void searchJobs_defaultsToEmptyStringsWhenParamsOmitted() throws Exception {
        when(jobSeekerService.searchJobs("", "")).thenReturn(List.of());

        mockMvc.perform(get("/api/seeker/jobs/search"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "asha@seeker.com")
    void applyForJob_returns201OnSuccess() throws Exception {
        ApplicationRequest request = new ApplicationRequest();
        request.setResumeUrl("https://resume.example/asha.pdf");
        request.setCoverLetter("Excited to apply.");
        when(jobSeekerService.applyForJob(eq(1L), any(ApplicationRequest.class), eq("asha@seeker.com")))
                .thenReturn(new Application());

        mockMvc.perform(post("/api/seeker/jobs/1/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Applied"));
    }

    @Test
    @WithMockUser(username = "asha@seeker.com")
    void applyForJob_worksWithNoRequestBody() throws Exception {
        when(jobSeekerService.applyForJob(eq(1L), any(ApplicationRequest.class), eq("asha@seeker.com")))
                .thenReturn(new Application());

        mockMvc.perform(post("/api/seeker/jobs/1/apply"))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "asha@seeker.com")
    void applyForJob_returns400OnDuplicateApplication() throws Exception {
        when(jobSeekerService.applyForJob(eq(1L), any(ApplicationRequest.class), eq("asha@seeker.com")))
                .thenThrow(new RuntimeException("You have already applied for this position."));

        mockMvc.perform(post("/api/seeker/jobs/1/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void getMyApplications_returnsApplicationsFromService() throws Exception {
        Application application = new Application();
        application.setId(3L);
        when(jobSeekerService.getMyApplications()).thenReturn(List.of(application));

        mockMvc.perform(get("/api/seeker/applications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(3));
    }
}