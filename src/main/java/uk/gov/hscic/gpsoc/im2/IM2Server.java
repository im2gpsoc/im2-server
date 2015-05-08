package uk.gov.hscic.gpsoc.im2;

import java.util.List;

import javax.servlet.ServletException;

import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.jpa.dao.IFhirSystemDao;
import ca.uhn.fhir.jpa.provider.JpaConformanceProviderDstu2;
import ca.uhn.fhir.jpa.provider.JpaSystemProviderDstu2;
import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.narrative.DefaultThymeleafNarrativeGenerator;
import ca.uhn.fhir.rest.server.ETagSupportEnum;
import ca.uhn.fhir.rest.server.EncodingEnum;
import ca.uhn.fhir.rest.server.FifoMemoryPagingProvider;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.interceptor.LoggingInterceptor;

public class IM2Server extends RestfulServer {

	private static final long serialVersionUID = 1L;

	private WebApplicationContext appCtx;

	@SuppressWarnings("unchecked")
	@Override
	protected void initialize() throws ServletException {
		super.initialize();

		/* Use DSTU2 */
		setFhirContext(new FhirContext(FhirVersionEnum.DSTU2));

		appCtx = ContextLoaderListener.getCurrentWebApplicationContext();

		/* Set resource providers - DSTU2 */ 
		List<IResourceProvider> beans = appCtx.getBean("myResourceProvidersDstu2", List.class);
		setResourceProviders(beans);
		
		/* Set system provider (for non-resource-type methods, such as transaction) - DSTU2 */
		setPlainProviders(appCtx.getBean("mySystemProviderDstu2", JpaSystemProviderDstu2.class));

		/* Set conformance provider - DSTU2 */
		IFhirSystemDao<Bundle> systemDao = appCtx.getBean("mySystemDaoDstu2", IFhirSystemDao.class);
		JpaConformanceProviderDstu2 confProvider = new JpaConformanceProviderDstu2(this, systemDao);
		confProvider.setImplementationDescription("GPSoC IM2 Server");
		setServerConformanceProvider(confProvider);

		/* Enable ETag Support */
		setETagSupport(ETagSupportEnum.ENABLED);

		/* Dynamically generate narratives */
		getFhirContext().setNarrativeGenerator(new DefaultThymeleafNarrativeGenerator());

		/* Use "browser friendly" MIME types if the request is from a browser (not FHIR-compliant) */
		setUseBrowserFriendlyContentTypes(true);

		/* Default to XML */
		setDefaultResponseEncoding(EncodingEnum.XML);

		setDefaultPrettyPrint(true);

		/* Keep last 10 searches in memory */
		setPagingProvider(new FifoMemoryPagingProvider(10));

		/* Configure logging */
		LoggingInterceptor loggingInterceptor = new LoggingInterceptor();
		loggingInterceptor.setLoggerName("fhir.access");
		loggingInterceptor.setMessageFormat("Path[${servletPath}] Operation[${operationType} ${idOrResourceName}] UA[${requestHeader.user-agent}] Params[${requestParameters}]");
		this.registerInterceptor(loggingInterceptor);

	}

}
