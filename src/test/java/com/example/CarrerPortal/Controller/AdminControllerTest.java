package com.example.CarrerPortal.Controller;

import com.example.CarrerPortal.Config.JwtUtils;
import com.example.CarrerPortal.Dto.JobRequest;
import com.example.CarrerPortal.Model.Application;
import com.example.CarrerPortal.Model.Job;
import com.example.CarrerPortal.Service.AdminService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Security filters are disabled here (addFilters = false) so this focuses
 * purely on request mapping, delegation to AdminService, and response
 * shape/status — not on the JWT/role enforcement itself, which lives in
 * SecurityConfig and is out of scope for a controller-slice test.
 */
@WebMvcTest(AdminController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminControllerTest {

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
    private AdminService adminService;

    // JwtAuthenticationFilter is a Filter bean, and Filter beans ARE picked up
    // by @WebMvcTest's component scan (unlike plain @Component/@Service beans).
    // It @Autowireds JwtUtils, so without this mock the ApplicationContext
    // fails to load with NoSuchBeanDefinitionException.
    @MockitoBean
    private JwtUtils jwtUtils;

    @Test
    void getAllJobs_returnsJobsFromService() throws Exception {
        Job job = new Job();
        job.setId(1L);
        job.setTitle("Backend Engineer");
        when(adminService.getAllJobs()).thenReturn(List.of(job));

        mockMvc.perform(get("/api/admin/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Backend Engineer"));
    }

    @Test
    void getAllApplications_returnsApplicationsFromService() throws Exception {
        Application application = new Application();
        application.setId(5L);
        when(adminService.getAllApplications()).thenReturn(List.of(application));

        mockMvc.perform(get("/api/admin/applications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(5));
    }

    @Test
    void updateJob_returnsUpdatedJob() throws Exception {
        JobRequest request = new JobRequest();
        request.setTitle("Updated title");
        request.setDescription("desc");
        request.setCompanyName("Acme");
        request.setLocation("Remote");

        Job updated = new Job();
        updated.setId(1L);
        updated.setTitle("Updated title");
        when(adminService.updateJob(eq(1L), any(JobRequest.class))).thenReturn(updated);

        mockMvc.perform(put("/api/admin/jobs/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated title"));
    }

    @Test
    void deleteJob_returnsConfirmationMessage() throws Exception {
        doNothing().when(adminService).deleteJob(1L);

        mockMvc.perform(delete("/api/admin/jobs/1"))
                .andExpect(status().isOk())
                .andExpect(content().string("Job deleted by admin successfully."));

        verify(adminService).deleteJob(1L);
    }
}