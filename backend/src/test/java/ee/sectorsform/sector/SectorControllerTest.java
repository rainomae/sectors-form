package ee.sectorsform.sector;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SectorController.class)
class SectorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SectorService sectorService;

    @Test
    void listSectors_returnsNestedTreeInDisplayOrder() throws Exception {
        when(sectorService.findAll())
                .thenReturn(List.of(
                        new Sector(1L, "Manufacturing", null, 1),
                        new Sector(6L, "Food and Beverage", 1L, 2),
                        new Sector(43L, "Beverages", 6L, 3),
                        new Sector(13L, "Furniture", 1L, 4),
                        new Sector(2L, "Service", null, 5)));

        mockMvc.perform(get("/api/sectors"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[*].id", contains(1, 2)))
                .andExpect(jsonPath("$[0].name").value("Manufacturing"))
                .andExpect(jsonPath("$[0].children[*].name", contains("Food and Beverage", "Furniture")))
                .andExpect(jsonPath("$[0].children[0].children[0].id").value(43))
                .andExpect(jsonPath("$[0].children[0].children[0].name").value("Beverages"))
                .andExpect(jsonPath("$[0].children[0].children[0].children", hasSize(0)))
                .andExpect(jsonPath("$[1].children", hasSize(0)));
    }

    @Test
    void listSectors_noSectors_returnsEmptyArray() throws Exception {
        when(sectorService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/sectors"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }
}
