package com.rccl.capi.reservations.bookinglist.service;

import com.rccl.capi.reservations.common.util.Idx;
import com.rccl.capi.reservations.common.util.transformers.Transform;
import com.rccl.services.bookinglistrs.BookingList;
import com.rccl.services.bookinglistrs.BookingListPage;
import com.rccl.services.bookinglistrs.BookingListRS;
import org.apache.commons.lang3.StringUtils;
import org.opentravel.ota._2003._05.alpha.*;
import org.springframework.stereotype.Component;

import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.rccl.capi.reservations.common.util.CommonArt.mapFun;
import static com.rccl.capi.reservations.common.util.CommonArt.notEmptyList;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

@Component
class MapResponse<S extends BookingListRS, T extends OTAResRetrieveRS> {

    public final FromBookingList extract = new FromBookingList();
    public final ToCruiseReservation<OTAResRetrieveRS.ReservationsList.CruiseReservation> load = new ToCruiseReservation<>();

    T maps(S source, T target) {

        AtomicBoolean moreIndicator = new AtomicBoolean(false);

        ofNullable(source.getBookingListPage()).map(BookingListPage::getNumberOfRowsReturned)
                .flatMap(Transform.bigDecimalToBigInteger).filter(s -> s.intValue() > 0).ifPresent(target::setMaxResponses);

        ofNullable(source.getBookingListPage()).map(BookingListPage::getMoreRowsAvailable)
                .flatMap(Transform.stringToBoolean).ifPresent(moreIndicator::set);
        Optional<Stream<BookingList>> bookingListStream =
                ofNullable(source.getBookingList()).filter(notEmptyList).map(Collection::stream);
        bookingListStream.ifPresent(z -> {
            List<OTAResRetrieveRS.ReservationsList.CruiseReservation> collect = z.map(booking -> {

                OTAResRetrieveRS.ReservationsList.CruiseReservation cruiseReservation =
                        new OTAResRetrieveRS.ReservationsList.CruiseReservation();
                Arrays.asList(
                        mapFun(extract.cruiseLength, load.duration, Transform.toDuration),
                        mapFun(extract.departureDate, load.start, Transform.toDashedYyyyMmDd),
                        mapFun(extract.shipCode, load.shipCode),
                        mapFun(extract.cruiseLineCode, load.vendorCode),
                        mapFun(extract.categoryCode, load.pricedCategoryCode),
                        mapFun(extract.cabinNumber, load.cabinNumber),
                        mapFun(extract.status, load.status),
                        mapFun(extract.bookingID, load.reservationID, Transform.bigDecimalToStringId),
                        mapFun(extract.bookingStatus, load.statusCode, Transform.toStsReservationStatus),
                        mapFun(extract.reservationType, load.reservationIdTyp),
                        mapFun(extract.userFirstName, load.givenName),
                        mapFun(extract.userLastName, load.surname),
                        mapFun(extract.bookInToGroupsEligible, load.bookInToGroupsEligible, Transform.toYesNo),
                        mapFun(extract.deckCode, load.deckCode),
                        mapFun(extract.passengerId, load.passengerId, Transform.bigDecimalToStringId),
                        mapFun(extract.consumerId, load.consumerId, Transform.bigDecimalToStringId),

                        mapFun(extract.offlineBookingType, load.bookingType),
                        mapFun(extract.assignedOfflineBookingOwner, load.companyName, Transform.toCompanyNameType),
                        mapFun(extract.offerDate, load.offerDate, Transform.toDashedYyyyMmDd),
                        mapFun(extract.effectiveDate, load.lastModifyDateTime, Transform.toXMLGregorianCalendar),
                        mapFun(extract.packageCode, load.cruisePackageCode),
                        mapFun(extract.currencyCode, load.currencyCode),
                        mapFun(extract.cabinType, load.bedType),
                        mapFun(extract.totalCruiseAmount, load.amount, Transform.stringToBigDecimal),

                        // finalPaymentDate -> optionDate -> cancellationDate, must be in this order
                        mapFun(extract.finalPaymentDate, load.finalPaymentDate, Transform.toDashedYyyyMmDd),
                        mapFun(extract.optionDate, load.optionDate, Transform.toDashedYyyyMmDd),
                        mapFun(extract.cancellationDate, load.cancellationDate, Transform.toDashedYyyyMmDd)
                ).forEach(m -> m.accept(booking, cruiseReservation));
                return cruiseReservation;
            }).collect(Collectors.toList());

            OTAResRetrieveRS.ReservationsList reservationsList =
                    ofNullable(target.getReservationsList()).orElseGet(OTAResRetrieveRS.ReservationsList::new);
            target.setReservationsList(reservationsList);
            List<OTAResRetrieveRS.ReservationsList.CruiseReservation> cruiseReservation = reservationsList.getCruiseReservation();
            cruiseReservation.addAll(collect);

            // MoreDataEchoToken
            of(moreIndicator.get()).filter(s -> s)
                    .ifPresent(t -> target.setMoreDataEchoToken(getMoreDataEchoToken(cruiseReservation)));
        });
        if (!ofNullable(target.getReservationsList()).isPresent()) {
            target.setReservationsList(new OTAResRetrieveRS.ReservationsList());
        }
        ofNullable(target.getReservationsList()).map(OTAResRetrieveRS.ReservationsList::getCruiseReservation)
                .filter(notEmptyList).ifPresent(s -> target.setMoreIndicator(moreIndicator.get()));
        return target;
    }

    private String getMoreDataEchoToken(List<OTAResRetrieveRS.ReservationsList.CruiseReservation> cruiseReservation) {
        AtomicInteger sourceLength = new AtomicInteger();
        String lNamePad;
        final int TARGET_LENGTH = 30;
        final Map<String, String> tokens = new HashMap<>();
        final String SHIP_CODE = "ShipCode";
        final String START = "Start";
        final String SURNAME = "Surname";
        final String ID = "ID";

        of(cruiseReservation).flatMap(Idx::last).ifPresent(l -> {
            ofNullable(l.getSailingInfo()).map(SailingCategoryInfoType::getSelectedSailing).ifPresent(s -> {
                ofNullable(s.getCruiseLine()).map(SailingBaseType.CruiseLine::getShipCode)
                        .ifPresent(x -> putAndMeasure(sourceLength, tokens, SHIP_CODE, x));
                ofNullable(s.getStart()).flatMap(Transform.toSlashedMmDdYyyy)
                        .ifPresent(x -> putAndMeasure(sourceLength, tokens, START, x));
            });
            ofNullable(l.getReservationInfo()).ifPresent(s -> {
                ofNullable(s.getReservationID()).flatMap(Idx::first).map(UniqueIDType::getID)
                        .ifPresent(x -> tokens.put(ID, x));

                ofNullable(s.getGuestDetails()).map(CruiseGuestInfoType.GuestDetails::getGuestDetail).flatMap(Idx::first)
                        .map(CruiseGuestDetailType::getContactInfo).flatMap(Idx::first)
                        .map(CruiseGuestDetailType.ContactInfo::getPersonName).map(PersonNameType::getSurname)
                        .ifPresent(x -> putAndMeasure(sourceLength, tokens, SURNAME, x));
            });
        });
        lNamePad = StringUtils.repeat(StringUtils.SPACE, TARGET_LENGTH - sourceLength.get());
        return tokens.get(SHIP_CODE) + tokens.get(START) + tokens.get(SURNAME) + lNamePad + tokens.get(ID);
    }

    private void putAndMeasure(AtomicInteger sourceLength, Map<String, String> tokens, String keyName, String val) {
        tokens.put(keyName, val);
        sourceLength.getAndAccumulate(val.length(), Integer::sum);
    }

}

class FromBookingList {
    private static final String FIX_STATUS = "41";

    Function<BookingList, Optional<BigDecimal>> cruiseLength = booking -> ofNullable(booking.getCruiseLength());
    Function<BookingList, Optional<String>> departureDate = booking -> ofNullable(booking.getDepartureDate());
    Function<BookingList, Optional<String>> shipCode = booking -> ofNullable(booking.getShipCode());
    Function<BookingList, Optional<String>> cruiseLineCode = booking -> ofNullable(booking.getCruiseLineCode());
    Function<BookingList, Optional<String>> categoryCode = booking -> ofNullable(booking.getCategoryCode());
    Function<BookingList, Optional<String>> cabinNumber = booking -> ofNullable(booking.getCabinNumber());
    Function<BookingList, Optional<String>> status = booking -> of(FIX_STATUS);
    Function<BookingList, Optional<BigDecimal>> bookingID = booking -> ofNullable(booking.getBookingID());
    Function<BookingList, Optional<String>> bookingStatus = booking -> ofNullable(booking.getBookingStatus());
    Function<BookingList, Optional<String>> reservationType = booking -> Transform.toCabinType.get();
    Function<BookingList, Optional<String>> userFirstName = booking -> ofNullable(booking.getUserFirstName());
    Function<BookingList, Optional<String>> userLastName = booking -> ofNullable(booking.getUserLastName());
    Function<BookingList, Optional<BigDecimal>> passengerId = booking -> ofNullable(booking.getPassengerID())
            .filter(s -> s.intValue() != 0);
    Function<BookingList, Optional<BigDecimal>> consumerId = booking -> ofNullable(booking.getConsumerID())
            .filter(s -> s.intValue() != 0);
    Function<BookingList, Optional<String>> bookInToGroupsEligible = booking -> ofNullable(booking.getItgEligibleFlag());
    Function<BookingList, Optional<String>> deckCode = booking -> ofNullable(booking.getDeckCode());
    Function<BookingList, Optional<String>> totalCruiseAmount = booking -> ofNullable(booking.getTotalCruiseAmount());
    Function<BookingList, Optional<String>> currencyCode = booking -> ofNullable(booking.getCurrencyCode());
    Function<BookingList, Optional<String>> packageCode = booking -> ofNullable(booking.getPackageCode());
    Function<BookingList, Optional<String>> offerDate = booking -> ofNullable(booking.getOfferDate());
    Function<BookingList, Optional<String>> effectiveDate = booking -> ofNullable(booking.getEffectiveDate());
    Function<BookingList, Optional<String>> finalPaymentDate = booking -> ofNullable(booking.getFinalPaymentDate());
    Function<BookingList, Optional<String>> optionDate = booking -> ofNullable(booking.getOptionDate());
    Function<BookingList, Optional<String>> cancellationDate = booking -> ofNullable(booking.getCancellationDate());
    Function<BookingList, Optional<String>> assignedOfflineBookingOwner = booking -> ofNullable(booking.getAssignedOfflineBookingOwner());
    Function<BookingList, Optional<String>> offlineBookingType = booking -> ofNullable(booking.getOfflineBookingType());
    Function<BookingList, Optional<String>> cabinType = booking -> ofNullable(booking.getCabinType());
}

class ToCruiseReservation<C extends OTAResRetrieveRS.ReservationsList.CruiseReservation> {
    private static final String PRICE_TYPE_CODE_VIII = "8";
    private static final String PRICE_TYPE_CODE_III = "3";
    private static final int PAYMENT_NUMBER_COD = 2;
    private static final int OPTION_DATE_COD = 1;
    private static final int CANCELLATION_DATE_COD = 3;

    BiConsumer<C, YesNoType> bookInToGroupsEligible = CruiseReservationType::setBookInToGroupsEligible;
    BiConsumer<C, String> bookingType = CruiseReservationType::setBookingType;

    private final Function<C, CruiseReservationType.PaymentsDue> paymentsDue = reservation -> {
        Supplier<CruiseReservationType.PaymentsDue> obj = reservation::getPaymentsDue;
        return ofNullable(obj.get())
                .orElseGet(() -> {
                    synchronized (this) {
                        return ofNullable(obj.get()).orElseGet(() -> {
                            CruiseReservationType.PaymentsDue newObj = new CruiseReservationType.PaymentsDue();
                            reservation.setPaymentsDue(newObj);
                            return newObj;
                        });
                    }
                });
    };
    BiConsumer<C, String> finalPaymentDate = (reservation, v) -> paymentDueDate(reservation, v, PAYMENT_NUMBER_COD);
    BiConsumer<C, String> optionDate = (reservation, v) -> paymentDueDate(reservation, v, OPTION_DATE_COD);
    BiConsumer<C, String> cancellationDate = (reservation, v) -> paymentDueDate(reservation, v, CANCELLATION_DATE_COD);
    private final Function<C, OTAResRetrieveRS.ReservationsList.CruiseReservation.TPAExtensions> traExtensions = reservation -> {
        Supplier<OTAResRetrieveRS.ReservationsList.CruiseReservation.TPAExtensions> obj = reservation::getTPAExtensions;
        return ofNullable(obj.get())
                .orElseGet(() -> {
                    synchronized (this) {
                        return ofNullable(obj.get()).orElseGet(() -> {
                            OTAResRetrieveRS.ReservationsList.CruiseReservation.TPAExtensions newObj =
                                    new OTAResRetrieveRS.ReservationsList.CruiseReservation.TPAExtensions();
                            reservation.setTPAExtensions(newObj);
                            return newObj;
                        });
                    }
                });
    };
    private final Function<C, CruiseBookingInfoType> bookingPayment = reservation -> {
        Supplier<CruiseBookingInfoType> obj = () -> traExtensions.apply(reservation).getBookingPayment();
        return ofNullable(obj.get())
                .orElseGet(() -> {
                    synchronized (this) {
                        return ofNullable(obj.get()).orElseGet(() -> {
                            CruiseBookingInfoType newObj = new CruiseBookingInfoType();
                            traExtensions.apply(reservation).setBookingPayment(newObj);
                            return newObj;
                        });
                    }
                });
    };
    private final Function<C, CruiseBookingInfoType.BookingPrices> bookingPrices = reservation -> {
        Supplier<CruiseBookingInfoType.BookingPrices> obj = () -> bookingPayment.apply(reservation).getBookingPrices();
        return ofNullable(obj.get())
                .orElseGet(() -> {
                    synchronized (this) {
                        return ofNullable(obj.get()).orElseGet(() -> {
                            CruiseBookingInfoType.BookingPrices newObj = new CruiseBookingInfoType.BookingPrices();
                            bookingPayment.apply(reservation).setBookingPrices(newObj);
                            return newObj;
                        });
                    }
                });
    };
    private final Function<C, List<CruiseBookingInfoType.BookingPrices.BookingPrice>> bookingPriceList = reservation -> {
        Supplier<List<CruiseBookingInfoType.BookingPrices.BookingPrice>> obj = () ->
                bookingPrices.apply(reservation).getBookingPrice();
        return ofNullable(obj.get())
                .orElseGet(() -> {
                    synchronized (this) {
                        return ofNullable(obj.get()).orElseGet(() -> bookingPrices.apply(reservation).getBookingPrice());
                    }
                });
    };
    BiConsumer<C, BigDecimal> amount = (reservation, v) -> {
        final List<CruiseBookingInfoType.BookingPrices.BookingPrice> bookPrices = bookingPriceList.apply(reservation);
        CruiseBookingInfoType.BookingPrices.BookingPrice booking = new CruiseBookingInfoType.BookingPrices.BookingPrice();
        booking.setAmount(v);
        booking.setPriceTypeCode(PRICE_TYPE_CODE_VIII);
        bookPrices.add(booking);
        CruiseBookingInfoType.BookingPrices.BookingPrice booking3 = new CruiseBookingInfoType.BookingPrices.BookingPrice();
        booking3.setAmount(null);
        booking3.setPriceTypeCode(PRICE_TYPE_CODE_III);
        bookPrices.add(booking3);
    };
    private final Function<C, SailingCategoryInfoType> sailingInfo = reservation -> {
        Supplier<SailingCategoryInfoType> obj = reservation::getSailingInfo;
        return ofNullable(obj.get())
                .orElseGet(() -> {
                    synchronized (this) {
                        return ofNullable(obj.get()).orElseGet(() -> {
                            SailingCategoryInfoType newObj = new SailingCategoryInfoType();
                            reservation.setSailingInfo(newObj);
                            return newObj;
                        });
                    }
                });
    };
    private final Function<C, SailingInfoType.InclusivePackageOption> inclusivePackageOption = reservation -> {
        Supplier<SailingInfoType.InclusivePackageOption> obj = () -> sailingInfo.apply(reservation).getInclusivePackageOption();
        return ofNullable(obj.get())
                .orElseGet(() -> {
                    synchronized (this) {
                        return ofNullable(obj.get()).orElseGet(() -> {
                            SailingCategoryInfoType sailInfo = sailingInfo.apply(reservation);
                            SailingInfoType.InclusivePackageOption newObj = new SailingInfoType.InclusivePackageOption();
                            sailInfo.setInclusivePackageOption(newObj);
                            return newObj;
                        });
                    }
                });
    };
    BiConsumer<C, String> cruisePackageCode = (reservation, v) -> inclusivePackageOption.apply(reservation).setCruisePackageCode(v);
    private final Function<C, SailingInfoType.Currency> currency = reservation -> {
        Supplier<SailingInfoType.Currency> obj = () -> sailingInfo.apply(reservation).getCurrency();
        return ofNullable(obj.get())
                .orElseGet(() -> {
                    synchronized (this) {
                        return ofNullable(obj.get()).orElseGet(() -> {
                            SailingCategoryInfoType sailInfo = sailingInfo.apply(reservation);
                            SailingInfoType.Currency newObj = new SailingInfoType.Currency();
                            sailInfo.setCurrency(newObj);
                            return newObj;
                        });
                    }
                });
    };
    BiConsumer<C, String> currencyCode = (reservation, v) -> currency.apply(reservation).setCurrencyCode(v);
    private final Function<C, SailingInfoType.SelectedSailing> selectedSailing = reservation -> {
        Supplier<SailingInfoType.SelectedSailing> obj = () -> sailingInfo.apply(reservation).getSelectedSailing();
        return ofNullable(obj.get())
                .orElseGet(() -> {
                    synchronized (this) {
                        return ofNullable(obj.get()).orElseGet(() -> {
                            SailingInfoType.SelectedSailing newObj = new SailingInfoType.SelectedSailing();
                            SailingCategoryInfoType sailInfo = reservation.getSailingInfo();
                            sailInfo.setSelectedSailing(newObj);
                            return newObj;
                        });
                    }
                });
    };
    BiConsumer<C, String> duration = (reservation, v) -> selectedSailing.apply(reservation).setDuration(v);
    BiConsumer<C, String> start = (reservation, v) -> selectedSailing.apply(reservation).setStart(v);
    private final Function<C, SailingBaseType.CruiseLine> cruiseLine = reservation -> {
        Supplier<SailingBaseType.CruiseLine> obj = () -> selectedSailing.apply(reservation).getCruiseLine();
        return ofNullable(obj.get())
                .orElseGet(() -> {
                    synchronized (this) {
                        return ofNullable(obj.get()).orElseGet(() -> {
                            SailingBaseType.CruiseLine newObj = new SailingBaseType.CruiseLine();
                            selectedSailing.apply(reservation).setCruiseLine(newObj);
                            return newObj;
                        });
                    }
                });
    };
    BiConsumer<C, String> shipCode = (reservation, v) -> cruiseLine.apply(reservation).setShipCode(v);
    BiConsumer<C, String> vendorCode = (reservation, v) -> cruiseLine.apply(reservation).setVendorCode(v);
    private final Function<C, SailingCategoryInfoType.SelectedCategory> selectedCategory = reservation -> {
        synchronized (this) {
            List<SailingCategoryInfoType.SelectedCategory> list = sailingInfo.apply(reservation).getSelectedCategory();
            if (list.isEmpty()) {
                list.add(new SailingCategoryInfoType.SelectedCategory());
            }
            return list.get(0);
        }
    };
    BiConsumer<C, String> pricedCategoryCode = (reservation, v) -> selectedCategory.apply(reservation).setPricedCategoryCode(v);
    private final Function<C, SailingCategoryInfoType.SelectedCategory.SelectedCabin> selectedCabin = reservation -> {
        synchronized (this) {
            List<SailingCategoryInfoType.SelectedCategory.SelectedCabin> list =
                    selectedCategory.apply(reservation).getSelectedCabin();
            if (list.isEmpty()) {
                list.add(new SailingCategoryInfoType.SelectedCategory.SelectedCabin());
            }
            return list.get(0);
        }
    };
    BiConsumer<C, String> cabinNumber = (reservation, v) -> selectedCabin.apply(reservation).setCabinNumber(v);
    BiConsumer<C, String> bedType = (reservation, v) -> selectedCabin.apply(reservation).setBedType(v);
    BiConsumer<C, String> status = (reservation, v) -> selectedCabin.apply(reservation).setStatus(v);
    BiConsumer<C, String> deckCode = (reservation, v) -> selectedCabin.apply(reservation).setDeckNumber(v);
    private final Function<C, CruiseGuestInfoType> cruiseGuestInfoType = reservation -> {
        Supplier<CruiseGuestInfoType> obj = reservation::getReservationInfo;
        return ofNullable(obj.get())
                .orElseGet(() -> {
                    synchronized (this) {
                        return ofNullable(obj.get()).orElseGet(() -> {
                            CruiseGuestInfoType newObj = new CruiseGuestInfoType();
                            reservation.setReservationInfo(newObj);
                            return newObj;
                        });
                    }
                });
    };
    private final Function<C, ReservationIDType> reservationIDType = reservation -> {
        synchronized (this) {
            List<ReservationIDType> list = cruiseGuestInfoType.apply(reservation).getReservationID();
            if (list.isEmpty()) {
                list.add(new ReservationIDType());
            }
            return list.get(0);
        }
    };
    BiConsumer<C, String> reservationID = (reservation, v) -> reservationIDType.apply(reservation).setID(v);
    BiConsumer<C, String> statusCode = (reservation, v) -> reservationIDType.apply(reservation).setStatusCode(v);
    BiConsumer<C, String> reservationIdTyp = (reservation, v) -> reservationIDType.apply(reservation).setType(v);
    BiConsumer<C, String> offerDate = (reservation, v) -> reservationIDType.apply(reservation).setOfferDate(v);
    BiConsumer<C, XMLGregorianCalendar> lastModifyDateTime = (reservation, v) -> reservationIDType.apply(reservation).setLastModifyDateTime(v);
    BiConsumer<C, CompanyNameType> companyName = (reservation, v) -> reservationIDType.apply(reservation).setCompanyName(v);
    private final Function<C, CruiseGuestInfoType.GuestDetails> guestDetails = reservation -> {
        Supplier<CruiseGuestInfoType.GuestDetails> obj = () -> cruiseGuestInfoType.apply(reservation).getGuestDetails();
        return ofNullable(obj.get())
                .orElseGet(() -> {
                    synchronized (this) {
                        return ofNullable(obj.get()).orElseGet(() -> {
                            CruiseGuestInfoType.GuestDetails newObj = new CruiseGuestInfoType.GuestDetails();
                            cruiseGuestInfoType.apply(reservation).setGuestDetails(newObj);
                            return newObj;
                        });
                    }
                });
    };
    private final Function<C, CruiseGuestDetailType> guestDetail = reservation -> {
        synchronized (this) {
            List<CruiseGuestDetailType> list = guestDetails.apply(reservation).getGuestDetail();
            if (list.isEmpty()) {
                list.add(new CruiseGuestDetailType());
            }
            return list.get(0);
        }
    };
    private final Function<C, CruiseGuestDetailType.ContactInfo> contactInfos = reservation -> {
        synchronized (this) {
            List<CruiseGuestDetailType.ContactInfo> list = guestDetail.apply(reservation).getContactInfo();
            if (list.isEmpty()) {
                list.add(new CruiseGuestDetailType.ContactInfo());
            }
            return list.get(0);
        }
    };
    BiConsumer<C, String> passengerId = (reservation, v) -> contactInfos.apply(reservation).setGuestRefNumber(v);
    BiConsumer<C, String> consumerId = (reservation, v) -> contactInfos.apply(reservation).setRPH(v);
    private final Function<C, PersonNameType> personNameType = reservation -> {
        Supplier<PersonNameType> obj = () -> contactInfos.apply(reservation).getPersonName();
        return ofNullable(obj.get())
                .orElseGet(() -> {
                    synchronized (this) {
                        return ofNullable(obj.get()).orElseGet(() -> {
                            PersonNameType newObj = new PersonNameType();
                            contactInfos.apply(reservation).setPersonName(newObj);
                            return newObj;
                        });
                    }
                });
    };
    BiConsumer<C, String> givenName = (reservation, v) -> personNameType.apply(reservation).getGivenName().add(v);
    BiConsumer<C, String> surname = (reservation, v) -> personNameType.apply(reservation).setSurname(v);

    private void paymentDueDate(C reservation, String v, int paymentNumber) {
        CruiseReservationType.PaymentsDue paymentsDues = paymentsDue.apply(reservation);
        List<CruiseReservationType.PaymentsDue.PaymentDue> paymentDueList = paymentsDues.getPaymentDue();
        CruiseReservationType.PaymentsDue.PaymentDue paymentDue = new CruiseReservationType.PaymentsDue.PaymentDue();
        paymentDueList.add(paymentDue);
        paymentDue.setDueDate(v);
        paymentDue.setPaymentNumber(paymentNumber);
    }

}

