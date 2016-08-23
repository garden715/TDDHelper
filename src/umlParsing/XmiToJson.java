package umlParsing;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XmiToJson {
	JSONObject obj = new JSONObject();

	public static void main(String argv[]) {
		JSONArray classUnits = new JSONArray();
		JSONArray interfaceUnits = new JSONArray();
		JSONObject totalClassOrInterface = new JSONObject();
		try {
			File file = new File("test.xmi");
			DocumentBuilderFactory docBuildFact = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuild = docBuildFact.newDocumentBuilder();
			Document doc = docBuild.parse(file);
			doc.getDocumentElement().normalize();

			NodeList packageList = doc.getElementsByTagName("packagedElement");

			for (int temp = 0; temp < packageList.getLength(); temp++) {
				Node n = (Node) packageList.item(temp);
				Element e = (Element) n;

				switch (e.getAttribute("xmi:type")) {

				case "uml:Class":
					classUnits.add(procedure(e));
					break;
				case "uml:Interface":
					interfaceUnits.add(procedure(e));
					break;
				}
			}
			totalClassOrInterface.put("ClassUnit", classUnits);
			totalClassOrInterface.put("InterfaceUnit", interfaceUnits);

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
		System.out.println(totalClassOrInterface);
		}
	}

	static JSONObject procedure(Element e) {
		JSONObject resultObject = new JSONObject();

		if (e.getAttribute("xmi:type").equals("uml:Class")) {
			resultObject.put("isAbstract", e.getAttribute("isAbstract"));
		}
		resultObject.put("name", e.getAttribute("name"));

		NodeList attriList = e.getElementsByTagName("ownedAttribute");
		JSONArray memberUnits = new JSONArray();
		for (int i = 0; i < attriList.getLength(); i++) {
			JSONObject attributeObject = new JSONObject();
			Node an = (Node) attriList.item(i);
			Element ae = (Element) an;
			if (ae.getAttribute("xmi:type").equals("uml:Property")) {
				attributeObject.put("name", ae.getAttribute("name"));

				/*
				 * * export : ExportKind [ public, private, protected, final,
				 * unknown ]
				 */
				attributeObject.put("export", ae.getAttribute("visibility"));
			} //
			memberUnits.add(attributeObject);
		} // end of attribute list loop
		resultObject.put("MemberUnit", memberUnits);

		NodeList operList = e.getElementsByTagName("ownedOperation");

		JSONArray operationUnits = new JSONArray();
		for (int i = 0; i < operList.getLength(); i++) {
			JSONObject operationObject = new JSONObject();
			Node on = (Node) operList.item(i);
			Element oe = (Element) on;

			if (oe.getAttribute("xmi:type").equals("uml:Operation")) {
				int pos = 1; // parameter Unit [pos]

				/*
				 * * kind : MethodKind [ method, constructor, destructor,
				 * operator, virtual, abstract, unknown ]
				 */
				operationObject.put("name", oe.getAttribute("name"));
				operationObject.put("export", oe.getAttribute("visibility"));

				try {
					if (oe.getAttribute("isAbstract").equals("true")) {
						operationObject.put("kind", "abstract");
					} else if (((Element) (oe.getElementsByTagName("stereotype").item(0))).getAttribute("value")
							.equals("constructor")) {
						operationObject.put("kind", "constructor");
					}
				} catch (NullPointerException excep) {
					operationObject.put("kind", "method");
				}

				NodeList paramList = oe.getElementsByTagName("ownedParameter");
				JSONArray paramArray = new JSONArray();

				/*
				 * * ParameterUnit [ kind : ParameterKind ] - byValue byName
				 * byReference variadic return throws exception catchall unknown
				 * [ pos : integer]
				 */
				for (int j = 0; j < paramList.getLength(); j++) {
					JSONObject paramObject = new JSONObject();

					Node pn = (Node) paramList.item(j);
					Element pe = (Element) pn;
					if (pe.getAttribute("xmi:type").equals("uml:Parameter")) {
						if (pe.getAttribute("direction").equals("in")) {
							paramObject.put("name", pe.getAttribute("name"));
							paramObject.put("kind", "byReference");
							paramObject.put("pos", pos++);

						} else {
							paramObject.put("name", "");
							paramObject.put("kind", "return");
							paramObject.put("pos", 1);
						}
					}
					paramArray.add(paramObject);
				} // end of param list loop
				operationObject.put("ParameterUnit", paramArray);
				operationUnits.add(operationObject);
			} // end of if - equals("uml:Operation")
		} // end of operation list loop
		resultObject.put("MethodUnit", operationUnits);
		return resultObject;
	}
}