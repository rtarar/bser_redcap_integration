package gov.cdc.fhir.bser.redcap.service;

import gov.cdc.fhir.bser.redcap.model.RequestReferalInstrument;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent;
import org.springframework.stereotype.Component;

import com.google.gson.internal.LinkedTreeMap;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class FHIRProxy {
    public static final String VITAL_SIGN_HEIGHT = "8302-2";
    public static final String VITAL_SIGN_WEIGHT = "29463-7";
    public static final String VITAL_SIGN_A1C = "4548-4";
    public static final String PRACTITIONER_ROLE_PROFILE = "http://hl7.org/fhir/us/bser/StructureDefinition/ReferralInitiatorPractitionerRole";

    
    
    public RequestReferalInstrument processReferral(Bundle results) {
        RequestReferalInstrument instrument = new RequestReferalInstrument();
        List<Bundle.BundleEntryComponent> entries = results.getEntry();
        instrument.setRecordId(results.getId());
        for (Bundle.BundleEntryComponent bundleEntryComponent : entries) {
            Resource resource = bundleEntryComponent.getResource();
            if ((ResourceType.MessageHeader).name().equalsIgnoreCase(resource.getResourceType().name())) {
                MessageHeader mheader = (MessageHeader) resource;
                Practitioner receiver = (Practitioner) mheader.getReceiver().getResource();
                instrument.setReferralPractitionerName(receiver.getNameFirstRep().getNameAsSingleString());
                instrument.setReferralPractitionerPhone(receiver.getTelecomFirstRep().getValue());
            } else if ((ResourceType.Patient).name().equalsIgnoreCase(resource.getResourceType().name())) {
                Patient patient = (Patient) resource;
                instrument.setPatientMRNumber(patient.getIdentifierFirstRep().getValue()); //improve to go throu list of indentifiers
                instrument.setPatientName(patient.getNameFirstRep().getNameAsSingleString());
                instrument.setPatientPhone(patient.getTelecomFirstRep().getValue());
                instrument.setPatientAge(getAge(patient.getBirthDate()) + "");
            } else if ((ResourceType.PractitionerRole).name().equalsIgnoreCase(resource.getResourceType().name())) {
                PractitionerRole prole = (PractitionerRole) resource;
                if (prole.getMeta() != null && prole.getMeta().getProfile() != null && prole.getMeta().getProfile().size() > 0 && PRACTITIONER_ROLE_PROFILE.equalsIgnoreCase(prole.getMeta().getProfile().get(0).getValue())) {
                    Organization org = (Organization) prole.getOrganization().getResource();
                    instrument.setReferralOrganizationName(org.getName());
                    instrument.setReferralOrganizationType(org.getTypeFirstRep().getCodingFirstRep().getDisplay());
                }
            } else if ((ResourceType.Bundle.name().equalsIgnoreCase(resource.getResourceType().name()))) { //Vital Signs Bundle:
                Bundle bundle = (Bundle) resource;
                for (Bundle.BundleEntryComponent vitalSigns : bundle.getEntry()) {
                    try {
                        Observation vitalSign = (Observation) vitalSigns.getResource();
                        String value = vitalSign.getValueQuantity().getValue() + " " + vitalSign.getValueQuantity().getUnit();
                        switch (vitalSign.getCode().getCodingFirstRep().getCode()) {
                            case VITAL_SIGN_WEIGHT:
                                instrument.setPatientWeight(value);
                                break;
                            case VITAL_SIGN_HEIGHT:
                                instrument.setPatientHeight(value);
                                break;
                            case VITAL_SIGN_A1C:
                                instrument.setPatientA1CObservation(value);
                                break;
                        }
                    } catch (ClassCastException e) {
                        //ignore exception - not interested in anything other than observations!
                    }
                }

            }
        }
        return instrument;
    }
    
    public void processFeedBack(Map<String,Object> map,Bundle feedBackBundle){
    	List<Resource> theResources = processFeedBackBundle(feedBackBundle,map);
    	
    }
    
    
    public  List<Resource> processFeedBackBundle(Bundle bundle,Map map) {
        //generate a list of resources...
        List<Resource> theResources = new ArrayList<Resource>();    //list of resources in bundle
        for (BundleEntryComponent entry : bundle.getEntry()) {
            theResources.add(entry.getResource());
        }
        return process(theResources,map);
    }
    
    
    private  Patient getPatient(List<Resource> theResources,Map<String,String> map) {
    	
    	for(Resource resource : theResources) {
     	   System.out.println(resource.getResourceType());
     	   if(ResourceType.Patient.equals(resource.getResourceType())) {
     		   Patient p = (Patient)resource;
     		   return p;
     	   }
    	}
    	return null;
    }
    
    
    private  List<Resource> process(List<Resource> theResources,Map<String,String> map) {
        Patient patient = getPatient(theResources,map);     //this will be the patient resource. There should only be one....
        List<Resource> processed = new ArrayList<Resource>();
        
       for(Resource resource : theResources) {
    	   System.out.println(resource.getResourceType());
    	   if(ResourceType.Patient.equals(resource.getResourceType())) {
    		   Patient p = (Patient)resource;
    		   System.out.println(p.getIdentifierFirstRep().getValue());
    		   patient= p;
       		   processed.add(processPatient(p,map));
    	   }else if(ResourceType.MessageHeader.equals(resource.getResourceType())) {
    		   //NOOP Just use the same from the xml
    		   
    	   }else if(ResourceType.Composition.equals(resource.getResourceType())){
    		   Composition c = (Composition) resource;
    		  // c.getIdentifier().setValue(map.get("feedback_referral_id"));
    		   Narrative n = new Narrative();
    		   n.setDivAsString(map.get("feedback_note"));
    		   c.getSectionFirstRep().setText(n);
    		   c.setSubject(new Reference(patient.getId()));
    		   processed.add(c);
    	   }else if(ResourceType.Observation.equals(resource.getResourceType())){
    		   //Education Level 
    	   }else if(ResourceType.Bundle.equals(resource.getResourceType())){
    		   Bundle bundle = (Bundle) resource;
    		   for (BundleEntryComponent entry : bundle.getEntry()) {
    	           Observation o = (Observation)entry.getResource();
    	           if("39156-5".equals(o.getCode().getCodingFirstRep().getCode())){
    	        	   //BMI
    	        	   o.setValue(new Quantity(Long.parseLong(map.get("visit_patient_bmi"))));
    	        	   
    	        	   
    	           }else if("29463-7".equals(o.getCode().getCodingFirstRep().getCode())){
    	        	   //body weight
    	        	   
    	        	   o.setValue(new Quantity(Long.parseLong(map.get("visit_patient_weight"))));
    	        	   
    	           }else if("8302-2".equals(o.getCode().getCodingFirstRep().getCode())){
    	        	   //body height
    	        	   
    	        	   o.setValue(new Quantity(Long.parseLong(map.get("visit_patient_height"))));
    	        	   
    	           }else if("4548-4".equals(o.getCode().getCodingFirstRep().getCode())){
    	        	   //a1c count
    	        	   
    	        	   o.setValue(new Quantity(Long.parseLong(map.get("visit_a1c_count"))));
    	        	   
    	           }
    	           //set the patient link
    	           o.setSubject(new Reference(patient.getId()));
    	           System.out.println(o.getValueQuantity().getValue());
    	        }
    		   processed.add(bundle);
    	   }
       }
 
       return theResources;
    }
    
    public  Patient processPatient(Patient p ,Map<String,String> map) {
    	
    	p.getIdentifierFirstRep().setValue(map.get("patient_mr_number"));
    	p.getNameFirstRep()
    			.setFamily(map.get("patient_family_name"))
    			.addGiven(map.get("patient_given_name"))
    			.addPrefix(map.get("patient_name_prefix"));
    	//p.setBirthDate(new Date((map.get("patient_age"))));
    	return p;
    }
    
    

    private static String getAddress(Address address) {
        String str = "";
        str = str + (address.getLine()).get(0);
        str = str + "," + address.getCity();
        str = str + "," + address.getState();
        str = str + "," + address.getPostalCode();
        return str;
    }


    public static int getAge(Date dateOfBirth) {

        Calendar today = Calendar.getInstance();
        Calendar birthDate = Calendar.getInstance();

        int age = 0;

        birthDate.setTime(dateOfBirth);
        if (birthDate.after(today)) {
            //throw new IllegalArgumentException("Can't be born in the future");
            return age;
        }

        age = today.get(Calendar.YEAR) - birthDate.get(Calendar.YEAR);

        // If birth date is greater than todays date (after 2 days adjustment of leap year) then decrement age one year
        if ((birthDate.get(Calendar.DAY_OF_YEAR) - today.get(Calendar.DAY_OF_YEAR) > 3) ||
                (birthDate.get(Calendar.MONTH) > today.get(Calendar.MONTH))) {
            age--;

            // If birth date and todays date are of same month and birth day of month is greater than todays day of month then decrement age
        } else if ((birthDate.get(Calendar.MONTH) == today.get(Calendar.MONTH)) &&
                (birthDate.get(Calendar.DAY_OF_MONTH) > today.get(Calendar.DAY_OF_MONTH))) {
            age--;
        }

        return age;
    }
}
