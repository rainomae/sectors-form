package ee.sectorsform.sector.dto;

import ee.sectorsform.sector.Sector;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Schema(description = "A sector with its sub-sectors, in display order")
public record SectorResponse(
        @Schema(example = "6") Long id,
        @Schema(example = "Food and Beverage") String name,

        @Schema(description = "Sub-sectors; empty for a leaf")
        List<SectorResponse> children) {

    /** Builds the tree from all sectors in display order: the top-level sectors, each with its sub-sectors nested. */
    public static List<SectorResponse> treeOf(List<Sector> sectors) {
        Map<Long, List<Sector>> childrenByParentId = new HashMap<>();
        for (Sector sector : sectors) {
            childrenByParentId
                    .computeIfAbsent(sector.getParentId(), parentId -> new ArrayList<>())
                    .add(sector);
        }
        return childrenOf(null, childrenByParentId);
    }

    private static List<SectorResponse> childrenOf(Long parentId, Map<Long, List<Sector>> childrenByParentId) {
        return childrenByParentId.getOrDefault(parentId, List.of()).stream()
                .map(sector -> from(sector, childrenByParentId))
                .toList();
    }

    private static SectorResponse from(Sector sector, Map<Long, List<Sector>> childrenByParentId) {
        return new SectorResponse(sector.getId(), sector.getName(), childrenOf(sector.getId(), childrenByParentId));
    }
}
