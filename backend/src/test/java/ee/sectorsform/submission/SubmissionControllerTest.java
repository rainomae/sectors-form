package ee.sectorsform.submission;

import ee.sectorsform.sector.Sector;
import ee.sectorsform.sector.UnknownSectorException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SubmissionController.class)
class SubmissionControllerTest {

    private static final Sector FOOD = new Sector(6L, "Food and Beverage", 1L, 4);
    private static final Sector FISH = new Sector(42L, "Fish & fish products", 6L, 7);

    private static final String VALID_BODY = """
            {"name": "Mari Maasikas", "sectorIds": [6, 42], "agreedToTerms": true}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SubmissionService submissionService;

    @Test
    void createSubmission_validRequest_returns201WithBody() throws Exception {
        when(submissionService.create("Mari Maasikas", Set.of(6L, 42L), true)).thenReturn(sampleSubmission(1L));

        mockMvc.perform(post("/api/submissions").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Mari Maasikas"))
                .andExpect(jsonPath("$.sectorIds", contains(6, 42)))
                .andExpect(jsonPath("$.agreedToTerms").value(true));
    }

    @Test
    void createSubmission_validRequest_bindsTheSubmissionToTheSession() throws Exception {
        when(submissionService.create("Mari Maasikas", Set.of(6L, 42L), true)).thenReturn(sampleSubmission(1L));
        var session = new MockHttpSession();

        mockMvc.perform(
                post("/api/submissions").session(session).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated());

        assertThat(session.getAttribute(SubmissionController.SESSION_ATTRIBUTE)).isEqualTo(1L);
    }

    @Test
    void createSubmission_sessionAlreadyOwnsOne_returns409() throws Exception {
        mockMvc.perform(
                post("/api/submissions").session(owning(7L)).contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("This session already has submission 7; update it instead"));

        verifyNoInteractions(submissionService);
    }

    @Test
    void createSubmission_emptyBody_returns400WithEveryFieldError() throws Exception {
        mockMvc.perform(post("/api/submissions").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Validation failed"))
                .andExpect(jsonPath("$.errors", hasSize(3)))
                .andExpect(jsonPath("$.errors[*].field", contains("agreedToTerms", "name", "sectorIds")))
                .andExpect(jsonPath("$.errors[0].message").value("You must agree to the terms"))
                .andExpect(jsonPath("$.errors[1].message").value("Name is required"))
                .andExpect(jsonPath("$.errors[2].message").value("Select at least one sector"));

        verifyNoInteractions(submissionService);
    }

    @Test
    void createSubmission_declinedTermsAndTooLongName_returns400() throws Exception {
        mockMvc.perform(
                post("/api/submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "%s", "sectorIds": [6], "agreedToTerms": false}
                                """.formatted("x".repeat(256))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(2)))
                .andExpect(jsonPath("$.errors[0].field").value("agreedToTerms"))
                .andExpect(jsonPath("$.errors[0].message").value("You must agree to the terms"))
                .andExpect(jsonPath("$.errors[1].field").value("name"))
                .andExpect(jsonPath("$.errors[1].message").value("Name must be at most 255 characters"));
    }

    @Test
    void createSubmission_unknownSectorIds_returns400() throws Exception {
        when(submissionService.create("Mari Maasikas", Set.of(6L, 42L), true))
                .thenThrow(new UnknownSectorException(List.of(42L)));

        mockMvc.perform(post("/api/submissions").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Unknown sector ids: [42]"));
    }

    @Test
    void createSubmission_malformedJson_returns400() throws Exception {
        mockMvc.perform(post("/api/submissions").contentType(MediaType.APPLICATION_JSON).content("{\"name\": "))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @Test
    void getCurrentSubmission_noneInSession_returns404() throws Exception {
        mockMvc.perform(get("/api/submissions/current"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("No submission has been saved in this session"));

        verifyNoInteractions(submissionService);
    }

    @Test
    void getCurrentSubmission_ownedSubmission_returns200() throws Exception {
        when(submissionService.findById(1L)).thenReturn(sampleSubmission(1L));

        mockMvc.perform(get("/api/submissions/current").session(owning(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.sectorIds", contains(6, 42)));
    }

    @Test
    void getSubmission_notOwnedByTheSession_returns403() throws Exception {
        mockMvc.perform(get("/api/submissions/1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Submission 1 does not belong to the current session"));

        mockMvc.perform(get("/api/submissions/1").session(owning(2L)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(submissionService);
    }

    @Test
    void getSubmission_ownedByTheSession_returns200() throws Exception {
        when(submissionService.findById(1L)).thenReturn(sampleSubmission(1L));

        mockMvc.perform(get("/api/submissions/1").session(owning(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Mari Maasikas"));
    }

    @Test
    void updateSubmission_ownedByTheSession_returns200() throws Exception {
        when(submissionService.update(1L, "Mari Tamm", Set.of(6L), true)).thenReturn(sampleSubmission(1L));

        mockMvc.perform(
                put("/api/submissions/1")
                        .session(owning(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Mari Tamm", "sectorIds": [6], "agreedToTerms": true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void updateSubmission_notOwnedByTheSession_returns403() throws Exception {
        mockMvc.perform(
                put("/api/submissions/1").session(owning(2L)).contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail", containsString("does not belong")));

        verify(submissionService, never()).update(anyLong(), any(), any(), anyBoolean());
    }

    @Test
    void updateSubmission_invalidBody_returns400() throws Exception {
        mockMvc.perform(
                put("/api/submissions/1").session(owning(1L)).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(3)));

        verifyNoInteractions(submissionService);
    }

    private static MockHttpSession owning(long submissionId) {
        var session = new MockHttpSession();
        session.setAttribute(SubmissionController.SESSION_ATTRIBUTE, submissionId);
        return session;
    }

    private static Submission sampleSubmission(long id) {
        var submission = new Submission("Mari Maasikas", Set.of(FOOD, FISH), true);
        ReflectionTestUtils.setField(submission, "id", id);
        return submission;
    }
}
