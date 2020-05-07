package com.rccl.capi.reservations.common.util;

import generated.Lookup;
import generated.LookupList;

import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CodeLookupUtils {

    private static Map<String, Lookup> lookupById;

    private CodeLookupUtils() {
    }

    public static void initialize(LookupList lookupList) {
        if (lookupById == null) {
            updateLookupList(lookupList);
        }
    }

    static void updateLookupList(LookupList lookupList) {
        lookupById = lookupList.getLookup().stream().collect(Collectors.toMap(Lookup::getId, lookup -> lookup,
                (oldValue, newValue) -> oldValue));
    }

    public static String getOtaCode(String lookupId, String rapiCode) {
        Predicate<generated.Map> searchByRapiCode = map -> map.getRapiCode().equalsIgnoreCase(rapiCode);
        Optional<generated.Map> result = getMapCode(lookupId, searchByRapiCode);
        return result.isPresent() ? result.get().getOtaCode() : "";
    }

    public static String getRapiCode(String lookupId, String otaCode) {
        Predicate<generated.Map> searchByOtaCode = map -> map.getOtaCode().equalsIgnoreCase(otaCode);
        Optional<generated.Map> result = getMapCode(lookupId, searchByOtaCode);
        return result.isPresent() ? result.get().getRapiCode() : "";
    }

    private static Optional<generated.Map> getMapCode(String lookupId, Predicate<generated.Map> filterCriteria) {
        Supplier<Stream<generated.Map>> supplier = () -> lookupById.get(lookupId).getMapList().getMap().stream();
        return supplier.get().filter(filterCriteria).findAny();
    }

}