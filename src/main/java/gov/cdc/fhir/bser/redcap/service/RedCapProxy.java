package gov.cdc.fhir.bser.redcap.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import gov.cdc.fhir.bser.redcap.model.RedCapFeedbackInstrument;
import gov.cdc.fhir.bser.redcap.model.RequestReferalInstrument;

/**
 * @Created - 2019-01-04
 * @Author Marcelo Caldas mcq1@cdc.gov
 */
@Component
public class RedCapProxy {

    Log logger = LogFactory.getLog(RedCapProxy.class.getName());

    private final HttpPost post;
   
    private final HttpClient client;
    private final StringBuffer result;
    private int respCode;
    private BufferedReader reader;
    private String line;


    @Value("${redcap.api.url}")
    private String _redcapURL;
    @Value("${redcap.api.token}")
    private String _token;


    public RedCapProxy(@Value("${redcap.api.url}") String url, @Value("${redcap.api.token}") String token) {
        _redcapURL = url;
        _token = token;

        post = new HttpPost(_redcapURL);
        post.setHeader("Content-Type", "application/x-www-form-urlencoded");

        result = new StringBuffer();
        client = HttpClientBuilder.create().build();
        respCode = -1;
        reader = null;
        line = null;
    }

    public void saveReferral(RequestReferalInstrument newRecord) {
        HttpResponse resp = null;
        Gson gson = new Gson();

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("token", _token));
        params.add(new BasicNameValuePair("content", "record"));
        params.add(new BasicNameValuePair("format", "json"));
        params.add(new BasicNameValuePair("returnFormat", "json"));
        params.add(new BasicNameValuePair("type", "flat"));
        params.add(new BasicNameValuePair("overwriteBehavior", "overwrite"));
        params.add(new BasicNameValuePair("forceAutoNumber", "false"));
        params.add(new BasicNameValuePair("returnContent", "count"));
        //params.add(new BasicNameValuePair("data", "[{\"record_id\":\"6\",\"firstname\":\"Spring\",\"lastname\":\"Boot\",\"age\":\"2\"}]"));
        params.add(new BasicNameValuePair("data", "[" + gson.toJson(newRecord) + "]"));

        try {
            post.setEntity(new UrlEncodedFormEntity(params));
            resp = client.execute(post);
        } catch (final Exception e) {
            e.printStackTrace();
        }

        if (resp != null) {
            respCode = resp.getStatusLine().getStatusCode();

            try {
                reader = new BufferedReader(new InputStreamReader(resp.getEntity().getContent()));
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }

        if (reader != null) {
            try {
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }

        logger.info("respCode: " + respCode);
        logger.info("result: " + result.toString());
    }
    
    public Map<String , Object> getFeedBackData(RedCapFeedbackInstrument feedback) {
    
    	//two subsequent calls will have to be made
    	//1. Call redcap to glean selected data (Patient info , Patient Id's)from the referral request form 
    	//2. Call redcap to glean Observation Data from the visit form (bmi height weight etc)
    	//3. get the feedback note
    	
    	//When all data is received then start creating the feedback Bundle with the values recieved.
    	if(!feedback.isFeedBackTrigger()) {
    		//as the create feedback form was not completed and clicked save.
    		//this is a catch all for all other form saves  in referral and visit form clicks should not process
    		return null;
    	}
    	HttpResponse resp = null;
        Gson gson = new Gson();

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("token", _token));
        params.add(new BasicNameValuePair("content", "record"));
        params.add(new BasicNameValuePair("format", "json"));
        params.add(new BasicNameValuePair("returnFormat", "json"));
        params.add(new BasicNameValuePair("type", "flat"));
        params.add(new BasicNameValuePair("overwriteBehavior", "normal"));
        params.add(new BasicNameValuePair("forceAutoNumber", "false"));
        params.add(new BasicNameValuePair("returnContent", "count"));
        //params.add(new BasicNameValuePair("data", "[{\"record_id\":\"6\",\"firstname\":\"Spring\",\"lastname\":\"Boot\",\"age\":\"2\"}]"));
       
        params.add(new BasicNameValuePair("records", feedback.getRecord()));
        //String fields = "bser_referral_request_complete,bser_visit_complete,create_feedback_complete,creating_feedback_for_pati,feedback_note,patient_a1cobservation,patient_age,patient_height,patient_mr_number,patient_name,patient_phone,patient_weight,record_id,referral_organization_name,referral_organization_type,referral_practitioner_name,referral_practitioner_phone,visit_a1c_count,visit_patient_bmi,visit_patient_height,visit_patient_weight";
        String fields = "feedback_note,patient_a1cobservation,patient_age,patient_height,patient_mr_number,patient_name,patient_phone,patient_weight,record_id,referral_organization_name,referral_organization_type,referral_practitioner_name,referral_practitioner_phone,visit_a1c_count,visit_patient_bmi,visit_patient_height,visit_patient_weight";
       
        params.add(new BasicNameValuePair("fields", fields));
                
        String forms =  "bser_referral_request,bser_visit,create_feedback";
        params.add(new BasicNameValuePair("forms", forms));
        
       
        params.add(new BasicNameValuePair("rawOrLabel", "raw"));
        params.add(new BasicNameValuePair("rawOrLabelHeaders", "raw"));
        params.add(new BasicNameValuePair("exportCheckboxLabel", "false"));
        params.add(new BasicNameValuePair("exportSurveyFields", "false"));
        params.add(new BasicNameValuePair("exportDataAccessGroups", "false"));
        params.add(new BasicNameValuePair("returnFormat", "json"));
        
        
        try {
            post.setEntity(new UrlEncodedFormEntity(params));
            resp = client.execute(post);
        } catch (final Exception e) {
            e.printStackTrace();
        }

        if (resp != null) {
            respCode = resp.getStatusLine().getStatusCode();

            try {
                reader = new BufferedReader(new InputStreamReader(resp.getEntity().getContent()));
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
        StringBuffer buf = new StringBuffer();
        if (reader != null) {
            try {
                while ((line = reader.readLine()) != null) {
                    buf.append(line);
                }
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }

        logger.info("respCode: " + respCode);
        logger.info("result: " + buf.toString());
        
        /**
         * [
         *  {"record_id":"44","redcap_event_name":"referral_received_arm_1","referral_organization_name":"Alliance Of Chicago Therapeutic Services and Supplies","referral_organization_type":"Healthcare Provider","referral_practitioner_name":"Dr. Udaya Liyanage","referral_practitioner_phone":"(618) 942-2002","patient_mr_number":"F255-9215-0094","patient_name":"Bradley Beal","patient_age":"68","patient_phone":"(999) 607-2500","patient_height":"182.88 cm","patient_weight":"105 kg","patient_a1cobservation":"128 mg/dL","visit_patient_weight":"","visit_patient_height":"","visit_patient_bmi":"","visit_a1c_count":"","feedback_note":""},
         *  {"record_id":"44","redcap_event_name":"visit_arm_1","referral_organization_name":"","referral_organization_type":"","referral_practitioner_name":"","referral_practitioner_phone":"","patient_mr_number":"","patient_name":"","patient_age":"","patient_phone":"","patient_height":"","patient_weight":"","patient_a1cobservation":"","visit_patient_weight":"105 kg","visit_patient_height":"182.88 cm","visit_patient_bmi":"[referral_received_arm_1][patient_bmi]","visit_a1c_count":"128 mg/dL","feedback_note":""},
         *  {"record_id":"44","redcap_event_name":"create_feedback_arm_1","referral_organization_name":"","referral_organization_type":"","referral_practitioner_name":"","referral_practitioner_phone":"","patient_mr_number":"","patient_name":"","patient_age":"","patient_phone":"","patient_height":"","patient_weight":"","patient_a1cobservation":"","visit_patient_weight":"","visit_patient_height":"","visit_patient_bmi":"","visit_a1c_count":"","feedback_note":"This is a Note for the Patient from the YMCA"}
         * ]
         */
        
        ArrayList array = new Gson().fromJson(buf.toString(), ArrayList.class);
        
        Map map = getMapfromArray(array);
       
		//return Convert.fromJson(jsonObject);
        return map;
    }
    
    private HashMap getMapfromArray(ArrayList list) {
    	Map<String, Object> returnMap = new HashMap();
    	Gson g = new Gson();
    	JSONObject use = new JSONObject();
    	for (Object object : list) {
    		String str  = g.toJson(object, HashMap.class);
    		JSONObject o = new JSONObject(str);
    		String redcapevent = o.getString("redcap_event_name");
    			
    		if(redcapevent.equalsIgnoreCase("referral_received_arm_1")) {
    			use=o;
    		}else if(redcapevent.equalsIgnoreCase("visit_arm_1")) {
    			use.put("visit_patient_bmi", o.get("visit_patient_bmi"));
    			use.put("visit_patient_weight", o.get("visit_patient_weight"));
    			use.put("visit_patient_height", o.get("visit_patient_height"));
    			use.put("visit_a1c_count", o.get("visit_a1c_count"));
    			
    		}else if(redcapevent.equalsIgnoreCase("create_feedback_arm_1")) {
    			use.put("feedback_note", o.get("feedback_note"));
    		}
    		
		}
    		return (HashMap) use.toMap();
    	
    }
    
    
}
