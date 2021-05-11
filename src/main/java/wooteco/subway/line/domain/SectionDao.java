package wooteco.subway.line.domain;

import wooteco.subway.line.entity.SectionEntity;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.Optional;

public interface SectionDao {
    SectionEntity save(SectionEntity sectionEntity);

    List<SectionEntity> findAll();

    Optional<SectionEntity> findById(Long id);

    void delete(Long id);

    List<SectionEntity> findByLineId(Long id);

    Optional<SectionEntity> findByLineIdWithUpStationId(Long lineId, Long id);

    void deleteByLineIdWithUpStationId(Long lineId, Long upStationId);

    Optional<SectionEntity> findByLineIdWithDownStationId(Long lineId, Long downStationId);

    void deleteByLineIdWithDownStationId(Long lineId, Long downStationId);
}
