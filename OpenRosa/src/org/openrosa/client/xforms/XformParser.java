package org.openrosa.client.xforms;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.openrosa.client.model.Calculation;
import org.openrosa.client.model.FormDef;
import org.openrosa.client.model.GroupDef;
import org.openrosa.client.model.IFormElement;
import org.openrosa.client.model.OptionDef;
import org.openrosa.client.model.QuestionDef;
import org.openrosa.client.model.RepeatQtnsDef;
import org.openrosa.client.util.ItextParser;
import org.openrosa.client.util.FormUtil;
import org.openrosa.client.xforms.XformConstants;
import org.openrosa.client.xforms.XformUtil;
import org.openrosa.client.xforms.XmlUtil;
import org.openrosa.client.xpath.XPathExpression;

import com.google.gwt.core.client.GWT;
import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.Element;
import com.google.gwt.xml.client.Node;
import com.google.gwt.xml.client.NodeList;


/**
 * Parse xforms documents and builds the form definition object model.
 * 
 * @author daniel
 *
 */
public class XformParser {

	/** The current question id. */
	private static int currentQuestionId = 1;

	/** The current page number. */
	private static int currentPageNo = 1;


	/**
	 * All methods in this class are static and hence we expect no external
	 * Instantiation of this class.
	 */
	private XformParser(){

	}


	/**
	 * Gets a new question id.
	 * 
	 * @return the new question id
	 */
	private static int getNextQuestionId(){
		return currentQuestionId++;
	}


	/**
	 * Gets a new page number.
	 * 
	 * @return the new page number.
	 */
	private static int getNextPageNo(){
		return currentPageNo++;
	}


	/**
	 * Creates a copy of a formDef together with its xform xml.
	 * 
	 * @param formDef the form to copy.
	 * @return the new copy of the form.
	 */
	public static FormDef copyFormDef(FormDef formDef){
		if(formDef.getDoc() == null)
			return new FormDef(formDef);
		else //Value of false creates bugs where repeat widgets are not loaded properly on data preview
			formDef.updateDoc(true); //formDef.updateDoc(false);

		return fromXform2FormDef(XformUtil.normalizeNameSpace(formDef.getDoc(),XmlUtil.fromDoc2String(formDef.getDoc())));

		/*if(formDef.getDoc() == null)
			return new FormDef(formDef);
		else
			formDef.updateDoc(false);

		return fromXform2FormDef(XformUtil.normalizeNameSpace(formDef.getDoc(),XmlUtil.fromDoc2String(formDef.getDoc())));*/
	}

	/**
	 * Converts an xml document to a form definition object.
	 * 
	 * @param xml the document xml.
	 * @return the form definition object.
	 */
	public static FormDef fromXform2FormDef(String xml, HashMap<Integer,HashMap<String,String>> languageText){
		Document doc = XmlUtil.getDocument(xml);

		String layoutXml = null, javaScriptSrc = null; NodeList nodes = null;
		Element root = doc.getDocumentElement();
		if(root.getNodeName().equals("PurcForm")){
			nodes = root.getElementsByTagName("Xform");
			assert(nodes.getLength() > 0);
			xml = XmlUtil.getChildElement(nodes.item(0)).toString();
			doc = XmlUtil.getDocument(xml);

			nodes = root.getElementsByTagName("Layout");
			if(nodes.getLength() > 0)
				layoutXml = FormUtil.formatXml(XmlUtil.getChildElement(nodes.item(0)).toString());

			nodes = root.getElementsByTagName("JavaScript");
			if(nodes.getLength() > 0)
				javaScriptSrc = XmlUtil.getChildCDATA(nodes.item(0)).getNodeValue();

			nodes = root.getElementsByTagName("LanguageText"); 
			assert(nodes.getLength() > 0);
		}

		FormDef formDef = getFormDef(doc);

		if(layoutXml != null)
			formDef.setLayoutXml(FormUtil.formatXml(layoutXml));

		if(javaScriptSrc != null)
			formDef.setJavaScriptSource(javaScriptSrc);

		if(nodes != null){
			loadLanguageText(formDef.getId(),nodes,languageText);
			formDef.setXformXml(FormUtil.formatXml(xml));
		}

		return formDef;
	}


	public static void loadLanguageText(Integer formId, NodeList nodes, HashMap<Integer,HashMap<String,String>> languageText){
		for(int index = 0; index < nodes.getLength(); index++){
			Element node = (Element)nodes.item(index);

			HashMap<String,String> map = languageText.get(formId);
			if(map == null){
				map = new HashMap<String,String>();
				languageText.put(formId, map);
			}

			map.put(node.getAttribute("lang"), FormUtil.formatXml(node.toString()));
		}
	}


	/**
	 * Converts an xml document to a form definition object.
	 * 
	 * @param xml the document xml.
	 * @return the form definition object.
	 */
	public static FormDef fromXform2FormDef(String xml){
		Document doc = XmlUtil.getDocument(xml);
		FormDef formDef = getFormDef(doc);
		formDef.setId(1);
		return formDef;
	}

	/**
	 * Converts an xforms document into a form definition object and also
	 * replaces its model with the given one.
	 * 
	 * @param xformXml the xforms document xml.
	 * @param modelXml the new xforms model xml.
	 * @return the form definition object.
	 */
	public static FormDef fromXform2FormDef(String xformXml, String modelXml){
		Document doc = XmlUtil.getDocument(xformXml);

		//If model xml has been supplied, use it to replace the existing one.
		if(modelXml != null){
			Element node = XmlUtil.getDocument(modelXml).getDocumentElement();//XformConverter.getNode(XformConverter.getDocument(modelXml).getDocumentElement().toString());
			Element dataNode = XformUtil.getInstanceDataNode(doc);
			Node parent = dataNode.getParentNode();
			parent.appendChild(node);
			parent.replaceChild(node,dataNode);
		}

		return getFormDef(doc);
	}


	/**
	 * Converts an xml document object to a form definition object.
	 * 
	 * @param doc the xml document object.
	 * @return the form definition object.
	 */
	public static FormDef getFormDef(Document doc){
		Element rootNode = doc.getDocumentElement();
		FormDef formDef = new FormDef();
		formDef.setDoc(doc);
		formDef.setId(1);
		HashMap id2VarNameMap = new HashMap();
		HashMap relevants = new HashMap();
		HashMap constraints = new HashMap();
		Vector repeats = new Vector();
		HashMap rptKidMap = new HashMap();
		List<QuestionDef> orphanDynOptionQns = new ArrayList<QuestionDef>();

		currentQuestionId = 1;
		currentPageNo = 1;

		parseElement(formDef,rootNode,id2VarNameMap,null,relevants,repeats,rptKidMap,(int)0,null,constraints,orphanDynOptionQns);

		if(formDef.getName() == null || formDef.getName().length() == 0)
			formDef.setName(formDef.getVariableName());

		DefaultValueUtil.setDefaultValues(XformUtil.getInstanceDataNode(doc),formDef,id2VarNameMap); //TODO Very slow needs optimisation for very big forms
		RelevantParser.addSkipRules(formDef,relevants);
		ConstraintParser.addValidationRules(formDef,constraints);

		ItemsetParser.parseOrphanDynOptionQns(formDef,orphanDynOptionQns);

		//Remove all that we had created as questions when parsing bindings but will not require
		//user input (eg JR's DeviceId, EndTime), since questions are only for cases where we want user input.
		//TODO Needs to be fixed when having multiple groups
//		removeElementsWithoutControlNode(formDef.getChildren());

		return formDef;
	}

	private static void removeElementsWithoutControlNode(List<IFormElement> elements){
		if(elements == null)
			return;
		
		for(int index = 0; index < elements.size(); index++){
			IFormElement element = elements.get(index);
			if(element.getControlNode() == null && !(element instanceof GroupDef)){
				//element.getParent().removeChild(element); //We do not want to lose the bindings if any.
				GWT.log("removing node:"+element.getBinding());
				element.getParent().getChildren().remove(element);
				index--;
			}
			else if(element instanceof GroupDef){
//				removeElementsWithoutText(element.getChildren());
			}
		}
	}


	/**
	 * Parses an xforms document and builds a form definition object.
	 * 
	 * @param formDef the form definition object that we are building.
	 * @param element the element that we are currently parsing.
	 * @param id2VarNameMap a map of question bind ids to the variable names.
	 * @param questionDef the question definition object that is currently being parsed.
	 * @param relevants the map of constraint attribute values keyed by their 
	 * 					  question definition objects.
	 * @param repeatQtns a list of repeat question types.
	 * @param rptKidMap a map of question definition objects which are children of a repeat 
	 * 					question type, keyed by their variable names.
	 * @param currentPageNo the number of the current page we are parsing.
	 * @param parentQtn the parent of the question we are currently processing.
	 * @param constraints the map of constraint attribute values keyed by their 
	 * 					  question definition objects.
	 * @param orphanDynOptionQns a list of dynamic option definition questions who parent
	 *                           questions have not yet been parsed.
	 * @return the question we are currently parsing.
	 */
	private static IFormElement parseElement(FormDef formDef, Element element, HashMap id2VarNameMap,IFormElement questionDef,HashMap relevants,Vector repeatQtns, HashMap rptKidMap, int currentPageNo, IFormElement parentQtn, HashMap constraints, List<QuestionDef> orphanDynOptionQns){
		String label = "";
		String hint = "";
		String value = "";
		Element labelNode = null;
		Element hintNode = null;
		Element valueNode = null;

		//TODO wiered bug here for some forms, nodes.getLength() returns a value less
		//than numOfEntries during the loop. So something could be changing the node list
		NodeList nodes = element.getChildNodes();
		int numOfEntries = nodes.getLength();
		for (int i = 0; i < numOfEntries; i++) {
			if(nodes.item(i) == null || nodes.item(i).getNodeType() != Node.ELEMENT_NODE)
				continue;

			Element child = (Element)nodes.item(i);
			String tagname = child.getNodeName(); //getNodeName(child);

			//if(tagname.equals(NODE_NAME_SUBMIT) || tagname.equals(NODE_NAME_SUBMIT_MINUS_PREFIX))
			if(XmlUtil.nodeNameEquals(tagname,XformConstants.NODE_NAME_SUBMIT_MINUS_PREFIX))
				continue;
			else if (XmlUtil.nodeNameEquals(tagname,"head"))
				parseElement(formDef,child,id2VarNameMap,questionDef,relevants,repeatQtns,rptKidMap,currentPageNo,parentQtn,constraints,orphanDynOptionQns);
			else if (XmlUtil.nodeNameEquals(tagname,"body")){
				formDef.setBodyNode(child);
				parseElement(formDef, child,id2VarNameMap,questionDef,relevants,repeatQtns,rptKidMap,currentPageNo,parentQtn,constraints,orphanDynOptionQns);
			}
			else if (XmlUtil.nodeNameEquals(tagname,"title")){
				formDef.setName(getText(child));
				formDef.setItextId(XmlUtil.getItextId(child));
			}
			//else if (tagname.equals(NODE_NAME_MODEL) || tagname.equals(NODE_NAME_MODEL_MINUS_PREFIX)){
			else if(XmlUtil.nodeNameEquals(tagname,XformConstants.NODE_NAME_MODEL_MINUS_PREFIX)){
				formDef.setModelNode((Element)child);
				formDef.setXformsNode(child.getOwnerDocument().getDocumentElement() /*child.getParentNode()*/);
				parseElement(formDef, child,id2VarNameMap,questionDef,relevants,repeatQtns,rptKidMap,currentPageNo,parentQtn,constraints,orphanDynOptionQns);
			}
			//else if (tagname.equals(NODE_NAME_GROUP) || tagname.equals(NODE_NAME_GROUP_MINUS_PREFIX)){
			else if(XmlUtil.nodeNameEquals(tagname,XformConstants.NODE_NAME_GROUP_MINUS_PREFIX)){
				/*questionDef = */parseGroupElement(formDef, child,id2VarNameMap,questionDef,relevants,repeatQtns,rptKidMap,currentPageNo,parentQtn,constraints,orphanDynOptionQns);
			}
			//else if(tagname.equals(NODE_NAME_INSTANCE)||tagname.equals(NODE_NAME_INSTANCE_MINUS_PREFIX)) {
			else if(XmlUtil.nodeNameEquals(tagname,XformConstants.NODE_NAME_INSTANCE_MINUS_PREFIX)){
				parseInstanceElement(formDef, child);
			} 
			//else if (tagname.equals(NODE_NAME_BIND)||tagname.equals(NODE_NAME_BIND_MINUS_PREFIX) /*|| tagname.equals(ATTRIBUTE_NAME_REF)*/) {
			else if(XmlUtil.nodeNameEquals(tagname,XformConstants.NODE_NAME_BIND_MINUS_PREFIX)){
				QuestionDef qtn = parseBindElement(formDef, child,id2VarNameMap,questionDef,relevants,repeatQtns,rptKidMap,currentPageNo,parentQtn,constraints,orphanDynOptionQns);
				qtn.setHasUINode(false);
				if(qtn.getDataType() == QuestionDef.QTN_TYPE_REPEAT)
					questionDef = qtn;
			} 
			//else if (tagname.equals(NODE_NAME_INPUT) || tagname.equals(NODE_NAME_SELECT1) || tagname.equals(NODE_NAME_SELECT) || tagname.equals(NODE_NAME_REPEAT)
			//		|| tagname.equals(NODE_NAME_INPUT_MINUS_PREFIX) || tagname.equals(NODE_NAME_SELECT1_MINUS_PREFIX) || tagname.equals(NODE_NAME_SELECT_MINUS_PREFIX) || tagname.equals(NODE_NAME_REPEAT_MINUS_PREFIX)) {
			else if(XmlUtil.nodeNameEquals(tagname,XformConstants.NODE_NAME_INPUT_MINUS_PREFIX) || XmlUtil.nodeNameEquals(tagname,XformConstants.NODE_NAME_SELECT1_MINUS_PREFIX) || 
					XmlUtil.nodeNameEquals(tagname,XformConstants.NODE_NAME_SELECT_MINUS_PREFIX) || XmlUtil.nodeNameEquals(tagname,XformConstants.NODE_NAME_REPEAT_MINUS_PREFIX) ||
					XmlUtil.nodeNameEquals(tagname,XformConstants.NODE_NAME_UPLOAD_MINUS_PREFIX) ||
					XmlUtil.nodeNameEquals(tagname,XformConstants.NODE_NAME_TRIGGER_MINUS_PREFIX)){

				NodeContext nodeContext = new NodeContext(label, hint, value, labelNode, hintNode, valueNode);
				questionDef = parseUiElement(formDef, child,id2VarNameMap,questionDef,relevants,repeatQtns,rptKidMap,currentPageNo,parentQtn,constraints,orphanDynOptionQns,nodeContext);
				questionDef.setHasUINode(true);
				label = nodeContext.getLabel();
				hint = nodeContext.getHint();
				value = nodeContext.getValue();
				labelNode = nodeContext.getLabelNode();
				hintNode = nodeContext.getHintNode();
				valueNode = nodeContext.getValueNode();
			} 
			//else if(tagname.equals(NODE_NAME_ITEMSET)||tagname.equals(NODE_NAME_ITEMSET_MINUS_PREFIX)){
			else if(XmlUtil.nodeNameEquals(tagname,XformConstants.NODE_NAME_ITEMSET_MINUS_PREFIX)){
				questionDef.setDataType(QuestionDef.QTN_TYPE_LIST_EXCLUSIVE_DYNAMIC);
				((QuestionDef)questionDef).setFirstOptionNode(child);
				ItemsetParser.parseDynamicOptionsList((QuestionDef)questionDef,child.getAttribute(XformConstants.ATTRIBUTE_NAME_NODESET),formDef,orphanDynOptionQns);
			}
			//else if(tagname.equals(NODE_NAME_LABEL)||tagname.equals(NODE_NAME_LABEL_MINUS_PREFIX)){
			else if(XmlUtil.nodeNameEquals(tagname,XformConstants.NODE_NAME_LABEL_MINUS_PREFIX)){

				NodeContext nodeContext = new NodeContext(label, hint, value, labelNode, hintNode, valueNode);;
				parseLabelElement(formDef, child, questionDef, nodeContext);

				if(questionDef instanceof GroupDef)
					setLabelValueNode(formDef, element, questionDef, parentQtn, nodeContext);

				label = nodeContext.getLabel();
				hint = nodeContext.getHint();
				value = nodeContext.getValue();
				labelNode = nodeContext.getLabelNode();
				hintNode = nodeContext.getHintNode();
				valueNode = nodeContext.getValueNode();
			}
			//else if (tagname.equals(NODE_NAME_HINT)||tagname.equals(NODE_NAME_HINT_MINUS_PREFIX)){
			else if(XmlUtil.nodeNameEquals(tagname,XformConstants.NODE_NAME_HINT_MINUS_PREFIX)){

				NodeContext nodeContext = new NodeContext(label, hint, value, labelNode, hintNode, valueNode);;
				parseHintElement(formDef, child, questionDef, nodeContext);

				label = nodeContext.getLabel();
				hint = nodeContext.getHint();
				value = nodeContext.getValue();
				labelNode = nodeContext.getLabelNode();
				hintNode = nodeContext.getHintNode();
				valueNode = nodeContext.getValueNode();
			}
			//else if (tagname.equals(NODE_NAME_ITEM)||tagname.equals(NODE_NAME_ITEM_MINUS_PREFIX))
			else if(XmlUtil.nodeNameEquals(tagname,XformConstants.NODE_NAME_ITEM_MINUS_PREFIX))
				parseElement(formDef, child,id2VarNameMap,questionDef,relevants,repeatQtns,rptKidMap,currentPageNo,parentQtn,constraints,orphanDynOptionQns);
			//else if (tagname.equals(NODE_NAME_VALUE)||tagname.equals(NODE_NAME_VALUE_MINUS_PREFIX)){
			else if(XmlUtil.nodeNameEquals(tagname,XformConstants.NODE_NAME_VALUE_MINUS_PREFIX)){
				if(true /*child.getChildNodes().getLength() != 0*/){
					value = getText(child);
					valueNode = child;
				}
			}
			else
				parseElement(formDef, child,id2VarNameMap,questionDef,relevants,repeatQtns,rptKidMap,currentPageNo,parentQtn,constraints,orphanDynOptionQns);
			// TODO - how are other elements like html:p or br handled?
		}

		NodeContext nodeContext = new NodeContext(label, hint, value, labelNode, hintNode, valueNode);;
		setLabelValueNode(formDef, element, questionDef, parentQtn, nodeContext);

		label = nodeContext.getLabel();
		hint = nodeContext.getHint();
		value = nodeContext.getValue();
		labelNode = nodeContext.getLabelNode();
		hintNode = nodeContext.getHintNode();
		valueNode = nodeContext.getValueNode();

		return questionDef;
	}


	/**
	 * Sets the label and value nodes of the current object being parsed.
	 * 
	 * @param formDef the form definition object.
	 * @param element the element we are currently parsing.
	 * @param questionDef the question we are currently parsing.
	 * @param parentQtn the parent of the question we are currently processing.
	 * @param nodeContext the node context.
	 */
	private static void setLabelValueNode(FormDef formDef, Element element, IFormElement questionDef, IFormElement parentQtn, NodeContext nodeContext){
		boolean hasLabel = !nodeContext.getLabel().isEmpty() ||
									(	(nodeContext.getLabelNode() != null) &&
										(nodeContext.getLabelNode().getAttribute("ref") != null) &&
										(!nodeContext.getLabelNode().getAttribute("ref").isEmpty())	);
		boolean hasValue = !nodeContext.getValue().isEmpty();
		if (hasLabel && hasValue) {
			if (questionDef instanceof QuestionDef && questionDef != null && questionDef.getChildren() != null){
				if(((QuestionDef)questionDef).getOptions().size() == 0)
					((QuestionDef)questionDef).setFirstOptionNode(element);
				OptionDef optionDef = new OptionDef(Integer.parseInt(String.valueOf(questionDef.getChildren().size())),nodeContext.getLabel(), nodeContext.getValue(),(QuestionDef)questionDef);
				optionDef.setControlNode(element);
				optionDef.setLabelNode(nodeContext.getLabelNode());
				optionDef.setValueNode(nodeContext.getValueNode());
				((QuestionDef)questionDef).addOption(optionDef);

				//Ids are mandatory for uniquely identifying items for localization xpath expressions.
				String id = element.getAttribute(XformConstants.ATTRIBUTE_NAME_ID);
				if(id == null || id.trim().length() == 0)
					element.setAttribute(XformConstants.ATTRIBUTE_NAME_ID, optionDef.getBinding());
			}
		} 
		else if (hasLabel && questionDef != null){
			if(questionDef.getText() == null || questionDef.getText().trim().length()==0){

				if(questionDef != parentQtn && parentQtn instanceof GroupDef && questionDef.getParent() != parentQtn){
					questionDef.getParent().getChildren().remove(questionDef);
					parentQtn.addChild(questionDef);
				}

				questionDef.setText(nodeContext.getLabel());				
				questionDef.setControlNode(element);
				questionDef.setLabelNode(nodeContext.getLabelNode());

				if(questionDef instanceof QuestionDef)
					setQuestionDataNode((QuestionDef)questionDef,formDef,parentQtn);
			}
		}
	}


	/**
	 * Sets the xforms instance data node child of a given question definition object.
	 * 
	 * @param qtn the question definition object.
	 * @param formDef the form to which the question belongs.
	 * @param parentQtn the parent question to which qtn belongs as a child.
	 * 					This is only non null for kids of repeat question types.
	 */
	private static void setQuestionDataNode(IFormElement qtn, FormDef formDef, IFormElement parentQtn){
		String xpath = qtn.getBinding();

		//xpath = new String(xpath.toCharArray(), 1, xpath.length()-1);
		int pos = xpath.lastIndexOf('@'); String attributeName = null;
		if(pos > 0){
			attributeName = xpath.substring(pos+1,xpath.length());
			xpath = xpath.substring(0,pos-1);
		}

		Element node = formDef.getDataNode();
		if(qtn.getControlNode().getParentNode().getNodeName().equals(XformConstants.NODE_NAME_REPEAT)){
			if(parentQtn != null)
				node = parentQtn.getDataNode();
		}

		if(node == null)
			return; //data node may not be present in the xforms document.
		
		if(xpath.startsWith("/" + node.getNodeName() + "/"))
			xpath = xpath.substring(node.getNodeName().length() + 2);

		XPathExpression xpls = new XPathExpression(node, xpath);
		Vector result = xpls.getResult();

		for (Enumeration e = result.elements(); e.hasMoreElements();) {
			Object obj = e.nextElement();
			if (obj instanceof Element){
				if(pos > 0) //Check if we are to set attribute value.
					qtn.setDataNode(((Element) obj)); //((Element) obj).setAttribute(attributeName, value);
				else
					qtn.setDataNode(((Element) obj));//((Element) obj).addChild(Node.TEXT_NODE, value);

				break;
			}
		}
	}


	/**
	 * Checks if this is a repeat child question and adds it.
	 * @param qtn the questions to check
	 * @param repeats the list of repeat questions
	 * @return true if so, else false.
	 */
	private static boolean addRepeatChildQtn(QuestionDef qtn, Vector repeats,Element child,HashMap map,HashMap rptKidmap){
		for(int i=0; i<repeats.size(); i++){
			QuestionDef rptQtn = (QuestionDef)repeats.get(i);
			if(qtn.getBinding().contains(rptQtn.getBinding())){
				RepeatQtnsDef rptQtnsDef = rptQtn.getRepeatQtnsDef();
				//rptQtnsDef.addQuestion(qtn); //TODO This is temporarily removed to solve the wiered problem list bug
				String varname = qtn.getBinding().substring(rptQtn.getBinding().length()+1);
				//varname = varname.substring(0, varname.indexOf('/'));
				//map.put(child.getAttribute(ATTRIBUTE_NAME_ID), varname);
				map.put(varname, varname);
				rptKidmap.put(varname, qtn);
				return true;		
			}
		}
		return false;
	}


	/**
	 * Adds a new question that uses a ref attribute instead of bind.
	 * 
	 * @param formDef the form definition object to which the question belongs.
	 * @param child the node being currently processed.
	 * @param relevants the map of constraint attribute values keyed by their 
	 * 					  question definition objects.
	 * @param ref the ref attribute value.
	 * @param bind the bind attribute value.
	 * @param constraints the map of constraint attribute values keyed by their 
	 * 					  question definition objects.
	 * @return the variable name of the new question.
	 */
	private static String addNonBindControl(FormDef formDef,Element child,HashMap relevants, String ref, String bind,HashMap constraints, IFormElement parentQtn){
		QuestionDef qtn = new QuestionDef(null);
		qtn.setId(getNextQuestionId());

		if(child.getAttribute(XformConstants.ATTRIBUTE_NAME_TYPE) == null){
			if(XmlUtil.nodeNameEquals(child.getNodeName(),XformConstants.NODE_NAME_TRIGGER_MINUS_PREFIX))
				qtn.setDataType(QuestionDef.QTN_TYPE_LABEL);
			else
				qtn.setDataType(QuestionDef.QTN_TYPE_TEXT);
		}
		else
			XformParserUtil.setQuestionType(qtn,child.getAttribute(XformConstants.ATTRIBUTE_NAME_TYPE),child);

		if(child.getAttribute(XformConstants.ATTRIBUTE_NAME_REQUIRED) != null && child.getAttribute(XformConstants.ATTRIBUTE_NAME_REQUIRED).equals(XformConstants.XPATH_VALUE_TRUE)){
			if(child.getAttribute(XformConstants.ATTRIBUTE_NAME_ACTION) == null)
				qtn.setRequired(true);
		}
		if(child.getAttribute(XformConstants.ATTRIBUTE_NAME_READONLY) != null && child.getAttribute(XformConstants.ATTRIBUTE_NAME_READONLY).equals(XformConstants.XPATH_VALUE_TRUE))
			qtn.setEnabled(false);
		if(child.getAttribute(XformConstants.ATTRIBUTE_NAME_LOCKED) != null && child.getAttribute(XformConstants.ATTRIBUTE_NAME_LOCKED).equals(XformConstants.XPATH_VALUE_TRUE))
			qtn.setLocked(true);
		if(child.getAttribute(XformConstants.ATTRIBUTE_NAME_VISIBLE) != null && child.getAttribute(XformConstants.ATTRIBUTE_NAME_VISIBLE).equals(XformConstants.XPATH_VALUE_FALSE))
			qtn.setVisible(false);

		qtn.setVariableName(((ref != null) ? ref : bind));

		if(parentQtn instanceof GroupDef)
			parentQtn.addChild(qtn);
		else
			formDef.addElement(qtn);

		if(child.getAttribute(XformConstants.ATTRIBUTE_NAME_RELEVANT) != null)
			relevants.put(qtn,child.getAttribute(XformConstants.ATTRIBUTE_NAME_RELEVANT));

		if(child.getAttribute(XformConstants.ATTRIBUTE_NAME_CONSTRAINT) != null)
			constraints.put(qtn,child.getAttribute(XformConstants.ATTRIBUTE_NAME_CONSTRAINT));

		if(child.getAttribute(XformConstants.ATTRIBUTE_NAME_CALCULATE) != null)
			formDef.addCalculation(new Calculation(qtn.getId(),child.getAttribute(XformConstants.ATTRIBUTE_NAME_CALCULATE)));

		return qtn.getBinding();
	}


	/**
	 * Parses the instance node of an xforms document.
	 * 
	 * @param formDef the form definition object that we are building.
	 * @param child the element that we are currently parsing.
	 */
	private static void parseInstanceElement(FormDef formDef, Element child){
		if(formDef.getDataNode() != null)
			return; //we only take the first instance node for formdef ref

		Element dataNode = null;
		for(int k=0; k<child.getChildNodes().getLength(); k++){
			if(child.getChildNodes().item(k).getNodeType() == Node.ELEMENT_NODE){
				dataNode = (Element)child.getChildNodes().item(k);
				formDef.setDataNode(dataNode);
			}
		}

		formDef.setVariableName(XmlUtil.getNodeName(dataNode));
		if(dataNode.getAttribute(XformConstants.ATTRIBUTE_NAME_DESCRIPTION_TEMPLATE) != null)
			formDef.setDescriptionTemplate(dataNode.getAttribute(XformConstants.ATTRIBUTE_NAME_DESCRIPTION_TEMPLATE));

		if(dataNode.getAttribute(XformConstants.ATTRIBUTE_NAME_ID) != null){
			try{
				formDef.setId(Integer.parseInt(dataNode.getAttribute(XformConstants.ATTRIBUTE_NAME_ID)));
			}
			catch(Exception ex){/*We may have non numeric ids like for odk. We just ignore them.*/}
		}

		if(dataNode.getAttribute(XformConstants.ATTRIBUTE_NAME_NAME) != null)
			formDef.setName(dataNode.getAttribute(XformConstants.ATTRIBUTE_NAME_NAME));

		if(dataNode.getAttribute(XformConstants.ATTRIBUTE_NAME_FORM_KEY) != null)
			formDef.setFormKey(dataNode.getAttribute(XformConstants.ATTRIBUTE_NAME_FORM_KEY));
	}


	/**
	 * Parses a group element of an xforms document.
	 * 
	 * @param formDef the form definition object that we are building.
	 * @param element the element that we are currently parsing.
	 * @param id2VarNameMap a map of question bind ids to the variable names.
	 * @param questionDef the question definition object that is currently being parsed.
	 * @param relevants the map of constraint attribute values keyed by their 
	 * 					  question definition objects.
	 * @param repeatQtns a list of repeat question types.
	 * @param rptKidMap a map of question definition objects which are children of a repeat 
	 * 					question type, keyed by their variable names.
	 * @param currentPageNo the number of the current page we are parsing.
	 * @param parentQtn the parent of the question we are currently processing.
	 * @param constraints the map of constraint attribute values keyed by their 
	 * 					  question definition objects.
	 * @param orphanDynOptionQns a list of dynamic option definition questions who parent
	 *                           questions have not yet been parsed.
	 */
	private static void parseGroupElement(FormDef formDef, Element child, HashMap id2VarNameMap,IFormElement questionDef,HashMap relevants,Vector repeatQtns, HashMap rptKidMap, int currentPageNo, IFormElement parentQtn, HashMap constraints, List<QuestionDef> orphanDynOptionQns){
		String parentName = ((Element)child.getParentNode()).getNodeName();
		//if(!(parentName.equalsIgnoreCase(NODE_NAME_GROUP)||parentName.equalsIgnoreCase(NODE_NAME_GROUP_MINUS_PREFIX))){
		//if(!XmlUtil.nodeNameEquals(parentName,XformConstants.NODE_NAME_GROUP_MINUS_PREFIX)){
		/*if(formDef.getPageCount() < ++currentPageNo)
				formDef.addPage();
			formDef.setPageGroupNode((Element)child);*/
		

		GroupDef groupDef = new GroupDef();
		groupDef.setControlNode((Element)child);

		if(parentQtn == null)
			formDef.addElement(groupDef);
		else
			parentQtn.addChild(groupDef);

		questionDef = groupDef;
		parentQtn = questionDef;
		
		parseElement(formDef, child,id2VarNameMap,questionDef,relevants,repeatQtns,rptKidMap,currentPageNo,parentQtn,constraints,orphanDynOptionQns);
	}


	/**
	 * Parses a bind element of an xforms document.
	 * 
	 * @param formDef the form definition object that we are building.
	 * @param element the element that we are currently parsing.
	 * @param id2VarNameMap a map of question bind ids to the variable names.
	 * @param questionDef the question definition object that is currently being parsed.
	 * @param relevants the map of constraint attribute values keyed by their 
	 * 					  question definition objects.
	 * @param repeatQtns a list of repeat question types.
	 * @param rptKidMap a map of question definition objects which are children of a repeat 
	 * 					question type, keyed by their variable names.
	 * @param currentPageNo the number of the current page we are parsing.
	 * @param parentQtn the parent of the question we are currently processing.
	 * @param constraints the map of constraint attribute values keyed by their 
	 * 					  question definition objects.
	 * @param orphanDynOptionQns a list of dynamic option definition questions who parent
	 *                           questions have not yet been parsed.
	 * @return the question we are currently parsing.
	 */
	private static QuestionDef parseBindElement(FormDef formDef, Element child, HashMap id2VarNameMap,IFormElement questionDef,HashMap relevants,Vector repeatQtns, HashMap rptKidMap, int currentPageNo, IFormElement parentQtn, HashMap constraints, List<QuestionDef> orphanDynOptionQns){
		QuestionDef qtn = new QuestionDef(null);
		qtn.setBindNode(child);
		qtn.setId(getNextQuestionId());
		qtn.setVariableName(XformParserUtil.getQuestionVariableName(child,formDef));
		XformParserUtil.setQuestionType(qtn,child.getAttribute(XformConstants.ATTRIBUTE_NAME_TYPE),child);
		if(child.getAttribute(XformConstants.ATTRIBUTE_NAME_REQUIRED) != null && child.getAttribute(XformConstants.ATTRIBUTE_NAME_REQUIRED).equals(XformConstants.XPATH_VALUE_TRUE)){
			if(child.getAttribute(XformConstants.ATTRIBUTE_NAME_ACTION) == null)
				qtn.setRequired(true);
		}
		if(child.getAttribute(XformConstants.ATTRIBUTE_NAME_READONLY) != null && child.getAttribute(XformConstants.ATTRIBUTE_NAME_READONLY).equals(XformConstants.XPATH_VALUE_TRUE))
			qtn.setEnabled(false);
		if(child.getAttribute(XformConstants.ATTRIBUTE_NAME_LOCKED) != null && child.getAttribute(XformConstants.ATTRIBUTE_NAME_LOCKED).equals(XformConstants.XPATH_VALUE_TRUE))
			qtn.setLocked(true);
		if(child.getAttribute(XformConstants.ATTRIBUTE_NAME_VISIBLE) != null && child.getAttribute(XformConstants.ATTRIBUTE_NAME_VISIBLE).equals(XformConstants.XPATH_VALUE_FALSE))
			qtn.setVisible(false);

		if(!addRepeatChildQtn(qtn,repeatQtns,child,id2VarNameMap,rptKidMap)){
			String id = child.getAttribute(XformConstants.ATTRIBUTE_NAME_ID);
			id2VarNameMap.put(id != null ? id : qtn.getBinding(), qtn.getBinding());
			formDef.addElement(qtn);
		}

		if(child.getAttribute(XformConstants.ATTRIBUTE_NAME_RELEVANT) != null)
			relevants.put(qtn,child.getAttribute(XformConstants.ATTRIBUTE_NAME_RELEVANT));

		if(child.getAttribute(XformConstants.ATTRIBUTE_NAME_CONSTRAINT) != null)
			constraints.put(qtn,child.getAttribute(XformConstants.ATTRIBUTE_NAME_CONSTRAINT));

		if(child.getAttribute(XformConstants.ATTRIBUTE_NAME_CALCULATE) != null)
			formDef.addCalculation(new Calculation(qtn.getId(),child.getAttribute(XformConstants.ATTRIBUTE_NAME_CALCULATE)));

		if(qtn.getDataType() == QuestionDef.QTN_TYPE_REPEAT){
			RepeatQtnsDef repeatQtnsDef = new RepeatQtnsDef(qtn);
			qtn.setRepeatQtnsDef(repeatQtnsDef);
			repeatQtns.addElement(qtn);

			//questionDef = qtn;
		}

		return qtn;
	}


	/**
	 * Parses a UI element of an xforms document.
	 * 
	 * @param formDef the form definition object that we are building.
	 * @param element the element that we are currently parsing.
	 * @param id2VarNameMap a map of question bind ids to the variable names.
	 * @param questionDef the question definition object that is currently being parsed.
	 * @param relevants the map of constraint attribute values keyed by their 
	 * 					  question definition objects.
	 * @param repeatQtns a list of repeat question types.
	 * @param rptKidMap a map of question definition objects which are children of a repeat 
	 * 					question type, keyed by their variable names.
	 * @param currentPageNo the number of the current page we are parsing.
	 * @param parentQtn the parent of the question we are currently processing.
	 * @param constraints the map of constraint attribute values keyed by their 
	 * 					  question definition objects.
	 * @param orphanDynOptionQns a list of dynamic option definition questions who parent
	 *                           questions have not yet been parsed.
	 * @param nodeContext the current node context.
	 * @return the question we are currently parsing.
	 */
	private static IFormElement parseUiElement(FormDef formDef, Element child, HashMap id2VarNameMap,IFormElement questionDef,HashMap relevants,Vector repeatQtns, HashMap rptKidMap, int currentPageNo, IFormElement parentQtn, HashMap constraints, List<QuestionDef> orphanDynOptionQns, NodeContext nodeContext){
		String ref = child.getAttribute(XformConstants.ATTRIBUTE_NAME_REF);
		String bind = child.getAttribute(XformConstants.ATTRIBUTE_NAME_BIND);
		if(ref == null && bind == null)
			ref = child.getAttribute(XformConstants.ATTRIBUTE_NAME_NODESET);
		String varName = (String)id2VarNameMap.get(((ref != null) ? ref : bind));

		String tagname = child.getNodeName();

		//new addition may cause bugs
		if(varName == null){

			if(ref != null && ref.startsWith("/"+formDef.getVariableName()+"/"))
				varName = ref.replace("/"+formDef.getVariableName()+"/", "");

			if(formDef.getElement(varName) == null)
				varName = addNonBindControl(formDef,child,relevants,ref,bind,constraints,parentQtn);

			if(ref != null)
				id2VarNameMap.put(ref, ref);
		}

		if(varName != null){
			IFormElement qtn = formDef.getElement(varName);
			if(qtn == null)
				qtn = (QuestionDef)rptKidMap.get(varName);

			if(XmlUtil.nodeNameEquals(tagname,XformConstants.NODE_NAME_SELECT1_MINUS_PREFIX) || XmlUtil.nodeNameEquals(tagname,XformConstants.NODE_NAME_SELECT_MINUS_PREFIX)){
				qtn.setDataType((XmlUtil.nodeNameEquals(tagname,XformConstants.NODE_NAME_SELECT1_MINUS_PREFIX)) ? QuestionDef.QTN_TYPE_LIST_EXCLUSIVE : QuestionDef.QTN_TYPE_LIST_MULTIPLE);
				((QuestionDef)qtn).setOptions(new Vector());
			}//TODO first addition for repeats
			else if(XmlUtil.nodeNameEquals(tagname,XformConstants.NODE_NAME_REPEAT_MINUS_PREFIX) && !nodeContext.getLabel().equals("")){
				questionDef.setDataType(QuestionDef.QTN_TYPE_REPEAT);
				questionDef.setText(nodeContext.getLabel());
				questionDef.setHelpText(nodeContext.getHint());

				questionDef.setBinding(qtn.getBinding());
				questionDef.setBindNode(qtn.getBindNode());

				nodeContext.setLabel("");
				nodeContext.setHint("");

				questionDef.setLabelNode(nodeContext.getLabelNode());
				questionDef.setHintNode(nodeContext.getHintNode());

				questionDef.setControlNode(child);
				setQuestionDataNode(questionDef,formDef,parentQtn);
				parentQtn = questionDef;

				formDef.getChildren().remove(qtn);
				formDef.getChildren().remove(qtn);
			}
			else if(XmlUtil.nodeNameEquals(tagname,XformConstants.NODE_NAME_UPLOAD_MINUS_PREFIX)){
				if("image/*".equalsIgnoreCase(child.getAttribute(XformConstants.ATTRIBUTE_NAME_MEDIATYPE)))
					qtn.setDataType(QuestionDef.QTN_TYPE_IMAGE);
				else if("audio/*".equalsIgnoreCase(child.getAttribute(XformConstants.ATTRIBUTE_NAME_MEDIATYPE)))
					qtn.setDataType(QuestionDef.QTN_TYPE_AUDIO);
				else if("video/*".equalsIgnoreCase(child.getAttribute(XformConstants.ATTRIBUTE_NAME_MEDIATYPE)))
					qtn.setDataType(QuestionDef.QTN_TYPE_VIDEO);
			}

			//TODO second addition for repeats
			Element parent = (Element)child.getParentNode(); 
			if(XmlUtil.nodeNameEquals(parent.getNodeName(),XformConstants.NODE_NAME_REPEAT_MINUS_PREFIX)){
				varName = (String)id2VarNameMap.get(parent.getAttribute(XformConstants.ATTRIBUTE_NAME_BIND) != null ? parent.getAttribute(XformConstants.ATTRIBUTE_NAME_BIND) : parent.getAttribute(XformConstants.ATTRIBUTE_NAME_NODESET));
				IFormElement rptQtnDef = formDef.getElement(varName);
				qtn.setId(getNextQuestionId());


				qtn.setBindNode(child);
				qtn.setControlNode(child);

				//Remove repeat question constraint if any
				XformParserUtil.replaceConstraintQtn(constraints,(QuestionDef)qtn);
			}

			if(XmlUtil.nodeNameEquals(child.getNodeName(),XformConstants.NODE_NAME_TRIGGER_MINUS_PREFIX))
				qtn.setDataType(QuestionDef.QTN_TYPE_LABEL);

			questionDef = qtn;
			parseElement(formDef, child, id2VarNameMap,questionDef,relevants,repeatQtns,rptKidMap,currentPageNo,parentQtn,constraints,orphanDynOptionQns);
		}
		
		//For the children being oredered in the correct way (by appearance of the Control node in the xml doc).
		IFormElement parent = questionDef.getParent();
		parent.removeChild(questionDef);
		parent.addChild(questionDef);
		
		
		return questionDef;
	}


	/**
	 * Parses a label element of an xforms document.
	 * 
	 * @param formDef the form definition object that we are building.
	 * @param child the element that we are currently parsing.
	 * @param questionDef the question definition object that is currently being parsed.
	 * @param nodeContext the current node context.
	 */
	private static void parseLabelElement(FormDef formDef, Element child, IFormElement questionDef, NodeContext nodeContext){
		String parentName = ((Element)child.getParentNode()).getNodeName();
		if(XmlUtil.nodeNameEquals(parentName,XformConstants.NODE_NAME_INPUT_MINUS_PREFIX) || XmlUtil.nodeNameEquals(parentName,XformConstants.NODE_NAME_SELECT_MINUS_PREFIX) ||
				XmlUtil.nodeNameEquals(parentName,XformConstants.NODE_NAME_SELECT1_MINUS_PREFIX) || XmlUtil.nodeNameEquals(parentName,XformConstants.NODE_NAME_ITEM_MINUS_PREFIX) ||
				XmlUtil.nodeNameEquals(parentName,XformConstants.NODE_NAME_UPLOAD_MINUS_PREFIX) || XmlUtil.nodeNameEquals(parentName,XformConstants.NODE_NAME_TRIGGER_MINUS_PREFIX)){
				String label = XmlUtil.getTextValue(child);
				if(label == null) label = "";
				nodeContext.setLabel(label);
				nodeContext.setLabelNode(child);
			
		}
		else if(XmlUtil.nodeNameEquals(parentName,XformConstants.NODE_NAME_REPEAT_MINUS_PREFIX)){
			if(questionDef != null)
				questionDef.setText(getText(child));
		}
		else if(XmlUtil.nodeNameEquals(parentName,XformConstants.NODE_NAME_GROUP_MINUS_PREFIX)){
				String label = XmlUtil.getTextValue(child);
				if(label == null) label = "";
				nodeContext.setLabel(label);
				nodeContext.setLabelNode(child);
		}
	}


	/**
	 * Parses a hint element of an xforms document.
	 * 
	 * @param formDef the form definition object that we are building.
	 * @param child the element that we are currently parsing.
	 * @param questionDef the question definition object that is currently being parsed.
	 * @param nodeContext the current node context.
	 */
	private static void parseHintElement(FormDef formDef, Element child, IFormElement questionDef, NodeContext nodeContext){
		String parentName = ((Element)child.getParentNode()).getNodeName();
		//if(parentName.equalsIgnoreCase(NODE_NAME_GROUP)||parentName.equalsIgnoreCase(NODE_NAME_GROUP_MINUS_PREFIX)){
		if(XmlUtil.nodeNameEquals(parentName,XformConstants.NODE_NAME_GROUP_MINUS_PREFIX)){
			if(true /*child.getChildNodes().getLength() != 0*/){
				nodeContext.setHint(getText(child));
				nodeContext.setHintNode(child);
			}
		}
		else if(questionDef != null){
			if(true /*child.getChildNodes().getLength() != 0*/){
				questionDef.setHelpText(getText(child));
				questionDef.setHintNode(child /*element*/);
			}
		}
	}

	private static String getText(Element node){
		if(node.getChildNodes().getLength() != 0){
			String text = node.getChildNodes().item(0).getNodeValue().trim();
			if(text.length() > 0)
				return text;
		}
		return node.getAttribute("ref");
	}
}
