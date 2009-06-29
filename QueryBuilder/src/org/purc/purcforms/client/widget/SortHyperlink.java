package org.purc.purcforms.client.widget;

import org.purc.purcforms.client.controller.ItemSelectionListener;
import org.purc.purcforms.client.model.QueryBuilderConstants;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.PopupPanel;

/**
 * 
 * @author daniel
 *
 */
public class SortHyperlink extends Hyperlink implements ItemSelectionListener{

	public static final String SORT_TEXT_ASCENDING = "Ascending"; //LocaleText.get("???");
	public static final String SORT_TEXT_DESCENDING = "Descending"; //LocaleText.get("???");
	public static final String SORT_TEXT_NOT_SORTED = "Not Sorted"; //LocaleText.get("???");
	
	private PopupPanel popup;
	private ItemSelectionListener itemSelectionListener;
	
	
	public SortHyperlink(String text, String targetHistoryToken,ItemSelectionListener itemSelectionListener){
		super(text,targetHistoryToken);
		this.itemSelectionListener = itemSelectionListener;
		DOM.sinkEvents(getElement(), DOM.getEventsSunk(getElement()) | Event.ONMOUSEDOWN );
	}
	
	public void onBrowserEvent(Event event) {
		if (DOM.eventGetType(event) == Event.ONMOUSEDOWN) {
			itemSelectionListener.onStartItemSelection(this);
			setupPopup();
			popup.setPopupPosition(event.getClientX(), event.getClientY());
			popup.show();
		}
	}
	
	private void setupPopup(){
		popup = new PopupPanel(true,true);

		MenuBar menuBar = new MenuBar(true);
		menuBar.addItem(SORT_TEXT_NOT_SORTED,true, new SelectItemCommand(SORT_TEXT_NOT_SORTED,this));
		menuBar.addItem(SORT_TEXT_ASCENDING,true, new SelectItemCommand(SORT_TEXT_ASCENDING,this));
		menuBar.addItem(SORT_TEXT_DESCENDING,true, new SelectItemCommand(SORT_TEXT_DESCENDING,this));
		popup.setWidget(menuBar);
	}
	
	public void onItemSelected(Object sender, Object item) {
		if(sender instanceof SelectItemCommand){
			popup.hide();
			setText((String)item);
			itemSelectionListener.onItemSelected(this, fromSortText2Value((String)item));
		}
	}
	
	public void onStartItemSelection(Object sender){

	}
	
	private int fromSortText2Value(String text){
		if(text.equals(SORT_TEXT_ASCENDING))
			return QueryBuilderConstants.SORT_ASCENDING;
		else if(text.equals(SORT_TEXT_DESCENDING))
			return QueryBuilderConstants.SORT_DESCENDING;

		return QueryBuilderConstants.SORT_NULL;
	}
}
