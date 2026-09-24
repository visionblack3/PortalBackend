package com.example.CarrerPortal.Controller;

import com.example.CarrerPortal.Config.JwtUtils;
import com.example.CarrerPortal.Dto.JobRequest;
import com.example.CarrerPortal.Dto.UpdateStatusRequest;
import com.example.CarrerPortal.Model.Application;
import com.example.CarrerPortal.Model.Job;
import com.example.CarrerPortal.Service.EmployerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EmployerController.class)
@AutoConfigureMockMvc(addFilters = false)
class EmployerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // Built locally rather than @Autowired: in Spring Boot 4.1,
    // spring-boot-starter-webmvc puts Jackson on the classpath but
    // @WebMvcTest doesn't trigger JacksonAutoConfiguration to actually
    // register an ObjectMapper bean in the sliced context (a known gap —
    // see spring-projects/spring-boot#47864). JavaTimeModule is needed
    // because JobRequest.deadline is a LocalDate.
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @MockitoBean
    private EmployerService employerService;

    // JwtAuthenticationFilter is a Filter bean, and Filter beans ARE picked up
    // by @WebMvcTest's component scan (unlike plain @Component/@Service beans).
    // It @Autowireds JwtUtils, so without this mock the ApplicationContext
    // fails to load with NoSuchBeanDefinitionException.
    @MockitoBean
    private JwtUtils jwtUtils;

    private JobRequest sampleJobRequest() {
        JobRequest request = new JobRequest();
        request.setTitle("Backend Engineer");
        request.setDescription("Build things");
        request.setCompanyName("Acme");
        request.setLocation("Remote");
        request.setJobType("FULL_TIME");
        return request;
    }

    @Test
    void postJob_returns201OnSuccess() throws Exception {
        when(employerService.postJob(any(JobRequest.class))).thenReturn(new Job());

        mockMvc.perform(post("/api/employer/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleJobRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Job posted successfully!"));
    }

    @Test
    void postJob_returns400WhenServiceRejects() throws Exception {
        when(employerService.postJob(any(JobRequest.class)))
                .thenThrow(new RuntimeException("Access Denied: Only employers are authorized to post jobs."));

        mockMvc.perform(post("/api/employer/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleJobRequest())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getMyPostedJobs_returnsJobList() throws Exception {
        Job job = new Job();
        job.setId(1L);
        when(employerService.getMyPostedJobs()).thenReturn(List.of(job));

        mockMvc.perform(get("/api/employer/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void updateJob_returnsUpdatedJobOnSuccess() throws Exception {
        Job updated = new Job();
        updated.setId(1L);
        updated.setTitle("Updated title");
        when(employerService.updateJob(eq(1L), any(JobRequest.class))).thenReturn(updated);

        mockMvc.perform(put("/api/employer/jobs/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleJobRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated title"));
    }

    @Test
    void updateJob_returns403WhenNotOwner() throws Exception {
        when(employerService.updateJob(eq(1L), any(JobRequest.class)))
                .thenThrow(new RuntimeException("Access Denied: You cannot edit job posts belonging to another employer."));

        mockMvc.perform(put("/api/employer/jobs/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleJobRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteJob_returnsSuccessMessage() throws Exception {
        doNothing().when(employerService).deleteJob(1L, "hr@acme.com");

        mockMvc.perform(delete("/api/employer/jobs/1")
                        .principal(new UsernamePasswordAuthenticationToken("hr@acme.com", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Job deleted successfully!"));
    }

    @Test
    void deleteJob_returns400WhenNotOwner() throws Exception {
        doThrow(new RuntimeException("Unauthorized: You can only delete your own job postings"))
                .when(employerService).deleteJob(eq(1L), eq("hr@acme.com"));

        mockMvc.perform(delete("/api/employer/jobs/1")
                        .principal(new UsernamePasswordAuthenticationToken("hr@acme.com", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Unauthorized: You can only delete your own job postings"));
    }

    @Test
    void getApplicationsForJob_returnsApplicationsOnSuccess() throws Exception {
        Application application = new Application();
        application.setId(9L);
        when(employerService.getApplicationsForJob(1L)).thenReturn(List.of(application));

        mockMvc.perform(get("/api/employer/jobs/1/applications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(9));
    }

    @Test
    void getApplicationsForJob_returns403WhenNotOwner() throws Exception {
        when(employerService.getApplicationsForJob(1L))
                .thenThrow(new RuntimeException("Access Denied: You can only view applications for your own jobs."));

        mockMvc.perform(get("/api/employer/jobs/1/applications"))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateApplicantStatus_returnsUpdatedApplication() throws Exception {
        Application application = new Application();
        application.setId(9L);
        application.setStatus("SHORTLISTED");
        when(employerService.updateApplicantStatus(9L, "SHORTLISTED")).thenReturn(application);

        UpdateStatusRequest request = new UpdateStatusRequest("SHORTLISTED");

        mockMvc.perform(put("/api/employer/applications/9/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHORTLISTED"));
    }

    @Test
    void updateApplicantStatus_returns400OnInvalidStatus() throws Exception {
        when(employerService.updateApplicantStatus(9L, "MAYBE"))
                .thenThrow(new IllegalArgumentException("Invalid status value: 'MAYBE'."));

        mockMvc.perform(put("/api/employer/applications/9/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateStatusRequest("MAYBE"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void updateApplicantStatus_returns403WhenNotOwner() throws Exception {
        when(employerService.updateApplicantStatus(9L, "HIRED"))
                .thenThrow(new RuntimeException("Access Denied: You can only update applications for your own job posts."));

        mockMvc.perform(put("/api/employer/applications/9/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateStatusRequest("HIRED"))))
                .andExpect(status().isForbidden());
    }
}