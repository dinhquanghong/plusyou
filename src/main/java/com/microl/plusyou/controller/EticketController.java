package com.stixcloud.controller.pac;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.stixcloud.controller.common.MasterCodeControllerBase;
import com.stixcloud.domain.transaction.EticketDelivery;
import com.stixcloud.domain.transaction.Transaction;
import com.stixcloud.dto.event.templates.EticketTemplateDTO;
import com.stixcloud.service.business.event.templates.impl.BizEticketRenderService;
import com.stixcloud.service.event.templates.impl.EticketService;

@Controller
@RequestMapping("/pac/acs")
public class EticketController extends MasterCodeControllerBase{
	Logger logger = Logger.getLogger(EticketController.class);

	private byte[] eticketData;

	@Autowired
	EticketService eticketService;

	@Autowired
	BizEticketRenderService bizEticketRenderService;

	//TODO add in post for generating from link
	@RequestMapping(value = { "/downloadEticket.htm" }, method = RequestMethod.GET, produces="application/pdf")
	public String downloadEticket(Model model, HttpServletRequest request, @RequestParam("linkId") String linkId, HttpServletResponse response) {
		logger.debug("download Eticket GET method called");

		response.setContentType("application/pdf");
		EticketDelivery eticketDelivery=eticketService.getEticketDeliveryByLinkId(linkId);
		try {
			if(eticketDelivery!=null) {
				Transaction txn = eticketDelivery.getTransaction();
				Long prdtId = eticketDelivery.getProductId();

				ByteArrayOutputStream baos = new ByteArrayOutputStream();

				String fullPath = bizEticketRenderService.generateEticket(txn.getTransactionid(), prdtId, baos);
				eticketData = baos.toByteArray();
				logger.debug("DOWNLOAD ETICKET full path");
				logger.debug(fullPath);

				fullPath = File.separator + "STIX" + File.separator + "SISTIC" + File.separator + fullPath;
				logger.debug("AFTER FORMAT");
				logger.debug(fullPath);

				InputStream inputStream = null;
				inputStream = new BufferedInputStream(new FileInputStream(new File(fullPath)));

				logger.debug(inputStream!=null ? "inputstream not null" : "null inputstream");
				byte[] inputStreamByte = IOUtils.toByteArray(inputStream);
				baos.write(inputStreamByte);
				response.setContentType("application/pdf");
				response.setContentLength(baos.size());
				ServletOutputStream outputStream = response.getOutputStream();
				baos.writeTo(outputStream);
				baos.flush();

			}
		}
		catch(IOException e){
			logger.error("IOException when doing outputstream");
		}
		return null;
	}

	@RequestMapping(value = { "/viewETicketTemplate.htm" }, method = RequestMethod.POST, produces= "application/pdf")
	public String previewEticket(HttpServletRequest request, HttpServletResponse response, @ModelAttribute EticketTemplateDTO Params) {
		String fullpath =
				bizEticketRenderService.previewEticket(Long.valueOf(Params.getEticketTemplateID()), -1L, Params.isPreview());
		try {
			logger.debug("FILE PATH FOR PREVIEW ETICKET");
			logger.debug(fullpath);

			//TODO for production - include for multi-tenant
			String serverPath = File.separator + "STIX" + File.separator + "SISTIC" + File.separator;
			fullpath = serverPath + fullpath;

			logger.debug("after full path");
			logger.debug(fullpath);

			InputStream inputStream = null;
			File file = new File(fullpath);
			inputStream = new BufferedInputStream(new FileInputStream(file));

			byte[] inputStreamByte = IOUtils.toByteArray(inputStream);

			ByteArrayOutputStream baos = new ByteArrayOutputStream();

			baos.write(inputStreamByte);
			response.setContentType("application/pdf");
			response.setContentLength(baos.size());
			ServletOutputStream outputStream = response.getOutputStream();
			baos.writeTo(outputStream);
			baos.flush();

		}
		catch(Exception e) {
			logger.error("Error while outputting to array");
		}

		return null;
	}
}
