package husacct.control.task.help;

import husacct.common.Resource;
import husacct.control.task.MainController;

import java.awt.Component;
import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeModel;

import org.apache.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

public class HelpTreeModelLoader {

	private Logger logger = Logger.getLogger(MainController.class);
	List<HelpTreeNode> HelpTreeNodeList = new ArrayList<HelpTreeNode>();

	public Document getXmlDocument() {
		String manualXmlPath = Resource.HELP_PATH + "manuals.xml";
		logger.debug(manualXmlPath);

		Reader reader = new InputStreamReader(Resource.getStream(manualXmlPath));
		SAXBuilder sax = new SAXBuilder();
		Document doc = new Document();
		try {			
			doc = sax.build(reader);
		}
		catch (Exception ex) {
			logger.error(ex.getMessage());
		}
		return doc;
	}



	public DefaultMutableTreeNode getTreeModel() {


		Element root = getXmlDocument().getRootElement();

		DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(root.getName());
		for(Element child : root.getChildren()) {
			HelpTreeNode HelpNode = new HelpTreeNode(child.getAttributeValue("filename"), child.getAttributeValue("viewname"), child.getName() ,"html");
			DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(HelpNode);
			if(HelpNode.getParent() == null) {
				rootNode.add(childNode);
			}
			else {
				DefaultMutableTreeNode ParentNode = new DefaultMutableTreeNode(new HelpTreeNode(HelpNode.parent,HelpNode.parent,HelpNode.parent, "folder"));
				ParentNode = checkIfParentAlreadyExists(rootNode, ParentNode);
				
				ParentNode.add(childNode);
				rootNode.add(ParentNode);
				//parent.add(new DefaultMutableTreeNode(new HelpTreeNode ("Test1", "Test2", "Test3")).add(childNode));
			}
			HelpTreeNodeList.add(HelpNode);
		}

		return rootNode;
	}

	public DefaultMutableTreeNode checkIfParentAlreadyExists(DefaultMutableTreeNode root, DefaultMutableTreeNode Child) {
		String nameOfNode = ((HelpTreeNode)Child.getUserObject()).getViewName();
		List<DefaultMutableTreeNode> list = Collections.list(root.children());
		for(int i = 0; i < list.size(); i++) {
			HelpTreeNode treeNode = (HelpTreeNode)list.get(i).getUserObject();
			if(treeNode.getViewName().equals(nameOfNode)) {
				return list.get(i);
			}
		}
		return Child;
	}
	public String getContent(InputStream stream) {		
		BufferedReader br = new BufferedReader(new InputStreamReader(stream));
		String line;
		String html = "";
		try {
			while((line = br.readLine()) != null) {
				html+=line;
			}	
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
		html = setPath(html);
		return html;
	}



	public String getContent2(File f) throws FileNotFoundException {

		BufferedReader br = new BufferedReader(new FileReader(f));

		String line;
		String html = "";
		try {
			while((line = br.readLine()) != null) {
				html+=line;
			}	
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
		
		if(html.contains("RESOURCE.GET")) {
			html = setPath(html);
		}
		return html;


	}
	
	public String setPath(String html) {
		while(html.contains("RESOURCE.GET")) {
			int startIndex = html.indexOf("RESOURCE.GET");
			int startFileNameIndex = startIndex += "RESOURCE.GET".length() + 1;
			int endFileNameIndex = startFileNameIndex;
			if(startIndex > -1) {				
				while(html.charAt(endFileNameIndex) != ')') {
					endFileNameIndex++;
				}
				String filename = html.substring(startFileNameIndex, endFileNameIndex);
				URL pathname = Resource.get(Resource.HELP_IMAGE_PATH + filename);
				html = html.replace("RESOURCE.GET(" + filename + ")", pathname+"");
				
			}
			else {
				break;
			}
		}
		return html;
	}



	public String getContent(Component component) {
		for(HelpTreeNode htn : HelpTreeNodeList) {
			if(htn.getComponentName().equals(component.getClass().getName().substring(component.getClass().getName().lastIndexOf('.')+1))) {
				
				String HelpPagePath = Resource.HELP_PAGES_PATH + htn.getFilename();
				InputStream stream = Resource.getStream(HelpPagePath);

				String html = getContent(stream);
				
				if(html.equals("") || html==null){
					HelpPagePath = Resource.HELP_PAGES_PATH + "home.html";
					stream = Resource.getStream(HelpPagePath);
					return getContent(stream);
				}
				else {
					return html;
					
				}



			}
		}
		String HelpPagePath = Resource.HELP_PAGES_PATH + "home.html";
		InputStream stream = Resource.getStream(HelpPagePath);
		return getContent(stream);
	}
}