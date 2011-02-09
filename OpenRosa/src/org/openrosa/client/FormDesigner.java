package org.openrosa.client;

import java.util.ArrayList;
import java.util.List;

import org.openrosa.client.view.FormDesignerWidget;
import org.purc.purcforms.client.PurcConstants;
import org.purc.purcforms.client.locale.LocaleText;
import org.purc.purcforms.client.model.Locale;
import org.purc.purcforms.client.util.FormDesignerUtil;
import org.purc.purcforms.client.util.FormUtil;
import org.purc.purcforms.client.xforms.XformConstants;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.RootPanel;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class FormDesigner implements EntryPoint ,ResizeHandler{

	/**
	 * Reference to the form designer widget.
	 */
	private FormDesignerWidget designer;
	
	/**
	 * URL used for retreving the xforms from the XEP Server.
	 * REMEMBER TO INCLUDE THE XEP SESSION TOKEN AFTER THIS POSTFIX!
	 */
	public static final String XEP_GET_FORM_URL = "/xep/xform/";
	
	/**
	 * URL used for sending an xform TO the XEP server.
	 */
	public static final String XEP_POST_FORM_URL = "/xep/save/";
	
	public static boolean showClosingMessage = true;

	public static native void alert(String msg)
	/*-{
	 $wnd.alert(msg);
	}-*/;
	
	public static String status;
	public static String token;
	/**
	 * This is the GWT entry point method.
	 */
	public void onModuleLoad() {
		
		FormDesigner.token = com.google.gwt.user.client.Window.Location.getParameter("token");
		FormDesigner.status = com.google.gwt.user.client.Window.Location.getParameter("status");
		FormUtil.setupUncaughtExceptionHandler();
		
		FormUtil.dlg.setText(LocaleText.get("loading"));
		FormUtil.dlg.center();
		
		publishJS();
		
		Window.addWindowClosingHandler(new Window.ClosingHandler() {
		      public void onWindowClosing(Window.ClosingEvent closingEvent) {
		    	  if(showClosingMessage){
			        closingEvent.setMessage("Do you really want to leave the page?  Your changes will be lost if you have not saved.");
			      }
		      }  
		    });
		DeferredCommand.addCommand(new Command() {
			public void execute() {
				onModuleLoadDeffered();
			}
		});		
	}
	public static void setShowClosingWindowMessage(boolean showMsg){
		showClosingMessage = showMsg;
	}
	
	/**
	 * Sets up the form designer.
	 */
	public void onModuleLoadDeffered() {

		try{
			RootPanel rootPanel = RootPanel.get("openrosaformdesigner");
			if(rootPanel == null){
				FormUtil.dlg.hide();
				return;
			}
			FormUtil.setupUncaughtExceptionHandler();

			FormDesignerUtil.setDesignerTitle();

			String s = FormUtil.getDivValue("allowBindEdit");
			if(s != null && s.equals("0"))
				Context.setAllowBindEdit(false);

			FormUtil.retrieveUserDivParameters();
			
			Context.setOfflineModeStatus();

			// Get rid of scrollbars, and clear out the window's built-in margin,
			// because we want to take advantage of the entire client area.
			Window.enableScrolling(false);
			Window.setMargin("0"+PurcConstants.UNITS);

			// Different themes use different background colors for the body
			// element, but IE only changes the background of the visible content
			// on the page instead of changing the background color of the entire
			// page. By changing the display style on the body element, we force
			// IE to redraw the background correctly.
			RootPanel.getBodyElement().getStyle().setProperty("display", "none");
			RootPanel.getBodyElement().getStyle().setProperty("display", "");

			loadLocales();
			
			//replace constraint message with JR custom name.
			XformConstants.ATTRIBUTE_NAME_CONSTRAINT_MESSAGE = "jr:constraintMsg";
			
			//This is required by ODK
			XformConstants.ATTRIBUTE_NAME_FORM_KEY = "id";
			
			//JR does not use base64 binary.
			XformConstants.DATA_TYPE_BINARY = "binary";
			
			designer = new FormDesignerWidget(true,true);
			
			// Finally, add the designer widget to the RootPanel, so that it will be displayed.
			rootPanel.add(designer);
			
			//updateTabs();
			
			//If a form id has been specified in the html host page, load the form
			//with that id in the designer.
			/*s = FormUtil.getFormId();
			if(s != null)
				designer.loadForm(Integer.parseInt(s));*/
			

			// Call the window resized handler to get the initial sizes setup. Doing
			// this in a deferred command causes it to occur after all widgets' sizes
			// have been computed by the browser.
			DeferredCommand.addCommand(new Command() {
				public void execute() {
					designer.onWindowResized(Window.getClientWidth(), Window.getClientHeight());
					
					String id = FormUtil.getFormId();
					if(id == null || id.equals("-1"))
						FormUtil.dlg.hide();
					
					
					
					if(FormDesigner.token != null){
//						Window.alert("showing dlg?");
						FormUtil.dlg.setText("Opening Form, Please Wait...");
						FormUtil.dlg.show();
						String xml = designer.getExternalForm();
					}
					
					
				}
			});
			
			// Hook the window resize event, so that we can adjust the UI.
			Window.addResizeHandler(this);
		}
		catch(Exception ex){
			FormUtil.dlg.hide();
			FormUtil.displayException(ex);
		}
	}
	
	/*private void updateTabs(){
		String s = FormUtil.getDivValue("showXformsSourceTab");
		if(!("1".equals(s) || "true".equals(s)))
			designer.removeXformSourceTab();
		
		s = FormUtil.getDivValue("showLayoutXmlTab");
		if(!("1".equals(s) || "true".equals(s)))
			designer.removeLayoutXmlTab();
		
		s = FormUtil.getDivValue("showLanguageTab");
		if(!("1".equals(s) || "true".equals(s)))
			designer.removeLanguageTab();
		
		s = FormUtil.getDivValue("showModelXmlTab");
		if(!("1".equals(s) || "true".equals(s)))
			designer.removeModelXmlTab();
		
		s = FormUtil.getDivValue("showJavaScriptTab");
		if(!("1".equals(s) || "true".equals(s)))
			designer.removeJavaScriptTab();
	}*/
	
	public void onResize(ResizeEvent event){
		designer.onWindowResized(event.getWidth(), event.getHeight());
	}
	
	// Set up the JS-callable signature as a global JS function.
	private native void publishJS() /*-{
   		$wnd.authenticationCallback = @org.openrosa.client.view.CenterWidget::authenticationCallback(Z);
	}-*/;
	
	
	/**
	 * Loads a list of locales supported by the form designer.
	 */
	private void loadLocales(){
		String localesList = FormUtil.getDivValue("localeList");
		
		if(localesList == null || localesList.trim().length() == 0)
			return;
		
		String[] tokens = localesList.split(",");
		if(tokens == null || tokens.length == 0)
			return;
		
		List<Locale> locales = new ArrayList<Locale>();
		
		for(String token: tokens){
			int index = token.indexOf(':');
			
			//Should at least have one character for key or name
			if(index < 1 || index == token.length() - 1)
				continue;
			
			locales.add(new Locale(token.substring(0,index).trim(),token.substring(index+1).trim()));
		}
		
		Context.setLocales(locales);
	}
}