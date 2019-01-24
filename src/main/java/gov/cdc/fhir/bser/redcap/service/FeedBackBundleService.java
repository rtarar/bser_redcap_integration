package gov.cdc.fhir.bser.redcap.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import lombok.Data;
@Data
@Service
public class FeedBackBundleService {

		
		
		@Value("classpath:Feedback-A.xml")
		private Resource resource;
		
		private String xml;
		
		public FeedBackBundleService() {}
		
		
		@PostConstruct
		private void init() {
			
			
			try {
				xml= new BufferedReader(new InputStreamReader(resource.getInputStream()))
						  .lines().collect(Collectors.joining("\n"));
				resource.getInputStream();
			}catch(Exception e) {
				
			}
		}
		
	
}
