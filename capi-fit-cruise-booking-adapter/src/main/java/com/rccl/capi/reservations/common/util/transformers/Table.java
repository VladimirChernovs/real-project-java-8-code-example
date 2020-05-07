package com.rccl.capi.reservations.common.util.transformers;

public enum Table {
    OTA_VERSION_INFO("OTA_Version_Info"),
    BOOLEANS("Boolean"),
    RES_TYP("RES_TYP"),
    FOLDER_TYPE("FOLDER_TYPE"),
    BOOKING_TYPE("BOOKING_TYPE"),
    YES_NO_TYPE("YesNoType"),
    STS_RESERVATION_STATUS("STS_ReservationStatus"),
    UIT("UIT"),
    BRAND_RQ3A("Brand_RQ3A");

    private String name;

    Table(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
