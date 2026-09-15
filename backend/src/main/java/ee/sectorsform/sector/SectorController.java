package ee.sectorsform.sector;

import ee.sectorsform.sector.dto.SectorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/api/sectors", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Sectors", description = "Sector reference data for the form")
public class SectorController {

    private final SectorService sectorService;

    public SectorController(SectorService sectorService) {
        this.sectorService = sectorService;
    }

    @GetMapping
    @Operation(summary = "List sectors as a tree, in display order")
    public List<SectorResponse> listSectors() {
        return SectorResponse.treeOf(sectorService.findAll());
    }
}
