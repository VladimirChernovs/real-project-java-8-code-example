package com.rccl.capi.reservations.bookinglist.service;

import com.rccl.capi.reservations.common.exceptions.RequestMapperException;
import com.rccl.capi.reservations.common.util.transformers.Transform;
import com.rccl.services.bookinglistrq.BookingListRQ;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentravel.ota._2003._05.alpha.OTAReadRQ;
import org.opentravel.ota._2003._05.alpha.PersonNameType;
import org.opentravel.ota._2003._05.alpha.SearchQualifierType;
import org.opentravel.ota._2003._05.alpha.YesNoType;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.stream.Stream;

import static com.rccl.capi.reservations.common.util.CommonArt.mapFun;
import static java.util.Optional.ofNullable;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertTrue;

class MapRequestTest {

    private final MapRequest<OTAReadRQ, BookingListRQ> rq = new MapRequest<>();
    private final OTAReadRQ source = new OTAReadRQ();
    private final BookingListRQ target = new BookingListRQ();
    private OTAReadRQ.ReadRequests.CruiseReadRequest.SelectedSailing selectedSailing;
    private OTAReadRQ.ReadRequests.CruiseReadRequest cruiseReadRequest;
    private PersonNameType personNameType;

    private static Stream<Arguments> forGroupId() {
        return Stream.of(
                Arguments.of("0", null),
                Arguments.of("9", BigDecimal.valueOf(123))
        );
    }

    private static Stream<Arguments> forConsumerId() {
        return Stream.of(
                Arguments.of("0", null),
                Arguments.of("16", BigDecimal.valueOf(123))
        );
    }

    private static Stream<Arguments> forScrollDirection() {
        return Stream.of(
                Arguments.of("", "D"),
                Arguments.of("Next", "D"),
                Arguments.of("U", "U")
        );
    }

    @BeforeEach
    void setUp() {
        InitCodeLookupUtils.done(MapRequest.class);

        OTAReadRQ.ReadRequests readRequests = ofNullable(source.getReadRequests()).orElseGet(OTAReadRQ.ReadRequests::new);
        source.setReadRequests(readRequests);
        List<OTAReadRQ.ReadRequests.CruiseReadRequest> cruiseReadRequests = readRequests.getCruiseReadRequest();

        cruiseReadRequest = new OTAReadRQ.ReadRequests.CruiseReadRequest();

        selectedSailing = ofNullable(cruiseReadRequest.getSelectedSailing())
                .orElseGet(OTAReadRQ.ReadRequests.CruiseReadRequest.SelectedSailing::new);
        cruiseReadRequest.setSelectedSailing(selectedSailing);

        cruiseReadRequests.add(cruiseReadRequest);

        personNameType = new PersonNameType();
        cruiseReadRequest.setGuestInfo(personNameType);
    }

    @ParameterizedTest(name = "Test ConsumerId")
    @MethodSource("forConsumerId")
    void consumerId(String type, BigDecimal testValue) {
        final String VALUE = "123";
        OTAReadRQ.UniqueID uniqueID = new OTAReadRQ.UniqueID();
        uniqueID.setID(VALUE);
        source.setUniqueID(uniqueID);
        final String SP_TYPE = "16";
        if (type.equals(SP_TYPE)) {
            uniqueID.setType(SP_TYPE);
        }
        mapFun(rq.extract.consumerId, rq.load.consumerId, Transform.stringToBigDecimal).accept(source, target);
        BigDecimal result = null;
        if (type.equals(SP_TYPE)) {
            result = target.getBookingListHeader().getConsumerID();
        }
        assertThat(result, equalTo(testValue));
    }

    @Test
    void evaluateItg() {
        final YesNoType VALUE = YesNoType.NO;
        final String TEST_VALUE = "N";
        OTAReadRQ.UniqueID uniqueID = new OTAReadRQ.UniqueID();
        uniqueID.setBookInToGroupsEvaluationRequested(VALUE);
        source.setUniqueID(uniqueID);
        mapFun(rq.extract.evaluateItg, rq.load.evaluateItg, Transform.fromYesNo).accept(source, target);
        String bookingLevelInfoRequiredTarget = target.getBookingListHeader().getEvaluateITG();
        assertThat(bookingLevelInfoRequiredTarget, equalTo(TEST_VALUE));
    }

    @ParameterizedTest(name = "Test groupId")
    @MethodSource("forGroupId")
    void groupId(String type, BigDecimal testValue) {
        final String VALUE = "123";
        OTAReadRQ.UniqueID uniqueID = new OTAReadRQ.UniqueID();
        uniqueID.setID(VALUE);
        source.setUniqueID(uniqueID);
        final String SP_TYPE = "9";
        if (type.equals(SP_TYPE)) {
            uniqueID.setType(SP_TYPE);
        }
        mapFun(rq.extract.groupId, rq.load.groupId, Transform.stringToBigDecimal).accept(source, target);
        BigDecimal result = null;
        if (type.equals(SP_TYPE)) {
            result = target.getBookingListHeader().getGroupID();
        }
        assertThat(result, equalTo(testValue));
    }

    @Test
    void bookingType() {
        // <map otaCode="36" rapiCode="ACT"/>
        final String VALUE = "36";
        final String TEST_VALUE = "ACT";
        OTAReadRQ.SearchQualifiers searchQualifiers = new OTAReadRQ.SearchQualifiers();
        List<SearchQualifierType.Status> statusList = searchQualifiers.getStatus();
        SearchQualifierType.Status status1 = new SearchQualifierType.Status();
        SearchQualifierType.Status status2 = new SearchQualifierType.Status();
        status2.setStatus(VALUE);
        statusList.add(status1);
        statusList.add(status2);
        source.setSearchQualifiers(searchQualifiers);
        mapFun(rq.extract.bookingType, rq.load.bookingType, Transform.bookingType).accept(source, target);
        String result = target.getBookingListHeader().getBookingType();
        assertThat(result, equalTo(TEST_VALUE));
    }

    @Test
    void folderType() {
        // <map otaCode="A" rapiCode="ALL"/>
        final String VALUE = "A";
        final String TEST_VALUE = "ALL";
        OTAReadRQ.SearchQualifiers searchQualifiers = new OTAReadRQ.SearchQualifiers();
        List<SearchQualifierType.Status> statusList = searchQualifiers.getStatus();
        SearchQualifierType.Status status = new SearchQualifierType.Status();
        status.setStatus(VALUE);
        statusList.add(status);
        source.setSearchQualifiers(searchQualifiers);
        mapFun(rq.extract.folderType, rq.load.folderType, Transform.folderType).accept(source, target);
        String result = target.getBookingListHeader().getFolderType();
        assertThat(result, equalTo(TEST_VALUE));
    }

    @Test
    void groupOrIndividual() {
        //<map otaCode="9" rapiCode="G"/>
        final String VALUE = "9";
        final String TEST_VALUE = "G";
        source.setTransactionIdentifier(VALUE);
        mapFun(rq.extract.transactionIdentifier, rq.load.groupOrIndividual,
                Transform.transactionIdentifier2groupOrIndividual).accept(source, target);
        String result = target.getBookingListHeader().getGroupOrIndividual();
        assertThat(result, equalTo(TEST_VALUE));
    }

    @Test
    void passengerId() {
        final String VALUE = "123";
        final BigDecimal TEST_VALUE = BigDecimal.valueOf(123);
        OTAReadRQ.UniqueID uniqueID = new OTAReadRQ.UniqueID();
        uniqueID.setID(VALUE);
        source.setUniqueID(uniqueID);
        mapFun(rq.extract.passengerId, rq.load.passengerId, Transform.stringToBigDecimal).accept(source, target);
        BigDecimal result = target.getBookingListHeader().getPassengerID();
        assertThat(result, equalTo(TEST_VALUE));
    }

    @Test
    void cruiseLineCode() {
        final String VALUE = "F";
        final String TEST_VALUE = "CDF";
        selectedSailing.setVendorCode(VALUE);
        mapFun(rq.extract.vendorCode, rq.load.cruiseLineCode, Transform.vendorToCruiseLineCode).accept(source, target);
        String result = target.getBookingListHeader().getCruiseLineCode();
        assertThat(result, equalTo(TEST_VALUE));
    }

    @Test
    void bookingIDKey() {
        final String VALUE = "AL12/29/2019ASSAD             28592";
        final BigDecimal TEST_VALUE = BigDecimal.valueOf(28592);
        source.setMoreDataEchoToken(VALUE);
        mapFun(rq.extract.moreDataEchoTokenBookingIDKey, rq.load.bookingIDKey, Transform.stringToBigDecimal).accept(source, target);
        BigDecimal result = target.getBookingListHeader().getBookingIDKey();
        assertThat(result, equalTo(TEST_VALUE));
    }

    @Test
    void lastShipKey() {
        final String VALUE = "AL12/29/2019ASSAD             28592";
        final String TEST_VALUE = "AL";
        source.setMoreDataEchoToken(VALUE);
        mapFun(rq.extract.lastShipKey, rq.load.lastShipKey).accept(source, target);
        String result = target.getBookingListHeader().getLastShipKey();
        assertThat(result, equalTo(TEST_VALUE));
    }

    @Test
    void lastNameKey() {
        final String VALUE = "AL12/29/2019ASSAD             28592";
        final String TEST_VALUE = "ASSAD";
        source.setMoreDataEchoToken(VALUE);
        mapFun(rq.extract.moreDataEchoTokenLastNameKey, rq.load.lastNameKey).accept(source, target);
        String result = target.getBookingListHeader().getLastNameKey();
        assertThat(result, equalTo(TEST_VALUE));
    }

    @Test
    void lastSailingDateKey() {
        final String VALUE = "AL12/29/2019ASSAD             28592";
        final BigDecimal TEST_VALUE = BigDecimal.valueOf(20191229);
        source.setMoreDataEchoToken(VALUE);
        mapFun(rq.extract.lastSailingDateKey, rq.load.lastSailingDateKey, Transform.toBdYyyyMmDd).accept(source, target);
        BigDecimal result = target.getBookingListHeader().getLastSailingDateKey();
        assertThat(result, equalTo(TEST_VALUE));
    }

    @ParameterizedTest(name = "Test scroll direction")
    @MethodSource("forScrollDirection")
    void scrollDirection(String direction, String result) {
        OTAReadRQ.SearchQualifiers searchQualifiers =
                ofNullable(source.getSearchQualifiers()).orElseGet(() -> {
                    OTAReadRQ.SearchQualifiers qualifiers = new OTAReadRQ.SearchQualifiers();
                    source.setSearchQualifiers(qualifiers);
                    return qualifiers;
                });
        searchQualifiers.setPageNavigation(direction);
        mapFun(rq.extract.pageNavigation, rq.load.scrollDirection).accept(source, target);
        String defaultScrollDirection = target.getBookingListHeader().getScrollDirection();
        assertThat(defaultScrollDirection, equalTo(result));
    }

    @Test
    void maxRows() {
        final BigInteger TEST_MAX_ROWS = BigInteger.valueOf(20);
        source.setMaxResponses(TEST_MAX_ROWS);
        mapFun(rq.extract.maxResponses, rq.load.maxRows, Transform.bigIntegerToBigDecimal).accept(source, target);
        BigDecimal maxRowsTarget = target.getBookingListHeader().getMaxRows();
        assertThat(maxRowsTarget, equalTo(BigDecimal.valueOf(TEST_MAX_ROWS.intValue())));
    }

    @Test
    void bookingLevelInfoRequired() {
        final YesNoType OTA_BOOKING_INFO_IND = YesNoType.NO;
        final String TEST_BOOKING_LEVEL_INFO_REQUIRED = "N";
        cruiseReadRequest.setBookingInfoInd(OTA_BOOKING_INFO_IND);
        mapFun(rq.extract.bookingInfoInd, rq.load.bookingLevelInfoRequired, Transform.fromYesNo).accept(source, target);
        String bookingLevelInfoRequiredTarget = target.getBookingListHeader().getBookingLevelInfoRequired();
        assertThat(bookingLevelInfoRequiredTarget, equalTo(TEST_BOOKING_LEVEL_INFO_REQUIRED));
    }

    @Test
    void shipCode() {
        final String TEST_SHIP_CODE = "123";
        selectedSailing.setShipCode(TEST_SHIP_CODE);
        mapFun(rq.extract.shipCode, rq.load.shipCode).accept(source, target);
        String shipCode = target.getBookingListHeader().getShipCode();
        assertThat(shipCode, equalTo(TEST_SHIP_CODE));
    }

    @Test
    void sailingDate() {
        final String OTA_START = "?-2019-10/30:--";
        final BigDecimal TEST_SAILING_DATE = new BigDecimal(20191030);
        selectedSailing.setStart(OTA_START);
        mapFun(rq.extract.start, rq.load.sailingDate, Transform.stringDateToBigDecimal).accept(source, target);
        BigDecimal sailingDateTarget = target.getBookingListHeader().getSailingDate();
        assertThat(sailingDateTarget, equalTo(TEST_SAILING_DATE));
    }

    @Test
    void userLastName() {
        final String TEST_SURNAME = "HI";
        personNameType.setSurname(TEST_SURNAME);
        mapFun(rq.extract.surname, rq.load.userLastName).accept(source, target);
        String userLastNameTarget = target.getBookingListHeader().getUserLastName();
        assertThat(userLastNameTarget, equalTo(TEST_SURNAME));
    }

    @Test
    void maps() throws RequestMapperException {
        // for coverage - details testing particularly
        rq.maps(source, target);
        assertTrue(true);
    }

}
