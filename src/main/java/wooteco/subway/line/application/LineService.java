package wooteco.subway.line.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wooteco.subway.line.domain.*;
import wooteco.subway.line.dto.LineRequest;
import wooteco.subway.line.dto.LineResponse;
import wooteco.subway.line.dto.SectionAddRequest;
import wooteco.subway.line.entity.LineEntity;
import wooteco.subway.line.entity.SectionEntity;
import wooteco.subway.station.domain.Station;
import wooteco.subway.station.domain.StationDao;
import wooteco.subway.station.dto.StationResponse;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LineService {

    private final LineDao lineDao;
    private final SectionDao sectionDao;
    private final StationDao stationDao;

    public LineService(LineDao lineDao, SectionDao sectionDao, StationDao stationDao) {
        this.lineDao = lineDao;
        this.sectionDao = sectionDao;
        this.stationDao = stationDao;
    }

    @Transactional
    public LineResponse save(final LineRequest lineRequest) {
        validateDuplication(lineRequest);
        LineEntity savedLineEntity = lineDao.save(new LineEntity(lineRequest.getName(), lineRequest.getColor()));
        Station upStation = findStationById(lineRequest.getUpStationId());
        Station downStation = findStationById(lineRequest.getDownStationId());
        Line line = new Line(savedLineEntity.id(), savedLineEntity.name(), savedLineEntity.color());

        SectionEntity sectionEntity = sectionDao.save(new SectionEntity(line.getId(), upStation.getId(), downStation.getId(), lineRequest.getDistance()));
        Section section = new Section(sectionEntity.getId(), line, upStation, downStation, sectionEntity.getDistance());
        return new LineResponse(line.getId(), line.nameAsString(), line.getColor(), toStationsResponses(Collections.singletonList(section)));
    }

    @Transactional(readOnly = true)
    public LineResponse findLine(final Long lineId) {
        LineEntity findLineEntity = findLineEntityById(lineId);
        Line line = new Line(findLineEntity.id(), findLineEntity.name(), findLineEntity.color());
        Sections sections = new Sections(toSections(lineId, line));

        List<Section> sortedSections = sections.sortedSections();
        return new LineResponse(line.getId(), line.nameAsString(), line.getColor(), toStationsResponses(sortedSections));
    }

    @Transactional
    public void addSection(final Long lineId, final SectionAddRequest sectionAddRequest) {
        LineEntity findLineEntity = findLineEntityById(lineId);
        Line line = new Line(findLineEntity.id(), findLineEntity.name(), findLineEntity.color());
        Sections sections = new Sections(toSections(lineId, line));
        Sections originSections = new Sections(sections.sections());

        Station targetUpStation = findStationById(sectionAddRequest.getUpStationId());
        Station targetDownStation = findStationById(sectionAddRequest.getDownStationId());
        int targetDistance = sectionAddRequest.getDistance();

        sections.upwardEndPointRegistration(line, targetUpStation, targetDownStation, targetDistance);
        sections.downwardEndPointRegistration(line, targetUpStation, targetDownStation, targetDistance);

        dirtyChecking(originSections, sections);

        // TODO : 예외
        //  lineId가 존재하는지
        //  line의 section에 upstationId와 downStationId 둘다 존재하는지 - 노선의 구간에 이미 등록되어있음
        //  upstationId 또는 downStationId로 section을 찾는데, 찾은 section의 distance가 sectionAddRequest의 distance보다 작거나 같은 경우


        // TODO : line의 section에 sectionAddRequest의 upstationId가 존재하는지
        //  존재하면 sectionAddRequest의 upstationId로 section을 찾고
        //  찾은 section의 upstationId를 sectionAddRequest의 downStationId로 수정한다.
        //  찾은 section의 distance를 sectionAddRequest의 distance를 뺀 값으로 수정한다.
        //

        // TODO : line의 section에 sectionAddRequest의 downStationId가 존재하는지
        //  존재하면 sectionAddRequest의 downStationId로 section을 찾고
        //  찾은 section의 downStationId를 sectionAddRequest의 upStationId로 수정한다.
        //  찾은 section의 distance를 sectionAddRequest의 distance를 뺀 값으로 수정한다.

        // TODO : section save
    }

    private void dirtyChecking(final Sections originSections, final Sections sections) {
        List<Section> changedSections = originSections.changedSections(sections);
        for (Section section : changedSections) {
            SectionEntity sectionEntity = new SectionEntity(section.line().getId(), section.upStation().getId(), section.downStation().getId(), section.distance());
            if (sectionDao.findByLineIdWithUpStationId(sectionEntity.getLineId(), sectionEntity.getUpStationId()).isPresent()) {
                sectionDao.deleteByLineIdWithUpStationId(sectionEntity.getLineId(), sectionEntity.getUpStationId());
            }

            if (sectionDao.findByLineIdWithDownStationId(sectionEntity.getLineId(), sectionEntity.getDownStationId()).isPresent()) {
                sectionDao.deleteByLineIdWithDownStationId(sectionEntity.getLineId(), sectionEntity.getDownStationId());
            }
            sectionDao.save(sectionEntity);
        }
    }

    private List<Section> toSections(final Long lineId, final Line line) {
        List<SectionEntity> sectionEntities = sectionDao.findByLineId(lineId);
        return sectionEntities.stream()
                .map(sectionEntity -> new Section(sectionEntity.getId(), line, findStationById(sectionEntity.getUpStationId()), findStationById(sectionEntity.getDownStationId()), sectionEntity.getDistance()))
                .collect(Collectors.toList());
    }

    private List<StationResponse> toStationsResponses(final List<Section> sections) {
        return sections.stream()
                .map(singleSection -> Arrays.asList(singleSection.upStation(), singleSection.downStation()))
                .flatMap(Collection::stream)
                .distinct()
                .map(StationResponse::new)
                .collect(Collectors.toList());
    }

    private void validateDuplication(final LineRequest lineRequest) {
        if (lineDao.findByName(lineRequest.getName()).isPresent()) {
            throw new IllegalStateException("이미 있는 역임!");
        }

        if (lineDao.findByColor(lineRequest.getColor()).isPresent()) {
            throw new IllegalStateException("이미 있는 색깔임!");
        }
    }

    private LineEntity findLineEntityById(Long id) {
        return lineDao.findById(id).orElseThrow(() -> new IllegalArgumentException("없는 노선임!"));
    }

    private Station findStationById(Long stationId) {
        return stationDao.findById(stationId).orElseThrow(() -> new IllegalStateException("없는 역임!"));
    }
}
