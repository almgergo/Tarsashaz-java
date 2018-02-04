package core;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class Tarsashaz {
	String request = "http://217.61.6.129/th_js_online/php/AlbetetKozoskoltseg.php?albetet_id=";
	String YEAR_CONST = "&ev=";
	String REQ_DAY_OF_MONTH = "10";
	int FIRST_YEAR = 2017;

	int testId = 9158;

	public static void calculatePeople(String[] args) {
		System.out.println("Started...");
		Tarsashaz th = new Tarsashaz();

		List<Integer> idList;
		try {
			idList = th.readIdList();

			for (Integer id : idList) {
				Map<Integer, String> responses = getResponses(th, id);

				decodeResponses(th, responses);

				Person person = null;
				for (Entry<Integer, String> response : responses.entrySet()) {
					person = th.parseResponse(response, person);
				}

				person.processPerson();

				System.out.println(person.getName());
				// System.in.read();

			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println("Ended...");
	}

	private static void decodeResponses(Tarsashaz th, Map<Integer, String> responses)
			throws FileNotFoundException, IOException {
		Map<String, String> decodeMap = th.readDecodeMap();
		for (Entry<Integer, String> response : responses.entrySet()) {
			for (Entry<String, String> e : decodeMap.entrySet()) {
				response.setValue(response.getValue().replaceAll(e.getKey(), e.getValue()));
			}
		}
	}

	private static Map<Integer, String> getResponses(Tarsashaz th, Integer testId) throws Exception {
		List<Integer> years = createYearList(th);

		Map<Integer, String> responses = new HashMap<>();
		for (Integer year : years) {
			responses.put(year, th.sendGet(testId, year).toString());
		}

		return responses;
	}

	private static List<Integer> createYearList(Tarsashaz th) {
		int currentYear = th.FIRST_YEAR;

		List<Integer> years = new ArrayList<>();
		years.add(th.FIRST_YEAR);

		Calendar c = Calendar.getInstance();
		int year = c.get(Calendar.YEAR);

		while (year > currentYear) {
			years.add(year);
			currentYear = year;
		}

		return years;
	}

	private Person parseResponse(Entry<Integer, String> response, Person p)
			throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		InputSource is = new InputSource();
		is.setCharacterStream(new StringReader(response.getValue()));

		Document doc = db.parse(is);
		NodeList personInfoNodes = doc.getChildNodes().item(0).getChildNodes().item(1).getChildNodes().item(1)
				.getChildNodes();

		if (p == null) {
			p = new Person();
			p.setIdentifier(personInfoNodes.item(5).getTextContent());
			p.setName(personInfoNodes.item(6).getTextContent());
			// System.out.println(p.getName());
			p.setStartBalance(Double.parseDouble(personInfoNodes.item(8).getTextContent().split(" ")[0]));

			p.setExistingBacklog(Double.parseDouble(doc.getChildNodes().item(0).getChildNodes().item(1).getChildNodes()
					.item(2).getChildNodes().item(8).getTextContent().split(" ")[0]));
			p.setPaymentTotal(Double.parseDouble(doc.getChildNodes().item(0).getChildNodes().item(1).getChildNodes()
					.item(3).getChildNodes().item(8).getTextContent().split(" ")[0]));
			p.setBalance(Double.parseDouble(doc.getChildNodes().item(0).getChildNodes().item(1).getChildNodes().item(4)
					.getChildNodes().item(8).getTextContent().split(" ")[0]));
		}

		for (int i = 7; i < doc.getChildNodes().item(0).getChildNodes().item(1).getChildNodes().getLength(); i++) {
			processRow(p, response.getKey(),
					doc.getChildNodes().item(0).getChildNodes().item(1).getChildNodes().item(i).getChildNodes());
		}

		return p;
	}

	private void processRow(Person p, int year, NodeList nodeList) {
		if ("1".equals(nodeList.item(0).getTextContent())) {
			recordBacklog(p, year, nodeList);
		} else if ("0".equals(nodeList.item(0).getTextContent())) {
			recordPayment(p, year, nodeList);
		}

	}

	private void recordPayment(Person p, int year, NodeList nodeList) {
		Calendar c = Calendar.getInstance();
		nullTime(c);
		c.set(Calendar.YEAR, Integer.parseInt(nodeList.item(5).getTextContent().split("\\.")[0]));
		c.set(Calendar.MONTH, Integer.parseInt(nodeList.item(5).getTextContent().split("\\.")[1]) - 1);
		c.set(Calendar.DAY_OF_MONTH, Integer.parseInt(nodeList.item(5).getTextContent().split("\\.")[2]));

		p.getPayments().add(new Payment(c, nodeList.item(6).getTextContent(), nodeList.item(7).getTextContent(),
				Double.parseDouble(nodeList.item(8).getTextContent().split(" ")[0])));

	}

	private void recordBacklog(Person p, int year, NodeList nodeList) {
		Calendar c = Calendar.getInstance();
		nullTime(c);
		c.set(Calendar.YEAR, year);
		c.set(Calendar.MONTH, Integer.parseInt(nodeList.item(1).getTextContent().split("\\.")[0]) - 1);
		c.set(Calendar.DAY_OF_MONTH, 10);

		p.getBacklogs()
				.add(new Backlog(c, Double.parseDouble(nodeList.item(2).getTextContent().split(" ")[0]),
						Double.parseDouble(nodeList.item(3).getTextContent().split(" ")[0]),
						Double.parseDouble(nodeList.item(4).getTextContent().split(" ")[0])));
	}

	private Map<String, String> readDecodeMap() throws FileNotFoundException, IOException {
		Map<String, String> decodeMap = new HashMap<>();

		try (BufferedReader br = new BufferedReader(
				new InputStreamReader(new FileInputStream("charMap.txt"), "utf-8"))) {
			String line;
			while ((line = br.readLine()) != null) {
				try {
					String[] parts = line.split("\t");
					decodeMap.put(parts[1], parts[0]);
				} catch (final Exception e) {

					// System.out.println("couldnt read line: " + line);
				}
			}
		}

		return decodeMap;
	}

	private List<Integer> readIdList() throws FileNotFoundException, IOException {
		List<Integer> idList = new ArrayList<>();

		try (BufferedReader br = new BufferedReader(
				new InputStreamReader(new FileInputStream("idList.txt"), "utf-8"))) {
			String line;
			while ((line = br.readLine()) != null) {
				try {
					// String[] parts = line.split("\t");
					idList.add(Integer.parseInt(line));
				} catch (final Exception e) {

					// System.out.println("couldnt read line: " + line);
				}
			}
		}

		return idList;
	}

	private StringBuffer sendGet(Integer testId, Integer year) throws Exception {

		String url = "http://217.61.6.129/th_js_online/php/AlbetetKozoskoltseg.php?albetet_id=" + testId + YEAR_CONST
				+ year;

		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();

		con.setRequestMethod("GET");

		int responseCode = con.getResponseCode();
		// System.out.println("\nSending 'GET' request to URL : " + url);
		// System.out.println("Response Code : " + responseCode);

		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();

		// print result
		// System.out.println(response.toString());
		return response;
	}

	private void nullTime(Calendar c) {
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);

	}
}
