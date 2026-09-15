package ee.sectorsform.sector;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SectorIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listSectors_returnsAllSeededSectorsAsTree() throws Exception {
        mockMvc.perform(get("/api/sectors"))
                .andExpect(status().isOk())
                // three roots in the order of the original select box
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].id", contains(1, 3, 2)))
                .andExpect(jsonPath("$[*].name", contains("Manufacturing", "Other", "Service")))
                // all 79 original options are present exactly once
                .andExpect(jsonPath("$..id", hasSize(79)))
                // Manufacturing > Machinery > Maritime > three leaves
                .andExpect(jsonPath("$[0].children[4].name").value("Machinery"))
                .andExpect(jsonPath("$[0].children[4].children[3].name").value("Maritime"))
                .andExpect(jsonPath("$[0].children[4].children[3].children[*].id", contains(271, 269, 230)))
                .andExpect(jsonPath("$[0].children[4].children[3].children[0].children", hasSize(0)))
                // Service > Transport and Logistics > Air, Rail, Road, Water
                .andExpect(jsonPath("$[2].children[5].name").value("Transport and Logistics"))
                .andExpect(jsonPath("$[2].children[5].children[*].name", contains("Air", "Rail", "Road", "Water")));
    }

    @Test
    void listSectors_returnsCleanedUpNames() throws Exception {
        mockMvc.perform(get("/api/sectors"))
                .andExpect(status().isOk())
                // trailing whitespace from the original markup is gone and entities are decoded
                .andExpect(jsonPath("$[0].children[2].children[2].name").value("Fish & fish products"))
                .andExpect(jsonPath("$[0].children[3].children[2].name").value("Children’s room"));
    }
}
