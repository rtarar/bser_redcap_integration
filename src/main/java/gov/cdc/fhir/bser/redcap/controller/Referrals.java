package gov.cdc.fhir.bser.redcap.controller;


import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import gov.cdc.fhir.bser.redcap.model.RedCapFeedbackInstrument;
import gov.cdc.fhir.bser.redcap.model.RequestReferalInstrument;
import gov.cdc.fhir.bser.redcap.service.FHIRProxy;
import gov.cdc.fhir.bser.redcap.service.FeedBackBundleService;
import gov.cdc.fhir.bser.redcap.service.RedCapProxy;

import java.util.List;
import java.util.Map;

import org.hl7.fhir.dstu3.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.json.GsonJsonParser;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value="/referral")
public class Referrals {
	Logger logger = LoggerFactory.getLogger(Referrals.class);
    @Autowired
    private FHIRProxy fhirProxy;
    @Autowired
    private RedCapProxy redCapProxy;
    @Autowired
    private FeedBackBundleService feeBackXML;

    @PutMapping("Bundle/{bundleId}")
    public String receiveReferral(@PathVariable String bundleId, @RequestBody(required=false) String body) {
        System.out.println("body = \n\n" + body + "\n\n");
        Bundle b = getBundle(body);
        if (b!=null) {
            RequestReferalInstrument redCapInstrument = fhirProxy.processReferral(b);
            redCapProxy.saveReferral(redCapInstrument);
            return "Bundle OK";
        } else {
            return "Empty Payload";
        }
    }

    //TODO::RISHI to Add Code here!
    @PostMapping(value="/feedback", consumes= MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String processFeedback(RedCapFeedbackInstrument feedback) {
        System.out.println("body = " + feedback);
        Map<String,Object> map = redCapProxy.getFeedBackData(feedback);
        fhirProxy.processFeedBack(map,getBundle(feeBackXML.getXml()));
        return "OK";
    }
    
    @RequestMapping(value="/ping")
    public String ping() {
    	return "I am alive";
    }

    //This method parsers either XML or JSON content:
    private Bundle getBundle(String body) {
        FhirContext ctx = FhirContext.forDstu3();
        IParser parser;
        if (body != null && body.trim().length() >0 ) {
            if (body.startsWith("<")) {
                parser = ctx.newXmlParser();
            } else {
                parser = ctx.newJsonParser();
            }
            return (Bundle) parser.parseResource(body);
        }
        return null;
    }



    @PostMapping
    public String receiveNewReferral(@RequestBody(required=false) String body) {
        System.out.println("body = " + body);
        return "ok";
    }
    @PostMapping("$process-message")
    public String receiveReferralMessage(@RequestBody(required=false) String body) {
        System.out.println("Processing referral mressage");
        System.out.println("body = " + body);
        return ("Message OK!");
    }



}
