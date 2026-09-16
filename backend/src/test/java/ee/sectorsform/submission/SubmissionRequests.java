package ee.sectorsform.submission;

import ee.sectorsform.sector.Sector;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/** Request builders and fixtures shared by the MockMvc tests of this package. */
final class SubmissionRequests {

    static final String SUBMISSIONS = "/api/submissions";

    static final Sector FOOD = new Sector(6L, "Food and Beverage", 1L, 4);
    static final Sector FISH = new Sector(42L, "Fish & fish products", 6L, 7);

    private SubmissionRequests() {
    }

    static MockHttpServletRequestBuilder postJson(String body) {
        return post(SUBMISSIONS).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    static MockHttpServletRequestBuilder postJson(MockHttpSession session, String body) {
        return postJson(body).session(session);
    }

    static MockHttpServletRequestBuilder putJson(long id, String body) {
        return put(SUBMISSIONS + "/" + id).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    static MockHttpServletRequestBuilder putJson(MockHttpSession session, long id, String body) {
        return putJson(id, body).session(session);
    }

    /** A body with the given name and sector ids (a JSON array literal) and the terms agreed. */
    static String json(String name, String sectorIds) {
        return """
                {"name": "%s", "sectorIds": %s, "agreedToTerms": true}
                """.formatted(name, sectorIds);
    }

    /** A session that owns the given submission. */
    static MockHttpSession owning(long submissionId) {
        var session = new MockHttpSession();
        session.setAttribute(SubmissionController.SESSION_ATTRIBUTE, submissionId);
        return session;
    }
}
