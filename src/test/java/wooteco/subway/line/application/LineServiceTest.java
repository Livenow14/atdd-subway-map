package wooteco.subway.line.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import wooteco.subway.line.domain.LineDao;
import wooteco.subway.line.entity.LineEntity;
import wooteco.subway.line.domain.SectionDao;
import wooteco.subway.line.dto.LineRequest;
import wooteco.subway.line.dto.LineResponse;
import wooteco.subway.line.dto.SectionAddRequest;
import wooteco.subway.line.entity.SectionEntity;
import wooteco.subway.station.domain.Station;
import wooteco.subway.station.domain.StationDao;
import wooteco.subway.station.dto.StationResponse;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class LineServiceTest {
    @InjectMocks
    private LineService lineService;

    @Mock
    private LineDao lineDao;

    @Mock
    private SectionDao sectionDao;

    @Mock
    private StationDao stationDao;

    @BeforeEach
    public void init() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("노선 정상 저장된다")
    void save() {
        //given
        when(lineDao.save(any(LineEntity.class))).thenReturn(new LineEntity(1L, "신분당선", "bg-red-600"));
        when(sectionDao.save(any(SectionEntity.class))).thenReturn(new SectionEntity(1L, 1L, 1L, 2L, 10));
        when(stationDao.findById(1L)).thenReturn(Optional.of(new Station(1L, "아마찌역")));
        when(stationDao.findById(2L)).thenReturn(Optional.of(new Station(2L, "검프역")));

        //when
        LineResponse lineResponse = lineService.save(new LineRequest("신분당선", "화이트", 1L, 2L, 10));

        //then
        assertThat(lineResponse.getId()).isEqualTo(1L);
        assertThat(lineResponse.getStations()).hasSize(2);
        assertThat(lineResponse.getStations().get(0).getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("노선에 포함된 구간을 찾는다")
    void findBy() {
        //given
        baseLine();
        when(sectionDao.findByLineId(1L)).thenReturn(Arrays.asList(
                new SectionEntity(2L, 1L, 4L, 3L, 5),
                new SectionEntity(3L, 1L, 1L, 4L, 7)));

        //when
        LineResponse response = lineService.findLine(1L);

        //then
        assertThat(response.getName()).isEqualTo("신분당선");
        assertThat(stationResponsesToString(response.getStations())).containsExactly("아마찌역", "검검역", "마찌역");
    }

    @Test
    @DisplayName("상행 종점 등록 로직")
    void upwardEndPointRegistration() {
        //given
        baseLine();
        long lineId = 1L;
        long upStationId = 2L;
        long downStationId = 1L;
        int distance = 10;

        when(sectionDao.save(any(SectionEntity.class))).thenReturn(new SectionEntity(lineId, upStationId, downStationId, distance));
        when(sectionDao.findByLineId(1L)).thenReturn(Arrays.asList(
                new SectionEntity(2L, lineId, 4L, 3L, 5),
                new SectionEntity(3L, lineId, downStationId, 4L, 7),
                new SectionEntity(4L, lineId, upStationId, downStationId, distance)
                ));

        //when
        lineService.addSection(lineId, new SectionAddRequest(upStationId, downStationId, distance));

        LineResponse response = lineService.findLine(lineId);

        //then
        assertThat(response.getName()).isEqualTo("신분당선");
        assertThat(stationResponsesToString(response.getStations())).containsExactly("검프역", "아마찌역", "검검역", "마찌역");
    }

    @Test
    @DisplayName("하행 종점 등록 로직")
    void downwardEndPointRegistration() {
        //given
        baseLine();
        long lineId = 1L;
        long upStationId = 3L;
        long downStationId = 2L;
        int distance = 10;

        when(sectionDao.save(any(SectionEntity.class))).thenReturn(new SectionEntity(lineId, upStationId, downStationId, distance));
        when(sectionDao.findByLineId(1L)).thenReturn(Arrays.asList(
                new SectionEntity(2L, lineId, 4L, 3L, 5),
                new SectionEntity(3L, lineId, 1L, 4L, 7),
                new SectionEntity(4L, lineId, upStationId, downStationId, distance)
        ));

        //when
        lineService.addSection(lineId, new SectionAddRequest(upStationId, downStationId, distance));

        LineResponse response = lineService.findLine(lineId);

        //then
        assertThat(response.getName()).isEqualTo("신분당선");
        assertThat(stationResponsesToString(response.getStations())).containsExactly("아마찌역", "검검역", "마찌역", "검프역");
    }

    private void baseLine() {
        when(lineDao.findById(1L)).thenReturn(Optional.of(new LineEntity(1L, "신분당선", "bg-red-600")));
        when(stationDao.findById(1L)).thenReturn(Optional.of(new Station(1L, "아마찌역")));
        when(stationDao.findById(2L)).thenReturn(Optional.of(new Station(2L, "검프역")));
        when(stationDao.findById(3L)).thenReturn(Optional.of(new Station(3L, "마찌역")));
        when(stationDao.findById(4L)).thenReturn(Optional.of(new Station(4L, "검검역")));
    }

    private List<String> stationResponsesToString(List<StationResponse> response) {
        return response.stream()
                .map(StationResponse::getName)
                .collect(Collectors.toList());
    }
}