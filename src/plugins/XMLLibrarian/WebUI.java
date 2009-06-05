package plugins.XMLLibrarian;

import freenet.support.HTMLNode;
import freenet.support.HTMLEncoder;
import freenet.l10n.L10n;




public class WebUI{
	static String plugName;
	static XMLLibrarian xl;
	
	public static void setup(XMLLibrarian xl, String plugName){
		WebUI.plugName = plugName;
		WebUI.xl = xl;
	}


	/**
	 * Build an empty search page
	 **/
	public static String searchpage(){
		return searchpage(null, false, null);
	}
	
	/**
	 * Put an error on the page
	 */
	public static void addError(HTMLNode node, Exception error){
		HTMLNode error1 = node.addChild("p", HTMLEncoder.encode(error.toString()));
		for (StackTraceElement ste : error.getStackTrace()){
			error1.addChild("br");
			error1.addChild("#", ste.toString());
		}
	}
	
    /**
     * Build a search page for search in it's current state
     **/
    public static String searchpage(Search searchobject, boolean refresh, Exception e){
		if(searchobject==null || searchobject.isSuccess() || e!=null)
			refresh = false;
			
		
        // Show any errors
		HTMLNode errorDiv = new HTMLNode("div", "id", "errors");
        if (e != null){
            addError(errorDiv, e);
		}
		if (searchobject != null && searchobject.getError() != null)
			addError(errorDiv, searchobject.getError());
			
		String search = "";
		String indexuri = "";
		try{
			search = searchobject !=null ? HTMLEncoder.encode(searchobject.getQuery()) : "";
			indexuri = searchobject !=null ? HTMLEncoder.encode(searchobject.getIndex().getIndexURI()) : "";
		}catch(Exception exe){
			addError(errorDiv, exe);
		}

			
			
		HTMLNode pageNode = new HTMLNode.HTMLDoctype("html", "-//W3C//DTD XHTML 1.1//EN");
		HTMLNode htmlNode = pageNode.addChild("html", "xml:lang", L10n.getSelectedLanguage().isoCode);
		HTMLNode headNode = htmlNode.addChild("head");
		if(refresh)
            headNode.addChild("meta", new String[] { "http-equiv", "content" }, new String[] { "refresh", "1" });
		headNode.addChild("meta", new String[] { "http-equiv", "content" }, new String[] { "Content-Type", "text/html; charset=utf-8" });
		headNode.addChild("title", (searchobject==null?"":(search + " - ")) + plugName);
		//headNode.addChild("link", new String[] { "rel", "href", "type", "title" }, new String[] { "stylesheet", "/static/themes/" + theme.code + "/theme.css", "text/css", theme.code });
		
		HTMLNode bodyNode = htmlNode.addChild("body");
		


        // Start of body
		HTMLNode searchDiv = bodyNode.addChild("div", "id", "searchbar");
		HTMLNode searchForm = searchDiv.addChild("form", "method", "GET");
			HTMLNode searchTable = searchForm.addChild("table", "width", "100%");
				HTMLNode searchTop = searchTable.addChild("tr");
					searchTop.addChild("td", new String[]{"rowspan","width"},new String[]{"2","120"})
						.addChild("H1", plugName);
					HTMLNode searchcell = searchTop.addChild("td", "width", "400");
						searchcell.addChild("input", new String[]{"name", "size", "type", "value"}, new String[]{"search", "40", "text", search});
						searchcell.addChild("input", new String[]{"name", "type", "value", "tabindex"}, new String[]{"find", "submit", "Find!", "1"});
				
				searchTable.addChild("tr")
					.addChild("td", xl.getString("Index"))
						.addChild("input", new String[]{"name", "type", "value", "size"}, new String[]{"index", "text", indexuri, "40"});
		
		
		bodyNode.addChild(errorDiv);


        // If showing a search
        if(searchobject != null){
			HTMLNode progressDiv = bodyNode.addChild("div", "id", "progress");
            // Search description
			HTMLNode progressTable = progressDiv.addChild("table", "width", "100%");
				HTMLNode searchingforCell = progressTable.addChild("tr")
					.addChild("td", "colspan", "2");
						searchingforCell.addChild("#", xl.getString("Searching-for"));
						searchingforCell.addChild("span", "class", "librarian-searching-for-target")
							.addChild("b", HTMLEncoder.encode(search));
						searchingforCell.addChild("#", xl.getString("in-index"));
						searchingforCell.addChild("i", HTMLEncoder.encode(indexuri));
			

				// Search status
				HTMLNode statusRow = progressTable.addChild("tr");
					statusRow.addChild("td", "width", "140", xl.getString("Search-status"));
					searchobject.getprogress(statusRow.addChild("td")
						.addChild("div", "id", "librarian-search-status"));
			
			bodyNode.addChild("p");

            // If search is complete show results
            if (searchobject.isdone())
				try{
					searchobject.getresult(bodyNode.addChild("div", "id", "results"));
				}catch(Exception ex){
					addError(errorDiv, ex);
				}
        }

		return pageNode.generate();
    }
}

