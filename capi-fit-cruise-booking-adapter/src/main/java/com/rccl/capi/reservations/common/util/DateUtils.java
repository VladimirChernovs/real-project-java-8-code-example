package com.rccl.capi.reservations.common.util;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Optional;

import static java.util.Optional.empty;
import static java.util.Optional.of;

public final class DateUtils {

    private static final String MM_DD_YYYY = "MM/dd/yyyy";

    // not for instantiation
    private DateUtils() {
    }

    // DATE UTILS

    // from "02/22/2019" to-> XMLGregorianCalendar "2019-02-22T00:00:00Z"
    public static Optional<XMLGregorianCalendar> toXMLGregorianCalendar(String stringDate) {
        return getDateFromPattern(stringDate, MM_DD_YYYY).flatMap(localDate -> {
            final String FORMATTER = "yyyy-MM-dd'T'HH:mm:ss";
            try {
                GregorianCalendar gc = new GregorianCalendar();
                Date date = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
                gc.setTime(date);
                DateFormat format = new SimpleDateFormat(FORMATTER);
                XMLGregorianCalendar result = DatatypeFactory.newInstance().newXMLGregorianCalendar(format.format(date));
                result.setTimezone(0);
                return of(result);
            } catch (DatatypeConfigurationException e) {
                return empty();
            }
        });
    }

    public static Optional<BigDecimal> convertStringDateToBigDecimal(String stringDate) {
        return getOnlyDateFromString(stringDate)
                .flatMap(CommonArt::stringToBigDecimal);
    }

    // "03/17/2039 -> 2039-03-17
    public static Optional<String> toDashedYyyyMmDdDateFromSlashedDdMMMyyyy(String stringDate) {
        return getDateFromPattern(stringDate, MM_DD_YYYY)
                .flatMap(x -> formatDataFromPattern(x, "yyyy-MM-dd"));
    }

    // 01/16/2019 -> 20190116
    public static Optional<BigDecimal> toBigDesYyyyMmDdFromSlashedMmDdYyyy(String stringDate) {
        return getDateFromPattern(stringDate, MM_DD_YYYY)
                .flatMap(x -> formatDataFromPattern(x, "yyyyMMdd")).map(BigDecimal::new);
    }

    // 2020-01-16 -> 01/16/2020
    public static Optional<String> toSlashedMmDdYyyyDateFromDashedYyyyMmDd(String stringDate) {
        return getDateFromPattern(stringDate, "yyyy-MM-dd")
                .flatMap(x -> formatDataFromPattern(x, MM_DD_YYYY));
    }

    private static Optional<String> formatDataFromPattern(LocalDate date, String pattern) {
        DateTimeFormatter outputFormatter;
        try {
            outputFormatter = DateTimeFormatter.ofPattern(pattern);
        } catch (IllegalArgumentException e) {
            return empty();
        }
        return of(date.format(outputFormatter));
    }

    private static Optional<LocalDate> getDateFromPattern(String inputDate, String pattern) {
        try {
            DateTimeFormatter inputFormatter = new DateTimeFormatterBuilder()
                    .parseCaseInsensitive()
                    .appendPattern(pattern)
                    .toFormatter(Locale.ENGLISH);
            return of(LocalDate.parse(inputDate, inputFormatter));
        } catch (DateTimeParseException e) {
            return empty();
        }
    }

    private static Optional<String> getOnlyDateFromString(String in) {
        final int DATE_LENGTH = 8;
        return onlyNumbers(in)
                .flatMap(s -> {
                    int inLength = s.length();
                    if (inLength < DATE_LENGTH) return empty();
                    if (inLength == DATE_LENGTH) return of(s);
                    String onlyDate = s.substring(0, DATE_LENGTH);
                    return of(onlyDate);
                });
    }

    private static Optional<String> onlyNumbers(String str) {
        if (str == null) return empty();
        String numbers = str.replaceAll("[^0-9.]", "");
        return of(numbers);
    }

}
