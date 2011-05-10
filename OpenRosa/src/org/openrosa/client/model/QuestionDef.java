package org.openrosa.client.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.openrosa.client.OpenRosaConstants;
import org.openrosa.client.controller.QuestionChangeListener;
import org.openrosa.client.util.Itext;
import org.openrosa.client.util.ItextParser;
import org.openrosa.client.xforms.UiElementBuilder;
import org.openrosa.client.locale.LocaleText;
import org.openrosa.client.model.ModelConstants;
import org.openrosa.client.util.FormUtil;
import org.openrosa.client.xforms.XformBuilderUtil;
import org.openrosa.client.xforms.XformConstants;
import org.openrosa.client.xforms.XformUtil;
import org.openrosa.client.xforms.XmlUtil;
import org.openrosa.client.xpath.XPathExpression;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.Element;
import com.google.gwt.xml.client.Node;
import com.google.gwt.xml.client.NodeList;


/** 
 * This is the question definition.
 * 
 * @author Daniel Kayiwa
 *
 */
public class QuestionDef implements IFormElement, Serializable{

	/** The value to save for boolean questions when one selects the yes option. */
	public static final String TRUE_VALUE = "true";

	/** The value to save for the boolean questions when one selects the no option. */
	public static final String FALSE_VALUE = "false";

	/** The text to display for boolean questions for the yes option. */
	public static final String TRUE_DISPLAY_VALUE = LocaleText.get("yes");

	/** The text to display for boolean questions for the no option. */
	public static final String FALSE_DISPLAY_VALUE = LocaleText.get("no");

	/** The prompt text. The text the user sees. */
	private String text = ModelConstants.EMPTY_STRING;

	/** The help text. */
	private String helpText = ModelConstants.EMPTY_STRING;

	/** The type of question. eg Numeric,Date,Text etc. */
	private int dataType = QTN_TYPE_TEXT;

	/** The value supplied as answer if the user has not supplied one. */
	private String defaultValue;

	/** The question answer of value. */
	private String answer;

	//TODO For a smaller payload, may need to combine (mandatory,visible,enabled,locked) 
	//into bit fields forming one byte. This would be a saving of 3 bytes per question.
	/** A flag to tell whether the question is to be answered or is optional. */
	private boolean required = false;

	/** A flag to tell whether the question should be shown or not. */
	private boolean visible = true;

	/** A flag to tell whether the question should be enabled or disabled. */
	private boolean enabled = true;

	/** A flag to tell whether a question is to be locked or not. A locked question 
	 * is one which is visible, enabled, but cannot be edited.
	 */
	private boolean locked = false;

	//TODO We have a bug here when more than one question, on a form, have the 
	//same variable names.
	//TODO May not need to serialize this property for smaller pay load. Then we would just rely on the id.
	/** The text indentifier of the question. This is used by the users of the questionaire 
	 * but in code we use the dynamically generated numeric id for speed. 
	 */
	private String questionID = ModelConstants.EMPTY_STRING;

	/** The allowed set of values (OptionDef) for an answer of the question. 
	 * This also holds repeat sets of questions (RepeatQtnsDef) for the QTN_TYPE_REPEAT.
	 * This is an optimization aspect to prevent storing these guys differently as 
	 * they can't both happen at the same time. The internal storage implementation of these
	 * repeats is hidden from the user by means of getRepeatQtnsDef() and setRepeatQtnsDef().
	 */
	private Object options;

	/** The numeric identifier of a question. When a form definition is being built, each question is 
	 * given a unique (on a form) id starting from 1 up to 127. The assumption is that one will never need to have
	 * a form with more than 127 questions for a mobile device (It would be too big).
	 */
	private int id = ModelConstants.NULL_ID;
	
	public static final int QTN_TYPE_NULL = 0;

	/** Text question type. */
	public static final int QTN_TYPE_TEXT = 1;

	/** Numeric question type. These are numbers without decimal points*/
	public static final int QTN_TYPE_NUMERIC = 2;

	/** Decimal question type. These are numbers with decimals */
	public static final int QTN_TYPE_DECIMAL = 3;

	/** Date question type. This has only date component without time. */
	public static final int QTN_TYPE_DATE = 4;

	/** Time question type. This has only time element without date*/
	public static final int QTN_TYPE_TIME = 5;

	/** This is a question with alist of options where not more than one option can be selected at a time. */
	public static final int QTN_TYPE_LIST_EXCLUSIVE = 6;

	/** This is a question with alist of options where more than one option can be selected at a time. */
	public static final int QTN_TYPE_LIST_MULTIPLE = 7;

	/** Date and Time question type. This has both the date and time components*/
	public static final int QTN_TYPE_DATE_TIME = 8;

	/** Question with true and false answers. */
	public static final int QTN_TYPE_BOOLEAN = 9;

	/** Question with repeat sets of questions. */
	public static final int QTN_TYPE_REPEAT = 10;

	/** Question with image. */
	public static final int QTN_TYPE_IMAGE = 11;

	/** Question with recorded video. */
	public static final byte QTN_TYPE_VIDEO = 12;

	/** Question with recoded audio. */
	public static final byte QTN_TYPE_AUDIO = 13;

	/** Question whose list of options varies basing on the value selected from another question.
	 * An example of such a question would be countries where the list depends on the continent
	 * selected in the continent question.
	 */
	public static final int QTN_TYPE_LIST_EXCLUSIVE_DYNAMIC = 14;

	/** Question with GPS cordinates. */
	public static final int QTN_TYPE_GPS = 15;
	
	/** Question with barcode cordinates. */
	public static final int QTN_TYPE_BARCODE = 16;
	
	/** Question which is a group. */
	public static final int QTN_TYPE_GROUP = 17;
	
	/** Question which is a group. */
	public static final int QTN_TYPE_LABEL = 18;

	/** The xforms model data node into which this question will feed its answer. */
	private Element dataNode;

	/** The xforms label node for this question. */
	private Element labelNode;

	/** The xforms hint node for this question. */
	private Element hintNode;

	/** The xforms bind node for this question. */
	private Element bindNode;

	/** The xforms input,select, or select1 node for the question. */
	private Element controlNode;

	/** For select and select1 questions, this is the reference to the node representing
	 * the first option.
	 */
	private Element firstOptionNode;


	/** A list of interested listeners to the question change events. */
	private List<QuestionChangeListener> changeListeners = new ArrayList<QuestionChangeListener>();

	/** The parent object for this question. It could be a page or
	 * just another question as for repeat question kids. 
	 */
	private IFormElement parent;

	private String itextId;
	
	/**
	 * Flag used to determine if this QuestionDef should have a 
	 * Control node (input, 1select, etc) generated upon XML output.
	 */
	private boolean hasUINode = true;
	
	private boolean hasAdvancedCalculate, hasAdvancedConstraint, hasAdvancedRelevant;
	
	private String advancedCalculate, advancedConstraint, advancedRelevant;
	
	/**
	 * Node path to an integer value that tells the JR engine
	 * how many times to do a repeat.  Shows up as a
	 * "jr:repeatCount" attribute in the control node.
	 */
	private String repeatCountNodePath;


	/** This constructor is used mainly during deserialization. */
	public QuestionDef(IFormElement parent){
		this.parent = parent;
	}

	/** The copy constructor. */
	public QuestionDef(QuestionDef questionDef, IFormElement parent){
		this(parent);
		setId(questionDef.getId());
		setText(questionDef.getText());
		setHelpText(questionDef.getHelpText());
		setDataType(questionDef.getDataType());
		setDefaultValue(questionDef.getDefaultValue());
		setVisible(questionDef.isVisible());
		setEnabled(questionDef.isEnabled());
		setLocked(questionDef.isLocked());
		setRequired(questionDef.isRequired());
		setVariableName(questionDef.getQuestionID());
		setItextId(questionDef.getItextId());

		if(getDataType() == QuestionDef.QTN_TYPE_LIST_EXCLUSIVE || getDataType() == QuestionDef.QTN_TYPE_LIST_MULTIPLE)
			copyQuestionOptions(questionDef.getOptions());
		else if(getDataType() == QuestionDef.QTN_TYPE_REPEAT){
			this.options = new RepeatQtnsDef(questionDef.getRepeatQtnsDef(),this);
		}
	}

	public QuestionDef(int id,String text,  int type, String variableName, IFormElement parent) {
		this(parent);
		setId(id);
		setText(text);
		setDataType(type);
		setVariableName(variableName);
	}

	/**
	 * Constructs a new question definition object from the supplied parameters.
	 * For String type parameters, they should NOT be NULL. They should instead be empty,
	 * for the cases of missing values.
	 * 
	 * @param id
	 * @param text
	 * @param helpText - The hint or help text. Should NOT be NULL.
	 * @param mandatory
	 * @param type
	 * @param defaultValue
	 * @param visible
	 * @param enabled
	 * @param locked
	 * @param variableName
	 * @param options
	 */
	public QuestionDef(int id,String text, String helpText, boolean mandatory, int type, String defaultValue, boolean visible, boolean enabled, boolean locked, String variableName, Object options,IFormElement parent) {
		this(parent);
		setId(id);
		setText(text);
		setHelpText(helpText);
		setDataType(type);
		setDefaultValue(defaultValue);
		setVisible(visible);
		setEnabled(enabled);
		setLocked(locked);
		setRequired(mandatory);		
		setVariableName(variableName);
		setOptions(options);
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public static boolean isDateFunction(String value){
		if(value == null)
			return false;

		return (value.contains("now()") || value.contains("date()")
				||value.contains("getdate()") || value.contains("today()"));
	}

	public static Date getDateFunctionValue(String function){
		return new Date();
	}

	public boolean isDate(){
		return (dataType == QuestionDef.QTN_TYPE_DATE_TIME || 
				dataType == QuestionDef.QTN_TYPE_DATE ||
				dataType == QuestionDef.QTN_TYPE_TIME);
	}

	public String getDefaultValueDisplay() {
		if(isDate() && isDateFunction(defaultValue)){
			if(dataType == QuestionDef.QTN_TYPE_TIME)
				return FormUtil.getTimeDisplayFormat().format(getDateFunctionValue(defaultValue));
			else if(dataType == QuestionDef.QTN_TYPE_DATE_TIME)
				return FormUtil.getDateTimeDisplayFormat().format(getDateFunctionValue(defaultValue));
			else
				return FormUtil.getDateDisplayFormat().format(getDateFunctionValue(defaultValue));
		}

		return defaultValue;
	}

	public String getDefaultValueSubmit() {
		if(isDate() && isDateFunction(defaultValue)){
			if(dataType == QuestionDef.QTN_TYPE_TIME)
				return FormUtil.getTimeSubmitFormat().format(new Date());
			else if(dataType == QuestionDef.QTN_TYPE_DATE_TIME)
				return FormUtil.getDateTimeSubmitFormat().format(new Date());
			else
				return FormUtil.getDateSubmitFormat().format(new Date());
		}

		return defaultValue;
	}

	public void setDefaultValue(String defaultValue) {
		//if(defaultValue != null && defaultValue.trim().length() > 0)
		this.defaultValue = defaultValue;
		this.answer =  defaultValue;
	}

	public String getAnswer() {
		return answer;
	}

	public void setAnswer(String answer) {
		//if(defaultValue != null && defaultValue.trim().length() > 0)
		this.answer = answer;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		boolean changed = this.enabled != enabled;

		this.enabled = enabled;

		if(changed){
			for(int index = 0; index < changeListeners.size(); index++)
				changeListeners.get(index).onEnabledChanged(this,enabled);
		}
	}

	public String getHelpText() {
		return helpText;
	}

	public void setHelpText(String helpText) {
		this.helpText = helpText;
	}

	public boolean isLocked() {
		return locked;
	}

	public void setLocked(boolean locked) {
		boolean changed = this.locked != locked;

		this.locked = locked;

		if(changed){
			for(int index = 0; index < changeListeners.size(); index++)
				changeListeners.get(index).onLockedChanged(this,locked);
		}
	}

	public boolean isRequired() {
		return required;
	}

	public void setRequired(boolean required) {
		boolean changed = this.required != required;

		this.required = required;

		if(changed){
			for(int index = 0; index < changeListeners.size(); index++)
				changeListeners.get(index).onRequiredChanged(this,required);
		}
	}

	public List getOptions() {
		//if(!(type == QTN_TYPE_LIST_EXCLUSIVE || type == QTN_TYPE_LIST_MULTIPLE))
		//	throw new Exception("Invalid Operation");
		return (List)options;
	}

	public void setOptions(Object options) {
		this.options = options;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {	
		this.text = text;
	}

	public int getDataType() {
		return dataType;
	}

	public String getItextId() {
			return itextId;
	}

	public void setItextId(String itextId) {
			this.itextId = itextId;
	}

	public List<String> getAllChildrenItextIDs(){
		ArrayList<String> list = new ArrayList<String>();
		List<IFormElement> children = this.getChildren();
		if(children == null){ return new ArrayList<String>(); }
		
		for(IFormElement child : children){
			list.addAll(Itext.getFullAvailableTextForms(child.getItextId())); //get the child's ItextID(s)
			list.addAll(child.getAllChildrenItextIDs()); //recurse down
		}
		return list;
	}
	
	public void setDataType(int dataType) {
		int oldDataType = this.dataType;
		int newDataType = dataType;
		boolean changed = oldDataType != newDataType;
		this.dataType = dataType;

		if(changed){
			
			//if(controlNode != null && (dataType == QuestionDef.QTN_TYPE_LIST_EXCLUSIVE || dataType == QuestionDef.QTN_TYPE_LIST_MULTIPLE))
			//	options = new ArrayList();
			boolean selectTransform = isAndWasASelect(oldDataType,newDataType);
			boolean groupRepeatTransform = isAndWasAGroupOrRepeat(oldDataType, newDataType);
			
			if(!selectTransform){ //the options list has lost meaning so delete it
				options = null;
			}
			
			if(!groupRepeatTransform){ //the groups contained herein as children have no meaning, delete them.
				options = null;
			}
			
			if(dataType == QuestionDef.QTN_TYPE_REPEAT && options == null){
				options = new RepeatQtnsDef(this);
			}
			
			
			for(int index = 0; index < changeListeners.size(); index++)
				changeListeners.get(index).onDataTypeChanged(this,dataType);
		}
	}
	
	/**
	 * Checks to see that both dataTypes are some kind of a select
	 * @param oldType - Previous Data Type
	 * @param newType - New Data Type
	 * @return True if both old and new are select and/or multiselect
	 */
	private static boolean isAndWasASelect(int oldType, int newType){
		if(oldType!=QTN_TYPE_LIST_EXCLUSIVE && oldType != QTN_TYPE_LIST_MULTIPLE){
			return false;
		}
		
		if(newType!=QTN_TYPE_LIST_EXCLUSIVE && newType != QTN_TYPE_LIST_MULTIPLE){
			return false;
		}
		
		return true;
	}

	/**
	 * Checks to see that both dataTypes are some kind of a repeat and/or group
	 * @param oldType - Previous Data Type
	 * @param newType - New Data Type
	 * @return True if both old and new are repeat and/or group type
	 */
	private static boolean isAndWasAGroupOrRepeat(int oldType, int newType){
		if(oldType!=QTN_TYPE_GROUP && oldType != QTN_TYPE_REPEAT){
			return false;
		}
		
		if(newType!=QTN_TYPE_GROUP && newType != QTN_TYPE_REPEAT){
			return false;
		}
		
		return true;
	}
	
	public String getQuestionID() {
		return questionID;
	}

	private void setVariableName(String variableName) {
		boolean changed = this.questionID != variableName;
		if(getDataType() == QuestionDef.QTN_TYPE_REPEAT){
			this.getRepeatQtnsDef().setQuestionIDInternal(variableName);
		}
		this.questionID = variableName;

		if(changed){
			for(int index = 0; index < changeListeners.size(); index++)
				changeListeners.get(index).onBindingChanged(this,variableName);
		}
	}
	
	/**
	 * Returns the element specified by varName.
	 * if varName matches the parent node, return self,
	 * else go through children elements and return a match
	 * If no match is found, return null.
	 * @param varName
	 * @return the IFormElement that matches varName or null if no match.
	 */
	public IFormElement getElement(String varName){
		if(varName == null){
			return null;
		}
		if(getQuestionID().equals(varName)){
			return this;
		}else{
			if(this.getDataType() == QuestionDef.QTN_TYPE_REPEAT){
				return this.getRepeatQtnsDef().getElement(varName);
			}
			
			return null;
			
		}
	}

	public IFormElement getParent() {
		return parent;
	}

	public void setParent(IFormElement parent) {
		this.parent = parent;
	}
	
	/**
	 * Get the Nodeset ref that points to the data node where the question's answer will be stored.
	 * @return
	 */
	public String getDataNodesetPath(){
		if(getParent() == null){
			return "/"+getQuestionID();
		}else{
			return getParent().getDataNodesetPath() + "/"+getQuestionID();
		}
		
	}

	/**
	 * @return the bindNode
	 */
	public Element getBindNode() {
		return bindNode;
	}

	/**
	 * @param bindNode the bindNode to set
	 */
	public void setBindNode(Element bindNode) {
		this.bindNode = bindNode;
	}

	/**
	 * @return the dataNode
	 */
	public Element getDataNode() {
		return dataNode;
	}

	/**
	 * @param dataNode the dataNode to set
	 */
	public void setDataNode(Element dataNode) {
		this.dataNode = dataNode;
	}

	/**
	 * @return the hintNode
	 */
	public Element getHintNode() {
		return hintNode;
	}

	/**
	 * @param hintNode the hintNode to set
	 */
	public void setHintNode(Element hintNode) {
		this.hintNode = hintNode;
	}

	/**
	 * @return the labelNode
	 */
	public Element getLabelNode() {
		return labelNode;
	}

	/**
	 * @param labelNode the labelNode to set
	 */
	public void setLabelNode(Element labelNode) {
		this.labelNode = labelNode;

//		if(itextId == null)
//			setItextId(XmlUtil.getItextId(labelNode));
	}

	/**
	 * @return the controlNode
	 */
	public Element getControlNode() {
		return controlNode;
	}

	/**
	 * @param controlNode the controlNode to set
	 */
	public void setControlNode(Element controlNode) {
		this.controlNode = controlNode;
	}

	/**
	 * @return the firstOptionNode
	 */
	public Element getFirstOptionNode() {
		return firstOptionNode;
	}

	/**
	 * @param firstOptionNode the firstOptionNode to set
	 */
	public void setFirstOptionNode(Element firstOptionNode) {
		this.firstOptionNode = firstOptionNode;
	}

	public boolean isVisible() {
		return visible;
	}

	public void setVisible(boolean visible) {
		boolean changed = this.visible != visible;

		this.visible = visible;

		if(changed){
			for(int index = 0; index < changeListeners.size(); index++)
				changeListeners.get(index).onVisibleChanged(this,visible);
		}
	}

	public void removeChangeListener(QuestionChangeListener changeListener) {
		changeListeners.remove(changeListener);
	}

	public void addChangeListener(QuestionChangeListener changeListener) {
		if(!changeListeners.contains(changeListener))
			changeListeners.add(changeListener);
	}

	public void clearChangeListeners(){
		if(changeListeners != null)
			changeListeners.clear();
	}

	public void addOption(OptionDef optionDef){
		addOption(optionDef,true);
	}

	public void addOption(OptionDef optionDef, boolean setAsParent){
		if(options == null || !(options instanceof ArrayList)){
			options = new ArrayList();
		}
		((List)options).add(optionDef);

		if(setAsParent){
			optionDef.setParent(this);
		}
	}

	public RepeatQtnsDef getRepeatQtnsDef(){
		return (RepeatQtnsDef)options;
	}

	public void addRepeatChildDef(IFormElement qtn){
		if(options == null){
			options = new RepeatQtnsDef(this);
		}
		((RepeatQtnsDef)options).addChild(qtn);
		qtn.setParent((RepeatQtnsDef)options);
	}

	public void setRepeatQtnsDef(RepeatQtnsDef repeatQtnsDef){
		options = repeatQtnsDef;
	}

	public String toString() {
		return Itext.getDisplayText(this);
	}

	private void copyQuestionOptions(List options){
		if(options == null)
			return;

		this.options = new ArrayList();
		for(int i=0; i<options.size(); i++)
			((List)this.options).add(new OptionDef((OptionDef)options.get(i),this));
	}

	public boolean removeOption(OptionDef optionDef){
		if(options instanceof List){ //Could be a RepeatQtnsDef
			if(!((List)options).remove(optionDef))
				return false;

			if(((List)options).size() == 0)
				firstOptionNode = null;
		}

		if(controlNode != null && optionDef.getControlNode() != null)
			controlNode.removeChild(optionDef.getControlNode());
		
		return true;
	}

	public void moveOptionUp(OptionDef optionDef){
		if(!(getDataType()==QuestionDef.QTN_TYPE_LIST_EXCLUSIVE ||
				getDataType()==QuestionDef.QTN_TYPE_LIST_MULTIPLE))
			return;

		List optns = (List)options;
		int index = optns.indexOf(optionDef);

		optns.remove(optionDef);

		//Store the question to replace
		OptionDef currentOptionDef = (OptionDef)optns.get(index-1);
		if(controlNode != null && optionDef.getControlNode() != null && currentOptionDef.getControlNode() != null)
			controlNode.removeChild(optionDef.getControlNode());

		List list = new ArrayList();
		//Remove all from index before selected all the way downwards
		while(optns.size() >= index){
			currentOptionDef = (OptionDef)optns.get(index-1);
			list.add(currentOptionDef);
			optns.remove(currentOptionDef);
		}

		optns.add(optionDef);
		for(int i=0; i<list.size(); i++){
			if(i == 0){
				OptionDef optnDef = (OptionDef)list.get(i);
				if(controlNode != null && optnDef.getControlNode() != null && optionDef.getControlNode() != null)
					controlNode.insertBefore(optionDef.getControlNode(), optnDef.getControlNode());
			}
			optns.add(list.get(i));
		}
	}

	public void moveOptionDown(OptionDef optionDef){
		if(!(getDataType()==QuestionDef.QTN_TYPE_LIST_EXCLUSIVE ||
				getDataType()==QuestionDef.QTN_TYPE_LIST_MULTIPLE))
			return;

		List optns = (List)options;
		int index = optns.indexOf(optionDef);	

		optns.remove(optionDef);

		if(controlNode != null && optionDef.getControlNode() != null)
			controlNode.removeChild(optionDef.getControlNode());

		OptionDef currentItem; // = parent.getChild(index - 1);
		List list = new ArrayList();

		//Remove all otions below selected index
		while(optns.size() > 0 && optns.size() > index){
			currentItem = (OptionDef)optns.get(index);
			list.add(currentItem);
			optns.remove(currentItem);
		}

		for(int i=0; i<list.size(); i++){
			if(i == 1){
				optns.add(optionDef); //Add after the first item but before the current (second).

				if(controlNode != null){
					OptionDef optnDef = getNextSavedOption(list,i); //(OptionDef)list.get(i);
					if(optnDef.getControlNode() != null && optionDef.getControlNode() != null)
						controlNode.insertBefore(optionDef.getControlNode(), optnDef.getControlNode());
					else
						controlNode.appendChild(optionDef.getControlNode());
				}
			}
			optns.add(list.get(i));
		}

		//If was second last and hence becoming last
		if(list.size() == 1){
			optns.add(optionDef);

			if(controlNode != null && optionDef.getControlNode() != null)
				controlNode.appendChild(optionDef.getControlNode());
		}
	}

	private OptionDef getNextSavedOption(List options, int index){
		for(int i=index; i<options.size(); i++){
			OptionDef optionDef = (OptionDef)options.get(i);
			if(optionDef.getControlNode() != null)
				return optionDef;
		}
		return (OptionDef)options.get(index);
	}
	

	public boolean updateDoc(Document doc, Element xformsNode, FormDef formDef, Element formNode, Element modelNode,Element groupNode,boolean appendParentBinding, boolean withData, String rootDataNodeName){
		boolean isNew = (controlNode == null) && (dataNode == null) && (bindNode == null);
		if(isNew){ //Must be new question.
			Element parentNode;
			if(this.getParent() instanceof FormDef){
				 parentNode = formDef.getBodyNode();
			}else{
				parentNode = this.getParent().getControlNode();
			}
			UiElementBuilder.fromQuestionDef2Xform(this, doc, formDef, formNode, modelNode, parentNode);
			if(!hasUINode()){
				updateControlNode();
			}
		}else{
			updateControlNode();
		}

		if(hasUINode()){
			controlNode.removeAttribute("bind");
			if(this.getDataType() != QuestionDef.QTN_TYPE_REPEAT){ //repeats are special beasts.
				controlNode.setAttribute("ref", this.getDataNodesetPath());
			}
			
			if(getDataType() == QuestionDef.QTN_TYPE_REPEAT && getRepeatCountNodePath() != null && !getRepeatCountNodePath().isEmpty()){
				controlNode.setAttribute("jr:count", getRepeatCountNodePath());
			}else{
				controlNode.removeAttribute("jr:count");
				controlNode.removeAttribute("count");
			}
		}
		if(labelNode != null){
			XmlUtil.setTextNodeValue(labelNode,text);
			UiElementBuilder.addItextRefs(labelNode, this);
		}
		

		
		Element node = bindNode;
		if(node == null && hasUINode()){
			//No bindNode, generate one.
			node = UiElementBuilder.createBindNodeForIFormElement(this, doc, modelNode);
			this.setBindNode(node);
		}
		if(node != null){
			String binding = getDataNodesetPath();

			if(dataType != QuestionDef.QTN_TYPE_REPEAT){
				node.setAttribute(XformConstants.ATTRIBUTE_NAME_TYPE, XformBuilderUtil.getXmlType(dataType,node));
			}else{
				node.removeAttribute(XformConstants.ATTRIBUTE_NAME_TYPE);
			}
			boolean hasRefAttr = (node.getAttribute(XformConstants.ATTRIBUTE_NAME_REF) != null);
			boolean hasNodeSetAttr =(node.getAttribute(XformConstants.ATTRIBUTE_NAME_NODESET) != null);
			if(hasNodeSetAttr){
				node.setAttribute(XformConstants.ATTRIBUTE_NAME_NODESET,binding);
			}
			if(hasRefAttr){
				node.setAttribute(XformConstants.ATTRIBUTE_NAME_REF,binding);
			}
			
			boolean hasRefOrNode = hasRefAttr || hasNodeSetAttr;
			if(getParent()!= null && getParent().getDataType() == QuestionDef.QTN_TYPE_REPEAT){
				if(!hasRefOrNode){
					node.setAttribute(XformConstants.ATTRIBUTE_NAME_REF,questionID);
				}else if(hasRefAttr){
					node.setAttribute(XformConstants.ATTRIBUTE_NAME_REF,questionID);
				}else if(hasNodeSetAttr){
					node.setAttribute(XformConstants.ATTRIBUTE_NAME_NODESET,getDataNodesetPath());
				}
			}

			if(required){
				node.setAttribute(XformConstants.ATTRIBUTE_NAME_REQUIRED,XformConstants.XPATH_VALUE_TRUE);
			}else{
				node.removeAttribute(XformConstants.ATTRIBUTE_NAME_REQUIRED);
			}

			if(!enabled){
				node.setAttribute(XformConstants.ATTRIBUTE_NAME_READONLY,XformConstants.XPATH_VALUE_TRUE);
			}
			else{
				node.removeAttribute(XformConstants.ATTRIBUTE_NAME_READONLY);
			}

			/*if(locked)
				node.setAttribute(XformConstants.ATTRIBUTE_NAME_LOCKED,XformConstants.XPATH_VALUE_TRUE);
			else
				node.removeAttribute(XformConstants.ATTRIBUTE_NAME_LOCKED);*/

			if(!visible){
				node.setAttribute(XformConstants.ATTRIBUTE_NAME_VISIBLE,XformConstants.XPATH_VALUE_FALSE);
			}else{
				node.removeAttribute(XformConstants.ATTRIBUTE_NAME_VISIBLE);
			}


			if(!(dataType == QuestionDef.QTN_TYPE_IMAGE || dataType == QuestionDef.QTN_TYPE_AUDIO ||
					dataType == QuestionDef.QTN_TYPE_VIDEO || dataType == QuestionDef.QTN_TYPE_GPS)){
				node.removeAttribute(XformConstants.ATTRIBUTE_NAME_FORMAT);
			}else{
				if(dataType == QuestionDef.QTN_TYPE_GPS)
					node.setAttribute(XformConstants.ATTRIBUTE_NAME_TYPE,"geopoint");
				else
					node.setAttribute(XformConstants.ATTRIBUTE_NAME_TYPE,"binary");

				node.removeAttribute(XformConstants.ATTRIBUTE_NAME_FORMAT);
			}
			
			if(hasUINode()){
				if(!(dataType == QuestionDef.QTN_TYPE_IMAGE || dataType == QuestionDef.QTN_TYPE_AUDIO ||
						dataType == QuestionDef.QTN_TYPE_VIDEO)){
					controlNode.removeAttribute(XformConstants.ATTRIBUTE_NAME_MEDIATYPE);
				}else{
					UiElementBuilder.setMediaType(controlNode, dataType);
				}
			}

			if(dataNode != null){
				updateDataNode(doc,formDef,rootDataNodeName);
			}
		}

		if((getDataType() == QuestionDef.QTN_TYPE_LIST_EXCLUSIVE ||
				getDataType() == QuestionDef.QTN_TYPE_LIST_MULTIPLE) && options != null){

			boolean allOptionsNew = areAllOptionsNew();
			List newOptns = new ArrayList();
			List optns = (List)options;
			for(int i=0; i<optns.size(); i++){
				OptionDef optionDef = (OptionDef)optns.get(i);

				if(!allOptionsNew && optionDef.getControlNode() == null){
					newOptns.add(optionDef);
				}

				optionDef.updateDoc(doc,controlNode);
				if(i == 0){
					firstOptionNode = optionDef.getControlNode();
				}
			}

			for(int k = 0; k < newOptns.size(); k++){
				OptionDef optionDef = (OptionDef)newOptns.get(k);
				int proposedIndex = optns.size() - (newOptns.size() - k);
				int currentIndex = optns.indexOf(optionDef);
				if(currentIndex == proposedIndex){	continue; }

				moveOptionNodesUp(optionDef,getRefOption(optns,newOptns,currentIndex /*currentIndex+1*/));
			}
		}
		else if(getDataType() == QuestionDef.QTN_TYPE_REPEAT){
			getRepeatQtnsDef().updateDoc(doc, groupNode, formDef, formNode, modelNode, withData, rootDataNodeName);

			if(controlNode != null){
//				((Element)controlNode.
//						getParentNode()).
//						setAttribute(XformConstants.ATTRIBUTE_NAME_ID, variableName);
			}
			if(!withData && dataNode != null){
				//Remove all repeating data kids
				Element parent = (Element)dataNode.getParentNode();
				NodeList nodes = parent.getElementsByTagName(dataNode.getNodeName());
				for(int index = 1; index < nodes.getLength(); index++){
					Node child = nodes.item(index);
					child.getParentNode().removeChild(child);
				}
			}
		}

		//Put after options because it depends on the firstOptionNode
//		if(hintNode != null){
//			if(helpText.trim().length() > 0)
//				XmlUtil.setTextNodeValue(hintNode,helpText);
//			else{
//				controlNode.removeChild(hintNode);
//				hintNode = null;
//			}
//		}else if(hintNode == null && helpText.trim().length() > 0){
//			UiElementBuilder.addHelpTextNode(this, doc, controlNode, firstOptionNode);
//		}
		if(FormUtil.shouldHaveHintDOMNode(this)){
			if(hintNode == null){
				hintNode = doc.createElement("hint");
			}
			boolean hasDefaultHelpText = (helpText != null && !helpText.isEmpty());
			
			if(hasDefaultHelpText){
				hintNode.appendChild(doc.createTextNode(helpText));
			}
			
			boolean hasHelpItext = Itext.getDefaultLocale().hasID(itextId+";hint");
			if(hasHelpItext){
				hintNode.setAttribute("ref", "jr:itext('"+getItextId()+"_hint')");
			}
			
			getControlNode().appendChild(hintNode);
			
		}
		
		if(withData){
			updateNodeValue(doc,formNode,(answer != null) ? answer : defaultValue,withData);
		}else{
			updateNodeValue(doc,formNode,defaultValue,withData);
		}
		
//		UiElementBuilder.addItextRefs(controlNode, this);
		return isNew;
	}
	
	

	private boolean areAllOptionsNew(){
		if(options == null)
			return false;

		List optns = (List)options;
		for(int i=0; i<optns.size(); i++){
			OptionDef optionDef = (OptionDef)optns.get(i);
			if(optionDef.getControlNode() != null)
				return false;
		}
		return true;
	}

	private OptionDef getRefOption(List options, List newOptions, int index){
		OptionDef optionDef;
		int i = index + 1;
		while(i < options.size()){
			optionDef = (OptionDef)options.get(i);
			if(!newOptions.contains(optionDef))
				return optionDef;
			i++;
		}

		return null;
	}

	public void updateNodeValue(FormDef formDef){
		updateNodeValue(formDef.getDoc(),formDef.getDataNode(),answer,true);
	}

	public void updateNodeValue(Document doc, Element formNode,String value, boolean withData){
		if((dataType == QuestionDef.QTN_TYPE_DATE || dataType == QuestionDef.QTN_TYPE_DATE_TIME)
				&& value != null && value.trim().length() > 0){

			if(withData){
				DateTimeFormat formatter = (dataType == QuestionDef.QTN_TYPE_DATE_TIME) ? FormUtil.getDateTimeSubmitFormat() : FormUtil.getDateSubmitFormat(); //DateTimeFormat.getFormat(); //new DateTimeFormat("yyyy-MM-dd");

				if(value.contains("now()") || value.contains("date()")
						||value.contains("getdate()") || value.contains("today()"))
					value = formatter.format(new Date());
				else{
					//if(formatter != null)
					//	value = formatter.format(FormUtil.getDateTimeDisplayFormat().parse(value));
				}
			}
		}

		if(value != null && value.trim().length() > 0){
			if(questionID.contains("@"))
				updateAttributeValue(formNode,value);
			else if(dataNode != null){
				if(isBinaryType()){
					NodeList childNodes = dataNode.getChildNodes();
					while(childNodes.getLength() > 0)
						dataNode.removeChild(childNodes.item(0));
					//Window.alert(variableName+"="+value.length());
					dataNode.appendChild(doc.createTextNode(value));
				}
				else{
					if(dataNode.getChildNodes().getLength() > 0)
						dataNode.getChildNodes().item(0).setNodeValue(value);
					else
						dataNode.appendChild(doc.createTextNode(value));
				}
			}
		}
		else{
			//TODO Check to see that this does not remove child model node of repeats
			if(dataNode != null && dataType != QuestionDef.QTN_TYPE_REPEAT){
				if(questionID.contains("@"))
					updateAttributeValue(formNode,"");
				else{
					NodeList childNodes = dataNode.getChildNodes();
					while(childNodes.getLength() > 0)
						dataNode.removeChild(childNodes.item(0));
				}
			}
		}
	}

	/**
	 * Checks of this question is a multimedia (Picture,Audio & Video) type.
	 * 
	 * @return true if yes, else false.
	 */
	private boolean isBinaryType(){
		return (dataType == QuestionDef.QTN_TYPE_IMAGE || dataType == QuestionDef.QTN_TYPE_VIDEO ||
				dataType == QuestionDef.QTN_TYPE_AUDIO);
	}

	private void updateAttributeValue(Element formNode, String value){
		String xpath = questionID;		
		Element elem = formNode; //(Element)formNode.getParentNode();

		if(dataType != QuestionDef.QTN_TYPE_REPEAT){
			//xpath = new String(xpath.toCharArray(), 1, xpath.length()-1);
			int pos = xpath.lastIndexOf('@'); String attributeName = null;
			if(pos > 0){
				attributeName = xpath.substring(pos+1,xpath.length());
				xpath = xpath.substring(0,pos-1);
			}
			else if(pos == 0){
				attributeName = questionID.substring(1,questionID.length());
				if(value != null && value.trim().length() > 0) //we are not allowing empty strings for now.
					formNode.setAttribute(attributeName, value);
				return;
			}
			XPathExpression xpls = new XPathExpression(elem, xpath);
			List result = xpls.getResult();

			for (Iterator e = result.iterator(); e.hasNext();) {
				Object obj = e.next();
				if (obj instanceof Element){
					if(pos > 0) //Check if we are to set attribute value.
						((Element) obj).setAttribute(attributeName, value);
					else
						((Element) obj).setNodeValue(value);
				}
			}
		}
		else //TODO Need to work on repeats
			;//updateRepeatModel(elem,qtnData);
	}

	private void updateDataNode(Document doc, FormDef formDef, String rootDataNodeName){
//		if(questionID.contains("@")){
//			return;
//		}

		String name = dataNode.getNodeName();
		if(name.equals(questionID)){ //equalsIgnoreCase was bug because our xpath lib is case sensitive
			if(dataType != QuestionDef.QTN_TYPE_REPEAT){
				return;
			}
			if(dataType == QuestionDef.QTN_TYPE_REPEAT && formDef.getQuestionID().equals(dataNode.getParentNode().getNodeName())){
				return;
			}
		}

		if(questionID.contains("/") && name.equals(questionID.substring(questionID.lastIndexOf("/")+1)) && dataNode.getParentNode().getNodeName().equals(questionID.substring(0,questionID.indexOf("/")))){
			return;
		}


		String xml = dataNode.toString();
		if(!questionID.contains("/")){
			xml = xml.replace(name, questionID);
			Element node = XformUtil.getNode(xml);
			node = (Element)controlNode.getOwnerDocument().importNode(node, true);
			Element parent = (Element)dataNode.getParentNode();
			parent.replaceChild(node, dataNode);
			dataNode = node;
		}
		else{
			String newName = questionID.substring(questionID.lastIndexOf("/")+1);
			if(!name.equals(newName)){
				xml = xml.replace(name, newName);
				Element node = XformUtil.getNode(xml);
				node = (Element)controlNode.getOwnerDocument().importNode(node, true);
				Element parent = (Element)dataNode.getParentNode();
				parent.replaceChild(node, dataNode);
				dataNode = node;
			}

			String parentName = questionID.substring(0,questionID.indexOf("/"));
			String parentNodeName = dataNode.getParentNode().getNodeName();
			if(!parentName.equals(parentNodeName)){ //equalsIgnoreCase was bug because our xpath lib is case sensitive
				if(questionID.equals(parentName+"/"+parentNodeName+"/"+name))
					return;
				
				if(questionID.endsWith("/"+parentNodeName+"/"+name))
					return; //Some bindings have nested paths which expose some bug here.

				Element parentNode = doc.createElement(parentName);
				//parentNode = EpihandyXform.getNode(parentNode.toString());
				Element parent = (Element)dataNode.getParentNode();
				Element node = (Element)dataNode.cloneNode(true);
				parentNode.appendChild(node);
				if(this.getParent().getQuestionID().equals(parent.getNodeName()))
					parent.replaceChild(parentNode, dataNode);
				else
					//if(dataNode.getParentNode().getParentNode() != null)
					formDef.getDataNode().replaceChild(parentNode, dataNode.getParentNode());

				dataNode = node;
			}
		}

		String id = questionID;
		if(id.contains("/"))
			id = id.substring(id.lastIndexOf('/')+1);

		//update binding node
		if(bindNode != null && bindNode.getAttribute(XformConstants.ATTRIBUTE_NAME_ID) != null){
			bindNode.setAttribute(XformConstants.ATTRIBUTE_NAME_ID,id);
		}

		//update control node referencing the binding
		if(controlNode != null&& controlNode.getAttribute(XformConstants.ATTRIBUTE_NAME_BIND) != null)
			controlNode.setAttribute(XformConstants.ATTRIBUTE_NAME_BIND,id);
		else if(controlNode != null && controlNode.getAttribute(XformConstants.ATTRIBUTE_NAME_REF) != null){
			/*String ref = controlNode.getAttribute(EpihandyXform.ATTRIBUTE_NAME_REF);
			if(!ref.contains("/"))
				controlNode.setAttribute(EpihandyXform.ATTRIBUTE_NAME_REF,variableName);
			else
				ref = ref.substring(0,ref.indexOf('/')) + variableName;*/
			controlNode.setAttribute(XformConstants.ATTRIBUTE_NAME_REF,id);
		}

		if(dataType == QuestionDef.QTN_TYPE_REPEAT)
			getRepeatQtnsDef().updateDataNodes(dataNode);

		formDef.updateRuleConditionValue(rootDataNodeName+"/"+name, parent.getQuestionID()+"/"+questionID);
	}


	/**
	 * Checks if the xforms ui node name of this question requires to
	 * be changed and does so, if it needs to be changed.
	 */
	private void updateControlNode(){
		//TODO How about cases where the prefix is not xf?
		
		//Remove the control node from the XForms doc if hasUINode == false
		if(!hasUINode()){
			Element parentCtr = this.getParent().getControlNode();
			if(parentCtr == null){ return ; } //well...shit.
			
			Element ctrl = FormUtil.getControlNodeByQuestionID(this.getQuestionID(), this.getParent().getControlNode());
			if(ctrl != null){
				ctrl.getParentNode().removeChild(ctrl);
				this.controlNode = null;
			}
			
			return; //short circuit the below since we've presumably removed the control node, for real.
		}
		if(controlNode == null && hasUINode()){
			Element parentControlNode = this.getParent().getControlNode();
			Element controlNode = UiElementBuilder.buildXformUIElement(this.getFormDef().getDoc(),this,false);
			NodeList childDOMNodes = parentControlNode.getChildNodes();
			XformBuilderUtil.insertNodeAtIndex(parentControlNode,childDOMNodes,this.getParent().getChildren().indexOf(this),controlNode);
			setControlNode(controlNode);
			
			Element labelNode =  controlNode.getOwnerDocument().createElement(XformConstants.NODE_NAME_LABEL);
			XmlUtil.setTextNodeValue(labelNode,this.getText());
			UiElementBuilder.addItextRefs(labelNode, this);
			controlNode.appendChild(labelNode);
			setLabelNode(labelNode);
		}
		String name = controlNode.getNodeName();
		Element parent = (Element)controlNode.getParentNode();
		String xml = controlNode.toString();
		boolean modified = false;
		
		if((name.contains(XformConstants.NODE_NAME_INPUT_MINUS_PREFIX) || 
				name.contains(XformConstants.NODE_NAME_UPLOAD_MINUS_PREFIX) ||
				name.contains(XformConstants.NODE_NAME_TRIGGER_MINUS_PREFIX) ||
				name.contains(XformConstants.NODE_NAME_GROUP_MINUS_PREFIX)) &&
				dataType == QuestionDef.QTN_TYPE_LIST_MULTIPLE){
			xml = xml.replace(name, XformConstants.NODE_NAME_SELECT);
			modified = true;
		}
		else if((name.contains(XformConstants.NODE_NAME_INPUT_MINUS_PREFIX) || 
				name.contains(XformConstants.NODE_NAME_UPLOAD_MINUS_PREFIX) ||
				name.contains(XformConstants.NODE_NAME_TRIGGER_MINUS_PREFIX) ||
				name.contains(XformConstants.NODE_NAME_GROUP_MINUS_PREFIX)) &&
				(dataType == QuestionDef.QTN_TYPE_LIST_EXCLUSIVE || dataType == QuestionDef.QTN_TYPE_LIST_EXCLUSIVE_DYNAMIC)){
			xml = xml.replace(name, XformConstants.NODE_NAME_SELECT1);
			modified = true;
		}
		else if(name.contains(XformConstants.NODE_NAME_SELECT1_MINUS_PREFIX) &&
				dataType == QuestionDef.QTN_TYPE_LIST_MULTIPLE){
			xml = xml.replace(name, XformConstants.NODE_NAME_SELECT);
			modified = true;
		}
		else if((name.contains(XformConstants.NODE_NAME_SELECT_MINUS_PREFIX) &&
				!name.contains(XformConstants.NODE_NAME_SELECT1_MINUS_PREFIX)) && 
				(dataType == QuestionDef.QTN_TYPE_LIST_EXCLUSIVE || dataType == QuestionDef.QTN_TYPE_LIST_EXCLUSIVE_DYNAMIC)){
			xml = xml.replace(name, XformConstants.NODE_NAME_SELECT1);
			modified = true;
		}
		else if((name.contains(XformConstants.NODE_NAME_SELECT1_MINUS_PREFIX) || 
				name.contains(XformConstants.NODE_NAME_SELECT_MINUS_PREFIX)) &&
				!(dataType == QuestionDef.QTN_TYPE_LIST_EXCLUSIVE ||
						dataType == QuestionDef.QTN_TYPE_LIST_MULTIPLE ||
						dataType == QuestionDef.QTN_TYPE_LIST_EXCLUSIVE_DYNAMIC ||
						isMultiMedia(dataType) ||
						dataType == QuestionDef.QTN_TYPE_LABEL)){
			xml = xml.replace(name, XformConstants.NODE_NAME_INPUT);
			modified = true;
		}
		else if(!(name.contains(XformConstants.NODE_NAME_UPLOAD_MINUS_PREFIX)) &&
				(dataType == QuestionDef.QTN_TYPE_IMAGE || dataType == QuestionDef.QTN_TYPE_AUDIO ||
						dataType == QuestionDef.QTN_TYPE_VIDEO)){
			xml = xml.replace(name, XformConstants.NODE_NAME_UPLOAD);
			modified = true;
		}
		else if(!(name.contains(XformConstants.NODE_NAME_TRIGGER_MINUS_PREFIX)) &&
				(dataType == QuestionDef.QTN_TYPE_LABEL)){
			xml = xml.replace(name, XformConstants.NODE_NAME_TRIGGER);
			modified = true;
		}
		else if( (name.contains(XformConstants.NODE_NAME_UPLOAD_MINUS_PREFIX) && !isMultiMedia(dataType)) ||
				(name.contains(XformConstants.NODE_NAME_TRIGGER_MINUS_PREFIX) && dataType != QuestionDef.QTN_TYPE_LABEL) ||
				name.contains(XformConstants.NODE_NAME_GROUP_MINUS_PREFIX) ){
			xml = xml.replace(name, XformConstants.NODE_NAME_INPUT);
			modified = true;
		}

		if(modified){
			Element child = XformUtil.getNode(xml);
			child = (Element)controlNode.getOwnerDocument().importNode(child, true);
			parent.replaceChild(child, controlNode);
			controlNode =  child;
			updateControlNodeChildren();
		}
		
		
		controlNode.removeAttribute("bind");
		if(this.getDataType() == QuestionDef.QTN_TYPE_REPEAT){
			controlNode.setAttribute("nodeset", this.getDataNodesetPath());
		}else{
			controlNode.setAttribute("ref", this.getDataNodesetPath());
		}
	}
	
	private boolean isMultiMedia(int dataType){
		return dataType == QuestionDef.QTN_TYPE_IMAGE || dataType == QuestionDef.QTN_TYPE_AUDIO ||
				dataType == QuestionDef.QTN_TYPE_VIDEO;
	}


	/**
	 * Updates xforms ui nodes of the child nodes when the name of xforms ui 
	 * node of this question has changed. Eg when changes from select to select1.
	 */
	private void updateControlNodeChildren(){
		NodeList list = controlNode.getElementsByTagName(XformConstants.NODE_NAME_LABEL_MINUS_PREFIX);
		if(list.getLength() > 0)
			labelNode = (Element)list.item(0);

		list = controlNode.getElementsByTagName(XformConstants.NODE_NAME_HINT_MINUS_PREFIX);
		if(list.getLength() > 0)
			hintNode = (Element)list.item(0);

		if(dataType == QuestionDef.QTN_TYPE_LIST_EXCLUSIVE ||
				dataType == QuestionDef.QTN_TYPE_LIST_MULTIPLE){
			if(options != null){
				List optns = (List)options;
				for(int i=0; i<optns.size(); i++){
					OptionDef optionDef = (OptionDef)optns.get(i);
					updateOptionNodeChildren(optionDef);
					if(i == 0)
						firstOptionNode = optionDef.getControlNode();
				}
			}
		}
		else if(dataType == QuestionDef.QTN_TYPE_LIST_EXCLUSIVE_DYNAMIC){
			list = controlNode.getElementsByTagName(XformConstants.NODE_NAME_ITEMSET_MINUS_PREFIX);
			if(list.getLength() > 0)
				firstOptionNode = (Element)list.item(0);
		}
	}

	/**
	 * Updates xforms ui nodes of an option definition object when the name of
	 * xforms ui node of this question has changed.
	 */
	private void updateOptionNodeChildren(OptionDef optionDef){
		int count = controlNode.getChildNodes().getLength();
		for(int i=0; i<count; i++){
			Node node = controlNode.getChildNodes().item(i);
			if(node.getNodeType() != Node.ELEMENT_NODE)
				continue;
			if(node.getNodeName().equals(XformConstants.NODE_NAME_ITEM)){
				NodeList list = ((Element)node).getElementsByTagName(XformConstants.NODE_NAME_LABEL_MINUS_PREFIX);
				if(list.getLength() == 0)
					continue;

				if(optionDef.getText().equals(XmlUtil.getTextValue((Element)list.item(0)))){
					optionDef.setLabelNode((Element)list.item(0));
					optionDef.setControlNode((Element)node);

					list = ((Element)node).getElementsByTagName(XformConstants.NODE_NAME_VALUE_MINUS_PREFIX);
					if(list.getLength() > 0)
						optionDef.setValueNode((Element)list.item(0));
					return;
				}
			}
		}
	}

	/**
	 * Gets the option with a given display text.
	 * 
	 * @param text the option text.
	 * @return the option definition object.
	 */
	public OptionDef getOptionWithText(String text){
		if(options == null || text == null)
			return null;

		List list = (List)options;
		for(int i=0; i<list.size(); i++){
			OptionDef optionDef = (OptionDef)list.get(i);
			if(optionDef.getText().equals(text))
				return optionDef;
		}
		return null;
	}

	/**
	 * Gets the option with a given id.
	 * 
	 * @param id the option id
	 * @return the option definition object.
	 */
	public OptionDef getOption(int id){
		if(options == null)
			return null;

		List list = (List)options;
		for(int i=0; i<list.size(); i++){
			OptionDef optionDef = (OptionDef)list.get(i);
			if(optionDef.getId() == id)
				return optionDef;
		}
		return null;
	}

	/**
	 * Gets the option with a given variable name or binding.
	 * 
	 * @param value the variable name or binding.
	 * @return the option definition object.
	 */
	public OptionDef getOptionWithValue(String value){
		if(options == null || value == null)
			return null;

		List list = (List)options;
		for(int i=0; i<list.size(); i++){
			OptionDef optionDef = (OptionDef)list.get(i);
			if(optionDef.getQuestionID().equals(value))
				return optionDef;
		}
		return null;
	}

	/**
	 * Updates this questionDef (as the main) with the parameter one (which is the old)
	 * 
	 * @param questionDef the old question before the refresh
	 */
	public void refresh(QuestionDef questionDef){
		setText(questionDef.getText());
		setHelpText(questionDef.getHelpText());
		setDefaultValue(questionDef.getDefaultValue());

		int prevDataType = dataType;

		//The old data type can only overwrite the new one if its not text (The new one is this question)
		if(questionDef.getDataType() != QuestionDef.QTN_TYPE_TEXT)
			setDataType(questionDef.getDataType());

		setEnabled(questionDef.isEnabled());
		setRequired(questionDef.isRequired());
		setLocked(questionDef.isLocked());
		setVisible(questionDef.isVisible());

		if((dataType == QuestionDef.QTN_TYPE_LIST_EXCLUSIVE || dataType == QuestionDef.QTN_TYPE_LIST_MULTIPLE) &&
				(questionDef.getDataType() == QuestionDef.QTN_TYPE_LIST_EXCLUSIVE || questionDef.getDataType() == QuestionDef.QTN_TYPE_LIST_MULTIPLE) ){
			refreshOptions(questionDef);

			if(!(prevDataType == QuestionDef.QTN_TYPE_LIST_EXCLUSIVE || prevDataType == QuestionDef.QTN_TYPE_LIST_MULTIPLE)){
				//A single or multiple select may have had options added on the client and so we do wanna
				//lose them for instance when the server has text data type.
				//TODO We may need to assign new option ids
				for(int index = 0; index < questionDef.getOptionCount(); index++)
					addOption(new OptionDef(questionDef.getOptionAt(index),this));
			}

		}
		else if(dataType == QuestionDef.QTN_TYPE_REPEAT && questionDef.getDataType() == QuestionDef.QTN_TYPE_REPEAT)
			getRepeatQtnsDef().refresh(questionDef.getRepeatQtnsDef()); //TODO Finish this
	}


	public int getOptionIndex(String varName){
		if(options == null)
			return -1;

		for(int i=0; i<getOptions().size(); i++){
			OptionDef def = (OptionDef)getOptions().get(i);
			if(def.getQuestionID().equals(varName))
				return i;
		}

		return -1;
	}

	private void refreshOptions(QuestionDef questionDef){
		List options2 = questionDef.getOptions();
		if(options == null || options2 == null)
			return;

		Vector<OptionDef> orderedOptns = new Vector<OptionDef>();
		Vector<OptionDef> missingOptns = new Vector<OptionDef>();

		for(int index = 0; index < options2.size(); index++){
			OptionDef optn = (OptionDef)options2.get(index);
			OptionDef optionDef = this.getOptionWithValue(optn.getQuestionID());
			if(optionDef == null){
				missingOptns.add(optn);
				continue;
			}
			
			optionDef.setText(optn.getText());

			orderedOptns.add(optionDef); //add the option in the order it was before the refresh.

			/*int index1 = this.getOptionIndex(optn.getVariableName());
			if(index != index1 && index1 != -1 && index < this.getOptionCount() - 1){
				((List)this.getOptions()).remove(optionDef);
				((List)this.getOptions()).set(index, optionDef);
			}*/
		}

		int oldCount = questionDef.getOptionCount();
		
		//now add the new questions which have just been added by refresh.
		int count = getOptionCount();
		for(int index = 0; index < count; index++){
			OptionDef optionDef = getOptionAt(index);
			if(questionDef.getOptionWithValue(optionDef.getQuestionID()) == null){
				
				//TODO Make sure this is not buggy.
				//If before refresh number of options is the same as the new number,
				//then we preserve the old option text and binding by replacing new
				//ones with the old values.
				if(oldCount == count){
					OptionDef optnDef = questionDef.getOptionAt(index);
					optionDef.setQuestionID(optnDef.getQuestionID());
					optionDef.setText(optnDef.getText());
				}
				
				orderedOptns.add(optionDef);
			}
		}
		
		//Now add the missing options. Possibly they were added by user and not existing in the
		//original server side form.
		for(int index = 0; index < missingOptns.size(); index++){
			OptionDef optnDef = missingOptns.get(index);
			orderedOptns.add(new OptionDef((orderedOptns.size() + index + 1), optnDef.getText(), optnDef.getQuestionID(), this));
		}

		options = orderedOptns;
	}

	/**
	 * Gets the number of options for this questions.
	 * 
	 * @return the number of options.
	 */
	public int getOptionCount(){
		if(options == null)
			return 0;
		return ((List)options).size();
	}

	/**
	 * Gets the option at a given position (zero based).
	 * 
	 * @param index the position.
	 * @return the option definition object.
	 */
	public OptionDef getOptionAt(int index){
		return (OptionDef)((List)options).get(index);
	}

	/**
	 * Clears the list of option for a question.
	 */
	public void clearOptions(){
		if(options != null)
			((List)options).clear();
	}

	public void moveOptionNodesUp(OptionDef optionDef, OptionDef refOptionDef){
		Element controlNode = optionDef.getControlNode();
		Element parentNode = controlNode != null ? (Element)controlNode.getParentNode() : null;

		if(controlNode != null)
			parentNode.removeChild(controlNode);

		if(refOptionDef.getControlNode() != null)
			parentNode.insertBefore(controlNode, refOptionDef.getControlNode());
	}

	/**
	 * Sets the list of options for a question.
	 * 
	 * @param optionList the option list.
	 */
	public void setOptionList(List<OptionDef> optionList){
		options = optionList;

		for(int index = 0; index < changeListeners.size(); index++)
			changeListeners.get(index).onOptionsChanged(this,optionList);
	}


	/**
	 * Updates the xforms instance data nodes referenced by this question and its children.
	 * 
	 * @param parentDataNode the parent data node for this question.
	 */
	public void updateDataNodes(Element parentDataNode){
		if(dataNode == null)
			return;

		String xpath = /*"/"+formDef.getVariableName()+"/"+*/dataNode.getNodeName();
		XPathExpression xpls = new XPathExpression(parentDataNode, xpath);
		Vector result = xpls.getResult();
		if(result == null || result.size() == 0)
			return;

		dataNode = (Element)result.elementAt(0);

		if(dataType == QuestionDef.QTN_TYPE_REPEAT)
			getRepeatQtnsDef().updateDataNodes(dataNode);
	}

	/**
	 * Builds the locale xpath xpressions and their text  values for this question.
	 * 
	 * @param parentXpath the parent xpath expression we are building onto.
	 * @param doc the locale document that we are building.
	 * @param parentXformNode the parent xforms node for this question.
	 * @param parentLangNode the parent language node we are building onto.
	 */
	public void buildLanguageNodes(String parentXpath, com.google.gwt.xml.client.Document doc, Element parentXformNode, Element parentLangNode){
		if(controlNode == null)
			return;

		String xpath = parentXpath + "/" + FormUtil.getNodePath(controlNode,parentXformNode);

		if(dataType == QuestionDef.QTN_TYPE_REPEAT){
			Element parent = (Element)controlNode.getParentNode();
			xpath = parentXpath + FormUtil.getNodePath(parent,parentXformNode);

			String id = parent.getAttribute(XformConstants.ATTRIBUTE_NAME_ID);
			if(id != null && id.trim().length() > 0)
				xpath += "[@" + XformConstants.ATTRIBUTE_NAME_ID + "='" + id + "']";
		}
		else{
			String id = controlNode.getAttribute(XformConstants.ATTRIBUTE_NAME_BIND);
			if(id != null && id.trim().length() > 0)
				xpath += "[@" + XformConstants.ATTRIBUTE_NAME_BIND + "='" + id + "']";
			else{
				id = controlNode.getAttribute(XformConstants.ATTRIBUTE_NAME_REF);
				if(id != null && id.trim().length() > 0)
					xpath += "[@" + XformConstants.ATTRIBUTE_NAME_REF + "='" + id + "']";
			}
		}

		if(labelNode != null){
			Element node = doc.createElement(XformConstants.NODE_NAME_TEXT);
			node.setAttribute(XformConstants.ATTRIBUTE_NAME_XPATH, xpath + "/" + FormUtil.getNodeName(labelNode));
			node.setAttribute(XformConstants.ATTRIBUTE_NAME_VALUE, text);
			parentLangNode.appendChild(node);
			node.setAttribute(XformConstants.ATTRIBUTE_NAME_ID, itextId);
			node.setAttribute(OpenRosaConstants.ATTRIBUTE_NAME_UNIQUE_ID, "QuestionDef-"+id);
		}

		if(hintNode != null){
			Element node = doc.createElement(XformConstants.NODE_NAME_TEXT);
			node.setAttribute(XformConstants.ATTRIBUTE_NAME_XPATH, xpath + "/" + FormUtil.getNodeName(hintNode));
			node.setAttribute(XformConstants.ATTRIBUTE_NAME_VALUE, helpText);
			parentLangNode.appendChild(node);
			
			String id = XmlUtil.getItextId(hintNode);
			if(id == null)
				id =  itextId + "-hint";
			node.setAttribute(XformConstants.ATTRIBUTE_NAME_ID, id);
			node.setAttribute(OpenRosaConstants.ATTRIBUTE_NAME_UNIQUE_ID, "QuestionDefHint-"+id);
		}

		if(dataType == QuestionDef.QTN_TYPE_REPEAT){

		}
		if(dataType == QuestionDef.QTN_TYPE_LIST_EXCLUSIVE || dataType == QuestionDef.QTN_TYPE_LIST_MULTIPLE){
			if(options != null){
				List optionsList = (List)options;
				for(int index = 0; index < optionsList.size(); index++)
					((OptionDef)optionsList.get(index)).buildLanguageNodes(xpath, doc, parentLangNode);
			}
		}
	}

	/**
	 * Gets the form to which this question belongs.
	 * 
	 * @return the form.
	 */
	public FormDef getParentFormDef(){
		return (FormDef)getParentFormDef(this);
	}

	private IFormElement getParentFormDef(IFormElement questionDef){
		IFormElement parent = questionDef.getParent();
		if(parent instanceof FormDef)
			return parent;
		return getParentFormDef(parent);
	}

	public String getDisplayText(){
		String displayText = getText();
		if(displayText != null && !displayText.isEmpty()){
			int pos1 = displayText.indexOf("${");
			int pos2 = displayText.indexOf("}$");
			if(pos1 > -1 && pos2 > -1 && (pos2 > pos1))
				displayText = displayText.replace(displayText.substring(pos1,pos2+2),"");
		}
		return displayText;
	}

	public void setQuestionID(String binding){
		setVariableName(binding);
	}

	public List<IFormElement> getChildren(){
		if(dataType == QuestionDef.QTN_TYPE_REPEAT) {
			return this.getRepeatQtnsDef().getChildren();
		}else if(dataType == QuestionDef.QTN_TYPE_LIST_EXCLUSIVE || dataType == QuestionDef.QTN_TYPE_LIST_MULTIPLE) {
			return (List<IFormElement>)options;
		}else{
			return null;
		}
	}

	public void setChildren(List<IFormElement> children){
		this.options = children;
	}
	
	public void refresh(IFormElement element){
		
	}
	
	public void updateDoc(Document doc, Element xformsNode, FormDef formDef, Element formNode, Element modelNode, boolean withData, String orgFormVarName){
		GWT.log("ENTERED INTO UNIMPLEMENTED AREA! THIS SHOULD NOT HAVE HAPPENED. QuestionDef.updateDoc(the, empty, one)");
		throw new RuntimeException("Code entered into a bad state. See log. QuestionDef.java");
	}
	
	public IFormElement copy(IFormElement parent){
		return new QuestionDef(this, parent);
	}
	
	public void addChild(IFormElement element){
		if(dataType == QuestionDef.QTN_TYPE_LIST_EXCLUSIVE || dataType == QuestionDef.QTN_TYPE_LIST_MULTIPLE) {
			addOption((OptionDef)element);
		}else if(dataType == QuestionDef.QTN_TYPE_REPEAT){
			this.getRepeatQtnsDef().addChild(element);
		}else{
			throw new RuntimeException("Cannot 'addChild' with this Question Type!");
		}
	}
	
	public boolean removeChild(IFormElement element){
		if(dataType == QuestionDef.QTN_TYPE_REPEAT){
			return this.getRepeatQtnsDef().removeChild(element);
		}else if(dataType == QuestionDef.QTN_TYPE_LIST_EXCLUSIVE || dataType == QuestionDef.QTN_TYPE_LIST_MULTIPLE) {
			return this.removeOption((OptionDef)element);
		}else{
			throw new RuntimeException("Cannot 'removeChild' from this Question Type!");
		}
	}
	
	public int getChildCount(){
		return getOptionCount();
	}
	

	public FormDef getFormDef(){
		IFormElement element = getParent();
		if(parent instanceof FormDef)
			return (FormDef)element;
		
		return element.getFormDef();
	}

	public boolean hasUINode() {
		return hasUINode;
	}

	public void setHasUINode(boolean hasUINode) {
		this.hasUINode = hasUINode;
	}
	
	public void moveChildToIndex(IFormElement child, int index) throws Exception{
		throw new Exception("QuestionDefs should not have children, therefore cannot move them!");
		
	}
	
	public boolean hasAdvancedCalculate() {
		return hasAdvancedCalculate;
	}

	public boolean hasAdvancedConstraint() {
		return hasAdvancedConstraint;
	}

	public boolean hasAdvancedRelevant() {
		return hasAdvancedRelevant;
	}

	public void setHasAdvancedCalculate(boolean enabled) {
		hasAdvancedCalculate = enabled;
	}

	public void setHasAdvancedConstraint(boolean enabled) {
		hasAdvancedConstraint = enabled;
	}

	public void setHasAdvancedRelevant(boolean enabled) {
		hasAdvancedRelevant = enabled;
	}

	public String getAdvancedCalculate() {
		return advancedCalculate;
	}

	public String getAdvancedConstraint() {
		return advancedConstraint;
	}

	public String getAdvancedRelevant() {
		return advancedRelevant;
	}

	public void setAdvancedCalculate(String calcValue) {
		advancedCalculate = calcValue;
	}

	public void setAdvancedConstraint(String constValue) {
		advancedConstraint = constValue;
	}

	public void setAdvancedRelevant(String releValue) {
		advancedRelevant = releValue;
	}
	
	public void setRepeatCountNodePath(String nodePath){
		this.repeatCountNodePath = nodePath;
	}
	
	public String getRepeatCountNodePath(){
		return repeatCountNodePath;
	}
	
	
	public boolean insertChildAfter(IFormElement child, IFormElement target) {
		if(child == null || target == null){ return false; } //aah...defensive programming.
		
		boolean questionIsSelect = getDataType() == QuestionDef.QTN_TYPE_LIST_EXCLUSIVE || 
								   getDataType() == QuestionDef.QTN_TYPE_LIST_MULTIPLE ||
								   getDataType() == QuestionDef.QTN_TYPE_LIST_EXCLUSIVE_DYNAMIC;
		boolean childIsOptionDef = child instanceof OptionDef;
		boolean questionIsRepeat = getDataType() == QuestionDef.QTN_TYPE_REPEAT;
		boolean childIsQtnOrGrpDef = child instanceof GroupDef || child instanceof QuestionDef;
		
		if(questionIsSelect && childIsOptionDef){
			return FormDef.insertChildBeforeOrAfter(child, target, getOptions(), FormDef.INSERT_AFTER);
		}else if (questionIsRepeat && childIsQtnOrGrpDef){
			return FormDef.insertChildBeforeOrAfter(child, target, getRepeatQtnsDef().getChildren(), FormDef.INSERT_AFTER);
		}else{
			//unsupported op
			return false;
		}
	}
	
	public boolean insertChildBefore(IFormElement child, IFormElement target) {
		if(child == null || target == null){ return false; }
		
		boolean questionIsSelect = getDataType() == QuestionDef.QTN_TYPE_LIST_EXCLUSIVE || 
								   getDataType() == QuestionDef.QTN_TYPE_LIST_MULTIPLE ||
								   getDataType() == QuestionDef.QTN_TYPE_LIST_EXCLUSIVE_DYNAMIC;
		boolean childIsOptionDef = child instanceof OptionDef;
		boolean questionIsRepeat = getDataType() == QuestionDef.QTN_TYPE_REPEAT;
		boolean childIsQtnOrGrpDef = child instanceof GroupDef || child instanceof QuestionDef;
		
		if(questionIsSelect && childIsOptionDef){
			return FormDef.insertChildBeforeOrAfter(child, target, getOptions(), FormDef.INSERT_BEFORE);
		}else if (questionIsRepeat && childIsQtnOrGrpDef){
			return FormDef.insertChildBeforeOrAfter(child, target, getRepeatQtnsDef().getChildren(), FormDef.INSERT_BEFORE);
		}else{
			//unsupported op
			return false;
		}
	}

}

