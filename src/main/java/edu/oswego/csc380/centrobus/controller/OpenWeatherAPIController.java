package edu.oswego.csc380.centrobus.controller;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.util.Date;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import edu.oswego.csc380.centrobus.data.Vehicle;
import edu.oswego.csc380.centrobus.data.Weather;

/**
 * Communicates with the OpenWeatherAPI and returns the weather for a given stop
 * @author alex
 */
@Component
public class OpenWeatherAPIController extends Controller {

	private Logger logger = LoggerFactory.getLogger(OpenWeatherAPIController.class);
    @Value("${openweather.key}")
	private String key;
	
	public OpenWeatherAPIController() {

	}
	
	/**
	 * Takes a vehicle, and gets the current weather from it via the API. If an error has occurred while attempting
	 * to get the weather, the condition will be -1, and the icon will be "error".
	 * @param v The vehicle to get data from
	 * @return The current weather at the vehicle
	 */
	public Weather getWeather(Vehicle v) {
		Weather w = new Weather(v, -1, "error", Date.from(Instant.now()), -1.0F, 0.0F);
		
		String urlStr = "https://api.openweathermap.org/data/2.5/weather?mode=xml&lat=" + v.lat + 
				"&lon=" + v.lon + "&appid=" + key;
		String loggedUrlStr = urlStr.split(Pattern.quote("&appid="))[0] + "[censored API key]";
		try {
			URL url = new URL(urlStr);
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			
			super.addUse();
			Document doc = db.parse(url.openStream());
			
			// I do wish that the following could be abstracted into its own method, as it reüses a *lot*
			// of code. It's all just slightly different in a way that makes it so that it can't be its own
			// method though :/
			
			NodeList weatherNodes = doc.getElementsByTagName("weather");
			// If there's more than one weather node, the documents say that the first is the primary one
			// Seeing how we're only storing one condition, just take the first one.
			if (weatherNodes.getLength() > 0) {
				Node n = weatherNodes.item(0);
				NamedNodeMap attr = n.getAttributes();
				Node condition = attr.getNamedItem("number");
				Node icon = attr.getNamedItem("icon");				
				
				if (condition != null) {
					w.conditionID = Integer.parseInt(condition.getTextContent());
				}
				if (icon != null) {
					w.icon =  icon.getTextContent();
				}
			} else {
				logger.error("Unable to parse weather condition data!");
			}
			
			NodeList temperatureNodes = doc.getElementsByTagName("temperature");
			if (temperatureNodes.getLength() > 0) {
				Node n = temperatureNodes.item(0); // there can only be one temperature, after all
				NamedNodeMap attr = n.getAttributes();
				Node temp = attr.getNamedItem("value");
				
				if (temp != null) {
					w.temp = Float.parseFloat(temp.getTextContent());
				}
			}
			
			NodeList precipitationNodes = doc.getElementsByTagName("precipitation");
			if (precipitationNodes.getLength() > 0) {
				Node n = precipitationNodes.item(0);
				NamedNodeMap attr = n.getAttributes();
				Node precip = attr.getNamedItem("value");
				
				if (precip != null) {
					w.precip = Float.parseFloat(precip.getTextContent());
				}
			}
		} catch (MalformedURLException e) {
			logger.error("MalformedURLException: Unable to parse URL for weather data! Given URL:" +
					loggedUrlStr
			);
		} catch (SAXException e) {
			logger.error("SAXException thrown when attempting to get weather data. Did the URL return a valid XML document? "
					+ "Tried " + loggedUrlStr);
		} catch (IOException e) {
			logger.error("IOException thrown when attempting to get weather data from " + loggedUrlStr);
		} catch (ParserConfigurationException e) {
			logger.error("ParserConfigurationException thrown when attempting to get weather data from " + loggedUrlStr
					+ " If you are seeing this message in your log, this is a bug!");
		} catch (NumberFormatException e) {
			logger.error("Unable to parse weather condition integer!");
		}
		
		return w;		
	}
	
}
