package ee.sectorsform.sector;

import ee.sectorsform.shared.BadRequestException;

import java.util.List;

public class UnknownSectorException extends BadRequestException {

    public UnknownSectorException(List<Long> ids) {
        super("Unknown sector ids: " + ids);
    }
}
