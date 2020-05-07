package com.rccl.capi.reservations.bookinglist.service;

import com.rccl.capi.reservations.common.exceptions.RequestMapperException;
import com.rccl.capi.reservations.common.exceptions.RequestValidationException;
import com.rccl.capi.reservations.common.util.CommonArt;
import com.rccl.capi.reservations.common.util.Idx;
import com.rccl.capi.reservations.common.util.ThrowingBiConsumer;
import com.rccl.capi.reservations.common.util.transformers.Transform;
import com.rccl.services.bookinglistrq.BookingListHeader;
import com.rccl.services.bookinglistrq.BookingListRQ;
import org.apache.commons.lang3.StringUtils;
import org.opentravel.ota._2003._05.alpha.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static com.rccl.capi.reservations.common.util.CommonArt.mapFun;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

@Component
class MapRequest<S extends OTAReadRQ, T extends BookingListRQ> {
    public final FromRQ<S> extract = new FromRQ<>();
    public final ToRQ<T> load = new ToRQ<>();
    private final ThrowingFields mapWithExceptions = new ThrowingFields();

    T maps(S source, T target) throws RequestMapperException {
        List<ThrowingBiConsumer<S, T>> mapList = Arrays.asList(
                mapFun(extract.shipCode, load.shipCode),
                mapFun(extract.start, load.sailingDate, Transform.stringDateToBigDecimal),
                mapFun(extract.surname, load.userLastName),
                mapFun(extract.lastShipKey, load.lastShipKey),
                mapFun(extract.lastSailingDateKey, load.lastSailingDateKey, Transform.toBdYyyyMmDd),
                mapFun(extract.moreDataEchoTokenLastNameKey, load.lastNameKey),
                mapFun(extract.moreDataEchoTokenBookingIDKey, load.bookingIDKey, Transform.stringToBigDecimal),
                mapFun(extract.vendorCode, load.cruiseLineCode, Transform.vendorToCruiseLineCode),
                // We are not mapping Passenger ID today and we can ignore this. (Sujini's response)
                mapFun(extract.passengerId, load.passengerId, Transform.stringToBigDecimal),
                mapFun(extract.transactionIdentifier, load.groupOrIndividual, Transform.transactionIdentifier2groupOrIndividual),
                mapFun(extract.bookingInfoInd, load.bookingLevelInfoRequired, Transform.fromYesNo),
                mapFun(extract.folderType, load.folderType, Transform.folderType),
                mapFun(extract.bookingType, load.bookingType, Transform.bookingType),
                mapFun(extract.groupId, load.groupId, Transform.stringToBigDecimal),
                mapFun(extract.evaluateItg, load.evaluateItg, Transform.fromYesNo),
                mapFun(extract.maxResponses, load.maxRows, Transform.bigIntegerToBigDecimal),
                mapFun(extract.pageNavigation, load.scrollDirection),
                mapWithExceptions.consumerId
        );
        try {
            mapList.forEach((m -> m.accept(source, target)));
        } catch (Exception e) {
            throw new RequestMapperException(e);
        }
        return target;
    }

    class ThrowingFields {
        private final ThrowingBiConsumer<S, T> consumerId = (source, target) -> {
            try {
                CommonArt.mapBigDecWithException(source, extract.consumerId, target, load.consumerId);
            } catch (final Exception e) {
                throw new RequestValidationException(e);
            }
        };
    }

    static class FromRQ<S extends OTAReadRQ> {
        public static final String TYPE_XVI = "16";
        public static final String TYPE_IX = "9";
        private static final String DEFAULT_NAVIGATION = "D";
        private static final String NEXT_NAVIGATION = "Next";
        private static final int LAST_NAME_KEY_POSITION = 12;
        private static final int BOOKING_ID_KEY_POSITION = 30;
        private static final int SAILING_DATE_KEY_POSITION = 2;
        // from - $OTA_ReadRQ/@MaxResponses
        Function<S, Optional<BigInteger>> maxResponses = source -> ofNullable(source.getMaxResponses());
        // from - $OTA_ReadRQ/in:SearchQualifiers/@PageNavigation
        Function<S, Optional<String>> pageNavigation = source ->
                of(ofNullable(source.getSearchQualifiers()).map(OTAReadRQ.SearchQualifiers::getPageNavigation)
                        .filter(s -> !s.equals(NEXT_NAVIGATION) && StringUtils.isNotBlank(s)).orElse(DEFAULT_NAVIGATION));
        // $OTA_ReadRQ/@TransactionIdentifier - CodeLookup:getRapiCode('RES_TYP',$TransactionIdentifier)
        Function<S, Optional<String>> transactionIdentifier = source -> ofNullable(source.getTransactionIdentifier());
        private final Function<S, Optional<OTAReadRQ.ReadRequests.CruiseReadRequest>> cruiseReadRequest = s ->
                ofNullable(s.getReadRequests())
                        .map(OTAReadRQ.ReadRequests::getCruiseReadRequest).flatMap(Idx::first);
        // from - $OTA_ReadRQ/in:ReadRequests/in:CruiseReadRequest/@BookingInfoInd"(YesNoType)
        Function<S, Optional<YesNoType>> bookingInfoInd = source ->
                cruiseReadRequest.apply(source).map(OTAReadRQ.ReadRequests.CruiseReadRequest::getBookingInfoInd);
        private final Function<S, Optional<OTAReadRQ.ReadRequests.CruiseReadRequest.SelectedSailing>> selectedSailing = s ->
                cruiseReadRequest.apply(s).map(OTAReadRQ.ReadRequests.CruiseReadRequest::getSelectedSailing);
        // from - $OTA_ReadRQ/in:ReadRequests/in:CruiseReadRequest/in:SelectedSailing/@ShipCode
        Function<S, Optional<String>> shipCode = source ->
                selectedSailing.apply(source).map(OTAReadRQ.ReadRequests.CruiseReadRequest.SelectedSailing::getShipCode);
        // from - $OTA_ReadRQ/in:ReadRequests/in:CruiseReadRequest/in:SelectedSailing/@Start
        Function<S, Optional<String>> start = source ->
                selectedSailing.apply(source).map(OTAReadRQ.ReadRequests.CruiseReadRequest.SelectedSailing::getStart);
        // OTA_ReadRQ/in:ReadRequests/in:CruiseReadRequest/in:SelectedSailing/@VendorCode - CodeLookup:getRapiCode('Brand_RQ3A',$VendorCode)
        Function<S, Optional<String>> vendorCode = source ->
                selectedSailing.apply(source).map(OTAReadRQ.ReadRequests.CruiseReadRequest.SelectedSailing::getVendorCode);
        private final Function<S, Optional<PersonNameType>> guestInfo = s ->
                cruiseReadRequest.apply(s).map(OTAReadRQ.ReadRequests.CruiseReadRequest::getGuestInfo);
        // from - $OTA_ReadRQ/in:ReadRequests/in:CruiseReadRequest/in:GuestInfo/in:Surname
        Function<S, Optional<String>> surname = source ->
                guestInfo.apply(source).map(PersonNameType::getSurname).map(StringUtils::upperCase);
        // from - OTA_ReadRQ/@MoreDataEchoToken -
        private final Function<S, Optional<String>> moreDataEchoToken = source -> ofNullable(source.getMoreDataEchoToken());
        // substring($MoreDataEchoToken, '1', '2')
        public final Function<S, Optional<String>> lastShipKey = source ->
                moreDataEchoToken.apply(source).map(s -> StringUtils.substring(s, 0, SAILING_DATE_KEY_POSITION));
        // SchemaUtil:convertToDSTSafeTime(substring($MoreDataEchoToken1,3,10),'10000'),'NameListRQ','LFLDDT')"
        public final Function<S, Optional<String>> lastSailingDateKey = source ->
                moreDataEchoToken.apply(source).map(s -> StringUtils.substring(s, SAILING_DATE_KEY_POSITION, LAST_NAME_KEY_POSITION));
        // substring($MoreDataEchoToken2,13,18)
        Function<S, Optional<String>> moreDataEchoTokenLastNameKey = source ->
                moreDataEchoToken.apply(source).map(s -> StringUtils.substring(s, LAST_NAME_KEY_POSITION, BOOKING_ID_KEY_POSITION));
        // substring($MoreDataEchoToken3,31,7)),'NameListRQ','LFCNBR')
        Function<S, Optional<String>> moreDataEchoTokenBookingIDKey = source ->
                moreDataEchoToken.apply(source).map(s -> StringUtils.substring(s, BOOKING_ID_KEY_POSITION, BOOKING_ID_KEY_POSITION + 7));
        // OTA_ReadRQ/in:UniqueID
        private final Function<S, Optional<OTAReadRQ.UniqueID>> uniqueIdObj = source -> ofNullable(source.getUniqueID());
        Function<S, Optional<String>> passengerId = source ->
                uniqueIdObj.apply(source).filter(x -> !isTheType(x, TYPE_XVI) && !isTheType(x, TYPE_IX)).map(UniqueIDType::getID);
        Function<S, Optional<String>> consumerId = source ->
                uniqueIdObj.apply(source).filter(x -> isTheType(x, TYPE_XVI)).map(UniqueIDType::getID);
        Function<S, Optional<String>> groupId = source ->
                uniqueIdObj.apply(source).filter(x -> isTheType(x, TYPE_IX)).map(UniqueIDType::getID);
        // OTA_ReadRQ/in:UniqueID/@BookInToGroupsEvaluationRequested - CodeLookup:getRapiCode('YesNoType',$BookInToGroupsEvaluationRequested)
        Function<S, Optional<YesNoType>> evaluateItg = source ->
                uniqueIdObj.apply(source).map(OTAReadRQ.UniqueID::getBookInToGroupsEvaluationRequested);
        private final Function<S, Optional<List<SearchQualifierType.Status>>> searchQualifierStatus = source ->
                ofNullable(source.getSearchQualifiers()).map(SearchQualifierType::getStatus);
        // OTA_ReadRQ/in:SearchQualifier - CodeLookup:getRapiCode('FOLDER_TYPE', $SearchQualifiers/Uni:Status[1]/@Status)
        Function<S, Optional<String>> folderType = source ->
                searchQualifierStatus.apply(source).flatMap(Idx::first).map(SearchQualifierType.Status::getStatus);
        // OTA_ReadRQ/in:SearchQualifier - CodeLookup:getRapiCode('BOOKING_TYPE', $SearchQualifiers1/Uni:Status[2]/@Status)"
        Function<S, Optional<String>> bookingType = source ->
                searchQualifierStatus.apply(source).flatMap(Idx::second).map(SearchQualifierType.Status::getStatus);

        private boolean isTheType(OTAReadRQ.UniqueID x, String type) {
            return x.getType() != null && x.getType().equals(type);
        }
    }

    static class ToRQ<T extends BookingListRQ> {
        private final Function<T, BookingListHeader> bookingListHeader = target ->
                ofNullable(target.getBookingListHeader()).orElseGet(() -> {
                    synchronized (this) {
                        return ofNullable(target.getBookingListHeader()).orElseGet(() -> {
                            BookingListHeader listHeader = new BookingListHeader();
                            target.setBookingListHeader(listHeader);
                            return listHeader;
                        });
                    }
                });

        BiConsumer<T, String> shipCode = (target, v) -> bookingListHeader.apply(target).setShipCode(v);
        BiConsumer<T, BigDecimal> sailingDate = (target, v) -> bookingListHeader.apply(target).setSailingDate(v);
        BiConsumer<T, String> userLastName = (target, v) -> bookingListHeader.apply(target).setUserLastName(v);
        BiConsumer<T, String> bookingLevelInfoRequired = (target, v) ->
                bookingListHeader.apply(target).setBookingLevelInfoRequired(v);
        BiConsumer<T, BigDecimal> maxRows = (target, v) -> bookingListHeader.apply(target).setMaxRows(v);
        BiConsumer<T, String> scrollDirection = (target, v) -> bookingListHeader.apply(target).setScrollDirection(v);
        BiConsumer<T, String> lastNameKey = (target, v) -> bookingListHeader.apply(target).setLastNameKey(v);
        BiConsumer<T, BigDecimal> bookingIDKey = (target, v) -> bookingListHeader.apply(target).setBookingIDKey(v);
        BiConsumer<T, String> cruiseLineCode = (target, v) -> bookingListHeader.apply(target).setCruiseLineCode(v);
        BiConsumer<T, BigDecimal> passengerId = (target, v) -> bookingListHeader.apply(target).setPassengerID(v);
        BiConsumer<T, BigDecimal> consumerId = (target, v) -> bookingListHeader.apply(target).setConsumerID(v);
        BiConsumer<T, String> groupOrIndividual = (target, v) -> bookingListHeader.apply(target).setGroupOrIndividual(v);
        BiConsumer<T, String> folderType = (target, v) -> bookingListHeader.apply(target).setFolderType(v);
        BiConsumer<T, String> bookingType = (target, v) -> bookingListHeader.apply(target).setBookingType(v);
        BiConsumer<T, BigDecimal> groupId = (target, v) -> bookingListHeader.apply(target).setGroupID(v);
        BiConsumer<T, String> evaluateItg = (target, v) -> bookingListHeader.apply(target).setEvaluateITG(v);
        BiConsumer<T, String> lastShipKey = (target, v) -> bookingListHeader.apply(target).setLastShipKey(v);
        BiConsumer<T, BigDecimal> lastSailingDateKey = (target, v) -> bookingListHeader.apply(target).setLastSailingDateKey(v);
    }
}
