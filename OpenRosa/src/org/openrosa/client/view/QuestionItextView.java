package org.openrosa.client.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.openrosa.client.Context;
import org.openrosa.client.model.IFormElement;
import org.openrosa.client.model.ItextModel;
import org.openrosa.client.model.QuestionDef;
import org.openrosa.client.util.Itext;
import org.openrosa.client.util.ItextLocale;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FocusListener;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.HasHorizontalAlignment.HorizontalAlignmentConstant;


public class QuestionItextView extends Composite {

	private FlexTable widgetTable = new FlexTable();
	private FlexTable layoutTable = new FlexTable();
	
//	private Button addLanguageButton;
	private Button addFormButton;
	
	private Label lblItextID;
	private TextBox txtItextID;
	
	/** Current ItextID that we're concerned about **/
	private IFormElement currentDef;
	private List<String> languages;
	private List<String> forms;
	private PropertiesView propertiesView;
	
	/**
	 * Gives the rownumber for the specified language/form row.
	 * Storage format:
	 * "language:form" -> int
	 * 
	 * form is optional, use "langauge" (without ":form" for pure version"
	 * int shows the row in the flextable where this is located
	 * 
	 * Should be useful if necessary to reference
	 * the widgets after they are set.
	 */
	private HashMap<String, Integer> rowLocations;


	public QuestionItextView(PropertiesView propertiesView){
		this.propertiesView = propertiesView;
		initButtonsAndLabels();
		
//		layoutTable.setWidget(0, 0, addLanguageButton);
		layoutTable.setWidget(0, 0, addFormButton);
		layoutTable.setWidget(0, 1, lblItextID);
		layoutTable.setWidget(0, 2, txtItextID);

		layoutTable.setWidget(1, 0, widgetTable);
		layoutTable.getFlexCellFormatter().setColSpan(1, 0, 2);

		
		rowLocations = new HashMap<String, Integer>();
		widgetTable.clear();
		languages = new ArrayList<String>();
		forms = new ArrayList<String>();
		
		//HIDE ITEXT ID TEXTBOX
		lblItextID.setVisible(false);
		txtItextID.setVisible(false);
		///////////////////////
		
		
		initWidget(layoutTable);
	}
	
	private void init(IFormElement def){
		rowLocations = new HashMap<String, Integer>();
		widgetTable.clear();
		languages = new ArrayList<String>();
		forms = new ArrayList<String>();
		txtItextID.setValue(def.getItextId());
		findAndAddAvailableLangs();
		findAndAddAvailableForms();
		refreshRows();
	}
	
	public void update(){
		propertiesView.update();
	}
	
	public void setItemID(IFormElement def){
		GWT.log("setting ItemID currentDef.getItextId()="+def.getItextId()+" QuestionItextView:90");
		if(def == null){
			currentDef = null;
			txtItextID.setText("");
			clearRows();
			return;
		}else if(def.getItextId() == null){
			def.setItextId(def.getQuestionID());
		}
		currentDef = def;
		init(def);
	}
	private void clearRows(){
		widgetTable.clear(); //clear the row and re-add everything so that widgets are added in the right order
		widgetTable.clear(true);
		clearTableOfSeperatorStyle();
		widgetTable.removeAllRows();
		rowLocations = new HashMap<String, Integer>();
	}
	private void refreshRows(){
		clearRows();
		   //assumes that all changes were handled correctly/saved by the textbox listeners.
		int rowIndex = 0;
		for(String lang:languages){
			addDefaultTextAndHintRows(lang,rowIndex);
			rowIndex+=2; //two rows were added above
			for (String f:forms){
				if(currentDef == null){ continue; }
				addRow(lang, f, rowIndex);
				rowIndex++;
			}
			
			addSeperator(rowIndex);
			rowIndex++;
		}
	}
	
	/**
	 * Adds the default (as in, no special text form)
	 * and the "hint" form rows.  
	 */
	private void addDefaultTextAndHintRows(String lang, int rowIndex){
		addRow(lang,null,rowIndex);
	}
	/**
	 * Adds a seperator in the widget table to help
	 * the user visually distinguis between languages.
	 * @param rowIndex
	 */
	private void addSeperator(int rowIndex){
		widgetTable.setText(rowIndex, 0, "");
//		widgetTable.setWidget(rowIndex, 0, new Label("---"));
		widgetTable.getFlexCellFormatter().setStyleName(rowIndex, 0, "itextTabSeperator");
		widgetTable.setWidget(rowIndex, 1, new Label(""));
		widgetTable.getFlexCellFormatter().setStyleName(rowIndex, 1, "itextTabSeperator");
	}
	
	
	private void clearTableOfSeperatorStyle(){
		for(int i=0;i<widgetTable.getRowCount();i++){
			for(int j=0;j<widgetTable.getCellCount(i);j++){
				widgetTable.getFlexCellFormatter().removeStyleName(i, j, "itextTabSeperator");
			}
		}
	}
	
	/**
	 * Finds all the available locales based on data from Itext class,
	 * adds it to this widget's concept of which locales are available for editing.
	 */
	private void findAndAddAvailableLangs(){
		List<ItextLocale> locales = Itext.locales;
		for (int i=0;i<locales.size();i++){
			languages.add(locales.get(i).getName());
			
		}
	}
	
	/**
	 * Finds all the available forms
	 * for the current ItemItextID
	 * and adds them to the pool of known TextForms.
	 */
	private void findAndAddAvailableForms(){
		for(ItextLocale locale: Itext.locales){
			mergeForms(locale.getAvailableForms(currentDef.getItextId()));
		}
	}
	
	/**
	 * Adds a new language row to this itext view.
	 * (creates a new locale in Itext.java if necessary)
	 * 
	 * loops through all the existing languages in this view
	 * and appends the form based rows to them (if any exist)
	 * @param name
	 */
	public void addLanguage(String language){
		if(currentDef.getItextId() == null)return;
		languages.add(language);
		refreshRows();
	}
	
	/**
	 * Adds a new "lang:form" row under each respective language row
	 * 
	 * ONLY CALL ME IF THERE IS ALREADY AT LEAST ONE LANGUAGE ROW
	 * @param form
	 */
	public void addForm(String form){
		if(currentDef.getItextId() == null)return;
		forms.add(form);
		refreshRows();
	}
	
	/**
	 * takes in an ArrayList of available forms (returned by ItextLocale) and
	 * merges it into this View's list of available forms
	 * @param newForms
	 */
	private void mergeForms(ArrayList<String> newForms){
		for(String f: newForms){
			if(forms.contains(f)) continue;
			else forms.add(f);
		}
	}
	
	private void addRow(String language, String Form, int rowNumber){
		GWT.log("adding textRow:QuestionItextView:207, currentDef.getItextId()="+currentDef.getItextId()+"; form="+Form);
		final String lang = language;
		final String form = Form; //this final business is to use the anon inner class below
		final TextBox textBoxWidget = new TextBox();
		

		String fullText = (form == null) ? lang : (lang + ";" + form);
		if(rowLocations.containsKey(fullText)) return; //don't want to duplicate rows.
		Label labelWidget = new Label(fullText);
		
		//set the default val for the textbox
		String itextValue = Itext.getLocale(language).getTranslation(((form == null) ? currentDef.getItextId() : (currentDef.getItextId() + ";" + form)));
		if(itextValue == null){
			itextValue = "";
		}
		textBoxWidget.setText(itextValue); 
		
		textBoxWidget.addKeyUpHandler(new KeyUpHandler() {
			
			@Override
			public void onKeyUp(KeyUpEvent event) {
				Itext.addText(lang, ((form == null) ? currentDef.getItextId() : (currentDef.getItextId() + ";" + form)), textBoxWidget.getText()); //handles creating new itext data
				update();
			}
		});
		
		textBoxWidget.addBlurHandler(new BlurHandler() {
			
			@Override
			public void onBlur(BlurEvent event) {
				Itext.addText(lang, ((form == null) ? currentDef.getItextId() : (currentDef.getItextId() + ";" + form)), textBoxWidget.getText()); //handles creating new itext data
				update();
				
			}
		});
		
		//populate the flex table
		widgetTable.setWidget(rowNumber, 0, labelWidget);
		widgetTable.setWidget(rowNumber, 1, textBoxWidget);

		
		//cosmetic things
		widgetTable.getFlexCellFormatter().removeStyleName(rowNumber, 0, "itextTabSeperator");
		widgetTable.getFlexCellFormatter().removeStyleName(rowNumber, 1, "itextTabSeperator");

		rowLocations.put(fullText, rowNumber); //in case we need to reference back at this.
	}
	
	/**
	 * Sets up the addLanguage and addForm buttons
	 * to have dialogue boxes pop up and all that.
	 */
	private void initButtonsAndLabels(){
		currentDef = null;
		addFormButton = new Button("Add a new TextForm");
		
		lblItextID = new Label("ItextID:");
		txtItextID = new TextBox();
		
		txtItextID.addKeyUpHandler(new KeyUpHandler() {
			public void onKeyUp(KeyUpEvent event) {
				updateItextID();
			}
		});
		
		txtItextID.addBlurHandler(new BlurHandler() {
			public void onBlur(BlurEvent event) {
				updateItextID();
			}
		});
		
		addFormButton.addClickHandler(new ClickHandler() {
			
			@Override
			public void onClick(ClickEvent event) {
				String newForm = Window.prompt("Please enter the name of the new TextForm you would like to edit", "long/short/etc");
				addForm(newForm);
			}
		});
	}
	
	private void updateItextID(){
		if(currentDef == null){
			return;
		}
		
		String newID = txtItextID.getText();
		String currentID = currentDef.getItextId();
		if(!newID.equals(currentID)){
			currentDef.setItextId(newID);
			Itext.renameID(currentID, newID);
		}
	}
	

}
