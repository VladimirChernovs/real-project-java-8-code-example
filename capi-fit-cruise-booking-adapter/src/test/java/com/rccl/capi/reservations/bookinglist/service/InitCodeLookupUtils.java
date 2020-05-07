package com.rccl.capi.reservations.bookinglist.service;

import com.rccl.capi.reservations.common.util.CodeLookupUtils;
import generated.LookupList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.net.URL;


public interface InitCodeLookupUtils {
    Logger log = LoggerFactory.getLogger(InitCodeLookupUtils.class);

    static boolean done(Class clazz) {
        try {
            ClassLoader classLoader = clazz.getClassLoader();
            URL codeLookupFile = classLoader.getResource("codeLookup.xml");
            JAXBContext jaxbContext = JAXBContext.newInstance(LookupList.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            LookupList lookupList = (LookupList) unmarshaller.unmarshal(codeLookupFile);
            CodeLookupUtils.initialize(lookupList);
            return true;
        } catch (JAXBException e) {
            log.error("Error InitCodeLookupUtils! Msg - {}", e.getMessage());
            return false;
        }
    }

}
