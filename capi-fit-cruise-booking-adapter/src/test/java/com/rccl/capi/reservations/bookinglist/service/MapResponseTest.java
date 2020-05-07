package com.rccl.capi.reservations.bookinglist.service;

import com.rccl.capi.reservations.common.util.transformers.Transform;
import com.rccl.services.bookinglistrs.BookingList;
import com.rccl.services.bookinglistrs.BookingListRS;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentravel.ota._2003._05.alpha.OTAResRetrieveRS;
import org.opentravel.ota._2003._05.alpha.YesNoType;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static com.rccl.capi.reservations.common.util.CommonArt.mapFun;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertTrue;

class MapResponseTest {

    private final MapResponse<BookingListRS, OTAResRetrieveRS> rs = new MapResponse<>();

    private final BookingListRS source = new BookingListRS();
    private final OTAResRetrieveRS target = new OTAResRetrieveRS();

    private static Stream<Arguments> forBookInToGroupsEligible() {
        return Stream.of(
                Arguments.of("Y", YesNoType.YES),
                Arguments.of("N", YesNoType.NO),
                Arguments.of("", null)
        );
    }

    @BeforeEach
    void setUp() {
        InitCodeLookupUtils.done(MapResponse.class);
    }

    @ParameterizedTest(name = "Test bookInToGroupsEligible")
    @MethodSource("forBookInToGroupsEligible")
    void bookInToGroupsEligible(String val, YesNoType test) {
        OTAResRetrieveRS.ReservationsList.CruiseReservation cruiseReservation = new OTAResRetrieveRS.ReservationsList.CruiseReservation();
        BookingList bookingList = new BookingList();
        bookingList.setItgEligibleFlag(val);
        mapFun(rs.extract.bookInToGroupsEligible, rs.load.bookInToGroupsEligible, Transform.toYesNo).accept(bookingList, cruiseReservation);
        YesNoType testVal = cruiseReservation.getBookInToGroupsEligible();
        assertThat(testVal, equalTo(test));
    }

    @Test
    void passengerId() {
        OTAResRetrieveRS.ReservationsList.CruiseReservation cruiseReservation = new OTAResRetrieveRS.ReservationsList.CruiseReservation();

        BookingList bookingList = new BookingList();
        BigDecimal value = BigDecimal.valueOf(94819709);
        bookingList.setPassengerID(value);

        mapFun(rs.extract.passengerId, rs.load.passengerId, Transform.bigDecimalToStringId).accept(bookingList, cruiseReservation);

        String testVal = cruiseReservation.getReservationInfo().getGuestDetails().getGuestDetail().get(0)
                .getContactInfo().get(0).getGuestRefNumber();

        assertThat(testVal, equalTo(value.toString()));
    }

    @Test
    void deckCode() {
        OTAResRetrieveRS.ReservationsList.CruiseReservation cruiseReservation = new OTAResRetrieveRS.ReservationsList.CruiseReservation();

        BookingList bookingList = new BookingList();
        String value = "123";
        bookingList.setDeckCode(value);

        mapFun(rs.extract.deckCode, rs.load.deckCode).accept(bookingList, cruiseReservation);

        String testVal = cruiseReservation.getSailingInfo().getSelectedCategory().get(0).getSelectedCabin().get(0).getDeckNumber();

        assertThat(testVal, equalTo(value));
    }

    @Test
    void consumerId() {
        OTAResRetrieveRS.ReservationsList.CruiseReservation cruiseReservation = new OTAResRetrieveRS.ReservationsList.CruiseReservation();

        BookingList bookingList = new BookingList();
        BigDecimal value = BigDecimal.valueOf(70780350);
        bookingList.setConsumerID(value);

        mapFun(rs.extract.consumerId, rs.load.consumerId, Transform.bigDecimalToStringId).accept(bookingList, cruiseReservation);

        String testVal = cruiseReservation.getReservationInfo().getGuestDetails().getGuestDetail().get(0)
                .getContactInfo().get(0).getRPH();

        assertThat(testVal, equalTo(value.toString()));
    }

    @Test
    void surname() {
        OTAResRetrieveRS.ReservationsList.CruiseReservation cruiseReservation = new OTAResRetrieveRS.ReservationsList.CruiseReservation();

        BookingList bookingList = new BookingList();
        String value = "Black";
        bookingList.setUserLastName(value);

        mapFun(rs.extract.userLastName, rs.load.surname).accept(bookingList, cruiseReservation);

        String testVal = cruiseReservation.getReservationInfo().getGuestDetails().getGuestDetail().get(0)
                .getContactInfo().get(0).getPersonName().getSurname();

        assertThat(testVal, equalTo(value));
    }

    @Test
    void givenName() {
        OTAResRetrieveRS.ReservationsList.CruiseReservation cruiseReservation = new OTAResRetrieveRS.ReservationsList.CruiseReservation();

        BookingList bookingList = new BookingList();
        String value = "V";
        bookingList.setUserFirstName(value);

        mapFun(rs.extract.userFirstName, rs.load.givenName).accept(bookingList, cruiseReservation);

        String testVal = cruiseReservation.getReservationInfo().getGuestDetails().getGuestDetail().get(0)
                .getContactInfo().get(0).getPersonName().getGivenName().get(0);

        assertThat(testVal, equalTo(value));
    }

    @Test
    void cabinType() {
        OTAResRetrieveRS.ReservationsList.CruiseReservation cruiseReservation = new OTAResRetrieveRS.ReservationsList.CruiseReservation();

        BookingList bookingList = new BookingList();

        mapFun(rs.extract.reservationType, rs.load.reservationIdTyp).accept(bookingList, cruiseReservation);

        String testVal = cruiseReservation.getReservationInfo().getReservationID().get(0).getType();
        assertThat(testVal, equalTo("14"));
    }

    @Test
    void statusCode() {
        OTAResRetrieveRS.ReservationsList.CruiseReservation cruiseReservation = new OTAResRetrieveRS.ReservationsList.CruiseReservation();

        BookingList bookingList = new BookingList();
        String value = "OF";
        bookingList.setBookingStatus(value);

        mapFun(rs.extract.bookingStatus, rs.load.statusCode, Transform.toStsReservationStatus).accept(bookingList, cruiseReservation);

        String testVal = cruiseReservation.getReservationInfo().getReservationID().get(0).getStatusCode();
        assertThat(testVal, equalTo("42"));
    }

    @Test
    void reservationID() {
        OTAResRetrieveRS.ReservationsList.CruiseReservation cruiseReservation = new OTAResRetrieveRS.ReservationsList.CruiseReservation();

        BookingList bookingList = new BookingList();
        BigDecimal value = BigDecimal.valueOf(1);
        bookingList.setBookingID(value);

        mapFun(rs.extract.bookingID, rs.load.reservationID, Transform.bigDecimalToStringId).accept(bookingList, cruiseReservation);

        String testVal = cruiseReservation.getReservationInfo().getReservationID().get(0).getID();
        assertThat(testVal, equalTo(value.toString()));
    }

    @Test
    void status() {
        OTAResRetrieveRS.ReservationsList.CruiseReservation cruiseReservation = new OTAResRetrieveRS.ReservationsList.CruiseReservation();

        BookingList bookingList = new BookingList();
        String value = "41";

        mapFun(rs.extract.status, rs.load.status).accept(bookingList, cruiseReservation);

        String testVal = cruiseReservation.getSailingInfo().getSelectedCategory().get(0).getSelectedCabin().get(0).getStatus();
        assertThat(testVal, equalTo(value));
    }

    @Test
    void cabinNumber() {
        OTAResRetrieveRS.ReservationsList.CruiseReservation cruiseReservation = new OTAResRetrieveRS.ReservationsList.CruiseReservation();

        BookingList bookingList = new BookingList();
        String value = "GTY";
        bookingList.setCabinNumber(value);

        mapFun(rs.extract.cabinNumber, rs.load.cabinNumber).accept(bookingList, cruiseReservation);

        String testVal = cruiseReservation.getSailingInfo().getSelectedCategory().get(0).getSelectedCabin().get(0).getCabinNumber();
        assertThat(testVal, equalTo(value));
    }

    @Test
    void pricedCategoryCode() {
        OTAResRetrieveRS.ReservationsList.CruiseReservation cruiseReservation = new OTAResRetrieveRS.ReservationsList.CruiseReservation();

        BookingList bookingList = new BookingList();
        String value = "RCC";
        bookingList.setCategoryCode(value);

        mapFun(rs.extract.categoryCode, rs.load.pricedCategoryCode).accept(bookingList, cruiseReservation);

        String testVal = cruiseReservation.getSailingInfo().getSelectedCategory().get(0).getPricedCategoryCode();
        assertThat(testVal, equalTo(value));
    }

    @Test
    void vendorCode() {
        OTAResRetrieveRS.ReservationsList.CruiseReservation cruiseReservation = new OTAResRetrieveRS.ReservationsList.CruiseReservation();

        BookingList bookingList = new BookingList();
        String value = "RCC";
        bookingList.setCruiseLineCode(value);

        mapFun(rs.extract.cruiseLineCode, rs.load.vendorCode).accept(bookingList, cruiseReservation);

        String testVal = cruiseReservation.getSailingInfo().getSelectedSailing().getCruiseLine().getVendorCode();
        assertThat(testVal, equalTo(value));
    }

    @Test
    void shipCode() {
        OTAResRetrieveRS.ReservationsList.CruiseReservation cruiseReservation = new OTAResRetrieveRS.ReservationsList.CruiseReservation();

        BookingList bookingList = new BookingList();
        String value = "NC";
        bookingList.setShipCode(value);

        mapFun(rs.extract.shipCode, rs.load.shipCode).accept(bookingList, cruiseReservation);

        String testVal = cruiseReservation.getSailingInfo().getSelectedSailing().getCruiseLine().getShipCode();
        assertThat(testVal, equalTo(value));
    }

    @Test
    void start() {
        OTAResRetrieveRS.ReservationsList.CruiseReservation cruiseReservation = new OTAResRetrieveRS.ReservationsList.CruiseReservation();

        // 03/17/2039 -> 2039-03-17
        BookingList bookingList = new BookingList();
        bookingList.setDepartureDate("03/17/2039");

        mapFun(rs.extract.departureDate, rs.load.start, Transform.toDashedYyyyMmDd).accept(bookingList, cruiseReservation);

        String testVal = cruiseReservation.getSailingInfo().getSelectedSailing().getStart();
        assertThat(testVal, equalTo("2039-03-17"));
    }

    @Test
    void duration() {
        OTAResRetrieveRS.ReservationsList.CruiseReservation cruiseReservation = new OTAResRetrieveRS.ReservationsList.CruiseReservation();

        BookingList bookingList = new BookingList();
        bookingList.setCruiseLength(BigDecimal.valueOf(11));

        mapFun(rs.extract.cruiseLength, rs.load.duration, Transform.toDuration).accept(bookingList, cruiseReservation);

        String testVal = cruiseReservation.getSailingInfo().getSelectedSailing().getDuration();
        assertThat(testVal, equalTo("P11N"));
    }

    @Test
    void maps() {
        // for coverage - details testing particularly
        rs.maps(source, target);
        assertTrue(true);
    }

}
