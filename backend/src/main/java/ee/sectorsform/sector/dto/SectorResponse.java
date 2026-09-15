package ee.sectorsform.sector.dto;

import ee.sectorsform.sector.Sector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record SectorResponse(
        Long id,
        String name,
        List<SectorResponse> children) {

    /** Builds the tree from all sectors in display order: top-level sectors with their sub-sectors nested. */
    public static List<SectorResponse> treeOf(List<Sector> sectors) {
        Map<Long, List<Sector>> childrenByParentId = new HashMap<>();
        for (Sector sector : sectors) {
            childrenByParentId.computeIfAbsent(sector.getParentId(), parentId -> new ArrayList<>()).add(sector);
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
