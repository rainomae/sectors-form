package ee.sectorsform.submission;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;

import static ee.sectorsform.submission.SubmissionRequests.SUBMISSIONS;
import static ee.sectorsform.submission.SubmissionRequests.json;
import static ee.sectorsform.submission.SubmissionRequests.postJson;
import static ee.sectorsform.submission.SubmissionRequests.putJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The full save-and-edit flow against the real database, seeded sectors and validator. */
@SpringBootTest
@AutoConfigureMockMvc
class SubmissionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createSubmission_storesEveryFieldAndBindsItToTheSession() throws Exception {
        var session = new MockHttpSession();

        var body = mockMvc.perform(
                post(SUBMISSIONS)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Mari Maasikas", "sectorIds": [6, 42], "agreedToTerms": true}
                                """))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name").value("Mari Maasikas"))
                .andExpect(jsonPath("$.sectorIds", contains(6, 42)))
                .andExpect(jsonPath("$.agreedToTerms").value(true))
                .andExpect(jsonPath("$.createdAt", notNullValue()))
                .andExpect(jsonPath("$.updatedAt", notNullValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long id = idOf(body);
        assertThat((String) JsonPath.read(body, "$.updatedAt")).isEqualTo(JsonPath.read(body, "$.createdAt"));

        mockMvc.perform(get(SUBMISSIONS + "/current").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("Mari Maasikas"))
                .andExpect(jsonPath("$.sectorIds", contains(6, 42)));

        mockMvc.perform(get(SUBMISSIONS + "/" + id).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
    }

    @Test
    void createSubmission_trimsTheNameAndDeduplicatesAndOrdersTheSectors() throws Exception {
        mockMvc.perform(create(new MockHttpSession(), "  Jaan Tamm  ", "[113, 1, 113]"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Jaan Tamm"))
                .andExpect(jsonPath("$.sectorIds", contains(1, 113)));
    }

    @Test
    void createSubmission_unknownSectorIds_returns400() throws Exception {
        mockMvc.perform(create(new MockHttpSession(), "Mari", "[1, 999999, 4242]"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Unknown sector ids: [4242, 999999]"))
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @Test
    void createSubmission_everyFieldInvalid_returns400WithAllErrors() throws Exception {
        mockMvc.perform(
                post(SUBMISSIONS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "   ", "sectorIds": [], "agreedToTerms": false}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", contains("agreedToTerms", "name", "sectorIds")))
                .andExpect(
                        jsonPath(
                                "$.errors[*].message",
                                contains(
                                        "You must agree to the terms", "Name is required",
                                        "Select at least one sector")));
    }

    @Test
    void createSubmission_twiceInOneSession_returns409() throws Exception {
        var session = new MockHttpSession();
        long id = idOf(
                mockMvc.perform(create(session, "First", "[1]"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString());

        mockMvc.perform(create(session, "Second", "[2]"))
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.detail")
                                .value("This session already has submission " + id + "; update it instead"));
    }

    @Test
    void updateSubmission_byTheOwner_changesEveryFieldAndKeepsCreatedAt() throws Exception {
        var session = new MockHttpSession();
        var created = mockMvc.perform(create(session, "Mari", "[6, 42]"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long id = idOf(created);
        String createdAt = JsonPath.read(created, "$.createdAt");

        mockMvc.perform(update(session, id, "Mari Maasikas-Tamm", "[111, 1, 21]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("Mari Maasikas-Tamm"))
                .andExpect(jsonPath("$.sectorIds", contains(1, 21, 111)))
                .andExpect(jsonPath("$.createdAt").value(createdAt));

        mockMvc.perform(get(SUBMISSIONS + "/current").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Mari Maasikas-Tamm"))
                .andExpect(jsonPath("$.sectorIds", contains(1, 21, 111)));
    }

    @Test
    void updateSubmission_byAnotherSession_returns403AndLeavesTheDataUnchanged() throws Exception {
        var owner = new MockHttpSession();
        long id = idOf(
                mockMvc.perform(create(owner, "Mari", "[1]"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString());

        mockMvc.perform(update(new MockHttpSession(), id, "Hacker", "[2]")).andExpect(status().isForbidden());

        var otherOwner = new MockHttpSession();
        mockMvc.perform(create(otherOwner, "Other", "[2]")).andExpect(status().isCreated());
        mockMvc.perform(update(otherOwner, id, "Hacker", "[2]")).andExpect(status().isForbidden());

        mockMvc.perform(get(SUBMISSIONS + "/" + id).session(owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Mari"))
                .andExpect(jsonPath("$.sectorIds", contains(1)));
    }

    @Test
    void getCurrentSubmission_withoutASession_returns404() throws Exception {
        mockMvc.perform(get(SUBMISSIONS + "/current"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No submission has been saved in this session"));
    }

    @Test
    void getCurrentSubmission_whenTheOwnedRowNoLongerExists_returns404AndForgetsTheId() throws Exception {
        var session = new MockHttpSession();
        session.setAttribute(SubmissionController.SESSION_ATTRIBUTE, 987654321L);

        mockMvc.perform(get(SUBMISSIONS + "/current").session(session))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No submission has been saved in this session"));

        assertThat(session.getAttribute(SubmissionController.SESSION_ATTRIBUTE)).isNull();
        mockMvc.perform(update(session, 987654321L, "Mari", "[1]")).andExpect(status().isForbidden());
    }

    @Test
    void updateSubmission_thatChangesOnlyTheSectors_refreshesUpdatedAt() throws Exception {
        var session = new MockHttpSession();
        var created = mockMvc.perform(create(session, "Mari", "[6]")).andReturn().getResponse().getContentAsString();
        long id = idOf(created);
        Instant updatedAtAfterCreate = Instant.parse(JsonPath.read(created, "$.updatedAt"));

        var updated = mockMvc.perform(update(session, id, "Mari", "[6, 42]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sectorIds", contains(6, 42)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String updatedAtAfterUpdate = JsonPath.read(updated, "$.updatedAt");
        assertThat(Instant.parse(updatedAtAfterUpdate)).isAfter(updatedAtAfterCreate);

        // read back from the database, not only from the response
        mockMvc.perform(get(SUBMISSIONS + "/current").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updatedAt").value(updatedAtAfterUpdate));
    }

    @Test
    void createSubmission_whenTheSessionPointsAtAMissingRow_createsANewOneInsteadOf409() throws Exception {
        var session = new MockHttpSession();
        session.setAttribute(SubmissionController.SESSION_ATTRIBUTE, 987654321L);

        var body = mockMvc.perform(create(session, "Mari", "[1]"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(session.getAttribute(SubmissionController.SESSION_ATTRIBUTE)).isEqualTo(idOf(body));
    }

    private static MockHttpServletRequestBuilder create(MockHttpSession session, String name, String sectorIds) {
        return postJson(session, json(name, sectorIds));
    }

    private static MockHttpServletRequestBuilder update(MockHttpSession session, long id, String name,
            String sectorIds) {
        return putJson(session, id, json(name, sectorIds));
    }

    private static long idOf(String responseBody) {
        return ((Number) JsonPath.read(responseBody, "$.id")).longValue();
    }
}
