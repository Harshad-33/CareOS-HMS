package com.careos.hms.common.exception;

import com.careos.hms.common.validation.TrimmedNotBlank;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = GlobalExceptionHandlerTest.TestExceptionController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = "com\\.careos\\.hms\\.security\\..*"
        )
)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, GlobalExceptionHandlerTest.TestExceptionController.class})
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void handlesResourceNotFound() throws Exception {
        mockMvc.perform(get("/test-errors/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Sample entity not found with identifier: 42"));
    }

    @Test
    void handlesDuplicateResource() throws Exception {
        mockMvc.perform(get("/test-errors/duplicate"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("DUPLICATE_RESOURCE"));
    }

    @Test
    void handlesValidationErrors() throws Exception {
        mockMvc.perform(post("/test-errors/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"));
    }

    @Test
    void handlesInvalidRequest() throws Exception {
        mockMvc.perform(get("/test-errors/invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void handlesDataIntegrityWithoutLeakingSqlDetails() throws Exception {
        mockMvc.perform(get("/test-errors/db-integrity"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("DATA_INTEGRITY_VIOLATION"))
                .andExpect(jsonPath("$.message", not(containsString("ORA-"))))
                .andExpect(jsonPath("$.message", not(containsString("SQL"))));
    }

    @Test
    void handlesDatabaseTimeoutWithoutLeakingDetails() throws Exception {
        mockMvc.perform(get("/test-errors/db-timeout"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("DATABASE_UNAVAILABLE"));
    }

    @Test
    void handlesUnexpectedErrorWithoutLeakingStackTrace() throws Exception {
        mockMvc.perform(get("/test-errors/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
    }

    record SamplePayload(
            @TrimmedNotBlank(message = "Name must not be blank")
            @Size(max = 50)
            String name
    ) {
    }

    @RestController
    @RequestMapping("/test-errors")
    static class TestExceptionController {

        @GetMapping("/not-found")
        String notFound() {
            throw new ResourceNotFoundException("Sample entity", 42);
        }

        @GetMapping("/duplicate")
        String duplicate() {
            throw new DuplicateResourceException("Resource code already exists");
        }

        @GetMapping("/invalid")
        String invalid() {
            throw new InvalidRequestException("Unsupported filter expression");
        }

        @PostMapping("/validate")
        String validate(@Valid @RequestBody SamplePayload payload) {
            return payload.name();
        }

        @GetMapping("/db-integrity")
        String dbIntegrity() {
            throw new DataIntegrityViolationException("could not execute statement; SQL [insert into secret_table]");
        }

        @GetMapping("/db-timeout")
        String dbTimeout() {
            throw new QueryTimeoutException("MySQL lock wait timeout exceeded");
        }

        @GetMapping("/unexpected")
        String unexpected() {
            throw new IllegalStateException("Sensitive internal state failure");
        }
    }
}
