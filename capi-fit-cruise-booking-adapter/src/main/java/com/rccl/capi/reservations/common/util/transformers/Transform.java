package com.rccl.capi.reservations.common.util.transformers;

import com.rccl.capi.reservations.common.util.CodeLookupUtils;
import com.rccl.capi.reservations.common.util.CommonArt;
import com.rccl.capi.reservations.common.util.DateUtils;
import org.apache.commons.lang3.StringUtils;
import org.opentravel.ota._2003._05.alpha.CompanyNameType;
import org.opentravel.ota._2003._05.alpha.YesNoType;

import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Optional.*;

public abstract class Transform {
    private Transform() {
    }

    public static final Function<String, Optional<String>> test = Optional::of;

    public static final Function<BigInteger, Optional<BigDecimal>> bigIntegerToBigDecimal = v -> ofNullable(BigDecimal.valueOf(v.intValue()));

    public static final Function<BigDecimal, Optional<BigInteger>> bigDecimalToBigInteger = v -> ofNullable(v.toBigInteger());

    public static final Function<String, Optional<BigDecimal>> stringToBigDecimal = CommonArt::stringToBigDecimal;

    public static final Function<BigDecimal, Optional<String>> bigDecimalToStringId = v -> of(Integer.toString(v.intValue()));

    public static final Function<String, Optional<BigDecimal>> stringDateToBigDecimal = DateUtils::convertStringDateToBigDecimal;

    public static final Function<String, Optional<String>> toDashedYyyyMmDd = DateUtils::toDashedYyyyMmDdDateFromSlashedDdMMMyyyy;
    public static final Function<String, Optional<String>> toSlashedMmDdYyyy = DateUtils::toSlashedMmDdYyyyDateFromDashedYyyyMmDd;

    public static final Function<String, Optional<Boolean>> stringToBoolean = v -> LookUp.out(Table.BOOLEANS, v)
            .filter(c -> StringUtils.equalsAny(c.toUpperCase(), "TRUE", "FALSE"))
            .map(Boolean::valueOf);

    public static final Function<YesNoType, Optional<String>> fromYesNo = v -> LookUp.in(Table.YES_NO_TYPE, v.value());
    public static final Function<String, Optional<YesNoType>> toYesNo = v -> {
        try {
            return LookUp.out(Table.YES_NO_TYPE, v).map(YesNoType::fromValue);
        } catch (IllegalArgumentException e) {
            return empty();
        }
    };

    public static final Function<BigDecimal, Optional<String>> toDuration = v -> of("P" + v.toString() + "N");

    public static final Supplier<Optional<String>> toCabinType = () -> LookUp.out(Table.UIT, "Reservation");

    public static final Function<String, Optional<String>> toStsReservationStatus = v -> LookUp.out(Table.STS_RESERVATION_STATUS, v);

    public static final Function<String, Optional<String>> vendorToCruiseLineCode = v -> LookUp.in(Table.BRAND_RQ3A, v);

    public static final Function<String, Optional<String>> transactionIdentifier2groupOrIndividual = v -> LookUp.in(Table.RES_TYP, v);

    public static final Function<String, Optional<String>> folderType = v -> LookUp.in(Table.FOLDER_TYPE, v);

    public static final Function<String, Optional<String>> bookingType = v -> LookUp.in(Table.BOOKING_TYPE, v);

    public static final Function<String, Optional<BigDecimal>> toBdYyyyMmDd = DateUtils::toBigDesYyyyMmDdFromSlashedMmDdYyyy;

    public static final Function<String, Optional<CompanyNameType>> toCompanyNameType = v -> {
        CompanyNameType companyNameType = new CompanyNameType();
        companyNameType.setCompanyShortName(v);
        return of(companyNameType);
    };

    public static final Function<String, Optional<XMLGregorianCalendar>> toXMLGregorianCalendar = DateUtils::toXMLGregorianCalendar;
}

/**
 * Look Up util
 */
interface LookUp {

    static Optional<String> out(Table table, String key) {
        return lookUpAndTranslate(true, table, key);
    }

    static Optional<String> in(Table table, String key) {
        return lookUpAndTranslate(false, table, key);
    }

    static Optional<String> lookUpAndTranslate(boolean ota, Table table, String key) {
        String code;
        try {
            if (ota) {
                code = CodeLookupUtils.getOtaCode(table.getName(), key);
            } else {
                code = CodeLookupUtils.getRapiCode(table.getName(), key);
            }
        } catch (Exception e) {
            return empty();
        }
        return ofNullable(code).filter(StringUtils::isNotBlank);
    }

}

