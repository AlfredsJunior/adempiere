/**
 * 
 */
package org.adempiere.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.Adempiere;
import org.compiere.model.I_AD_Process_Para;
import org.compiere.model.MProcess;
import org.compiere.model.MProcessPara;
import org.compiere.model.Query;
import org.compiere.process.SvrProcess;
import org.compiere.util.CLogger;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;

/**
 * 	Generate Process Classes extending SvrProcess.
 *	@author Yamel Senih, ysenih@erpcya.com, ERPCyA http://www.erpcya.com
 *		<li> FR [ 326 ] Process source code generated automatically
 *		@see https://github.com/adempiere/adempiere/issues/326
 */
public class ProcessClassGenerator {

	/**
	 * Standard constructor
	 * @param process
	 * @param directory
	 */
	public ProcessClassGenerator(MProcess process, String directory) {
		//	Get Values
		AD_Process_ID = process.getAD_Process_ID();
		className = process.getClassname();
		processName = process.getName();
		directoryName = directory;
	}
	
	/**	Process ID		*/
	private int AD_Process_ID = 0;
	/**	Process Name	*/
	private String processName = null;
	/**	Class Name		*/
	private String className = null;
	/**	Directory Name	*/
	private String directoryName = null;
	
	/**
	 * Create file
	 * @return
	 */
	public String createFile() {
		int index = className.lastIndexOf(".");
		if(index == -1)
			throw new AdempiereException("@Classname@ @NotFound@");
		//	
		String packageName = className.substring(0, index);
		String fileName = className.substring(index + 1);
		
		StringBuffer header = createHeader(AD_Process_ID, packageName, fileName);
		// Save
		if (!directoryName.endsWith(File.separator) )
			directoryName += File.separator;
		//	Write to file
		writeToFile(header, directoryName + fileName + ".java");
		//	Return
		return directoryName + fileName + ".java";
	}
	
	/** Import classes 		*/
	private Collection<String> s_importClasses = new TreeSet<String>();
	/**	Logger			*/
	private static CLogger	log	= CLogger.getCLogger (ProcessClassGenerator.class);
	/** Parameters Name	*/
	private StringBuffer parametersName = new StringBuffer();
	/** Parameters Value	*/
	private StringBuffer parametersValue = new StringBuffer();
	/** Parameters Fill	*/
	private StringBuffer parametersFill = new StringBuffer();
	
	/**
	 * Create Parameters for header
	 * @param AD_Process_ID
	 * @return
	 */
	private void createParameters(int AD_Process_ID) {
		List <MProcessPara> parameters = new Query(Env.getCtx(), 
				I_AD_Process_Para.Table_Name, I_AD_Process_Para.COLUMNNAME_AD_Process_ID + " = ?", null)
			.setParameters(AD_Process_ID)
			.setOrderBy(I_AD_Process_Para.COLUMNNAME_SeqNo)
			.list();
		//	Create Name and Values
		for(MProcessPara para : parameters) {
			createParameterName(para.getColumnName());
			createParameterValue(para.getColumnName(), para.getAD_Reference_ID(), false);
			createParameterFill(para.getColumnName(), para.getAD_Reference_ID(), false);
			//	For Range
			if(para.isRange()) {
				createParameterValue(para.getColumnName(), para.getAD_Reference_ID(), true);
				createParameterFill(para.getColumnName(), para.getAD_Reference_ID(), true);
			}
		}
	}
	
	/**
	 * Create Header class
	 * @param AD_Process_ID
	 * @param packageName
	 * @return
	 */
	private StringBuffer createHeader(int AD_Process_ID, String packageName, String className) {
		StringBuffer header = new StringBuffer();
		createParameters(AD_Process_ID);
		//	Add SvrProcess
		if(!packageName.equals("org.compiere.process"))
			addImportClass(SvrProcess.class);
		//	Add license
		header.append(ModelInterfaceGenerator.COPY);
		//	New line
		header.append(ModelInterfaceGenerator.NL);
		//	Package
		header.append("package ").append(packageName).append(";\n");
		//	New line
		header.append(ModelInterfaceGenerator.NL);
		//	Import Class
		header.append(getImportClass());
		//	New line
		header.append(ModelInterfaceGenerator.NL);
		//	New line
		header.append(ModelInterfaceGenerator.NL);
		//	Add comments
		header.append("\n/** Generated Process (").append(processName).append(")\n")
		 	.append(" *  @author Adempiere (generated) \n")
		 	.append(" *  @version ").append(Adempiere.MAIN_VERSION).append("\n")
		 	.append(" */\n");
		//	Add Class Name
		header
			.append("public class ").append(className).append(" extends ").append("SvrProcess")
			.append("\n{");
		//	Add Parameters Name
		header.append(parametersName);
		//	New line
		header.append(ModelInterfaceGenerator.NL);
		//	Add Parameters Value
		header.append(parametersValue);
		//	Add Prepare method
		header
			.append("\n\n\t@Override")
			.append("\n\tprotected void prepare()")
			.append("\n\t{");
		//	Add fill
		header.append(parametersFill);
		//	End Prepare
		header.append("\n\t}");
		//	Add do it
		header
			.append("\n\n\t@Override")
			.append("\n\tprotected String doIt() throws Exception")
			.append("\n\t{")
			.append("\n\t\treturn \"\";")
			.append("\n\t}");
		//	End class
		header.append("\n}");
		//	Return
		return header;
	}
	
	/**
	 * Create Comment and parameter Name
	 * @param parameterName
	 */
	private void createParameterName(String parameterName) {
		//	Add new Line
		parametersName.append(ModelInterfaceGenerator.NL);
		//	Add Comment
		parametersName
			.append("\t/**\tParameter Name for ").append(parameterName).append("\t*/")
			.append(ModelInterfaceGenerator.NL)
			.append("\tprivate final String PARAMETERNAME_").append(parameterName)
			.append(" = ").append("\"").append(parameterName)
			.append("\";");
	}
	
	/**
	 * Create Comment and parameter Value
	 * @param parameterName
	 * @param DisplayType
	 * @param isTo
	 */
	private void createParameterValue(String parameterName, int AD_Reference_ID, boolean isTo) {
		//	Add new Line
		parametersValue.append(ModelInterfaceGenerator.NL);
		//	Add Comment
		parametersValue
			.append("\t/**\tParameter Value for ").append(parameterName).append(isTo? "_To": "").append("\t*/")
			.append(ModelInterfaceGenerator.NL)
			.append("\tprivate ").append(getType(AD_Reference_ID)).append(" ")
			.append("p_").append(parameterName)
			.append(isTo? "_To": "")
			.append(";");
	}
	
	/**
	 * Create Fill Source
	 * @param parameterName
	 * @param AD_Reference_ID
	 * @param isTo
	 */
	private void createParameterFill(String parameterName, int AD_Reference_ID, boolean isTo) {
		//	Add new Line
		parametersFill.append(ModelInterfaceGenerator.NL);
		//	Add Comment
		parametersFill
			.append("\t\tp_").append(parameterName).append(isTo? "_To": "")
			.append(" = ").append(getProcessMethod(AD_Reference_ID, isTo))
			.append("(PARAMETERNAME_").append(parameterName).append(")")
			.append(";");
	}
	
	/**
	 * Get Type for declaration
	 * @param AD_Reference_ID
	 * @return
	 */
	private String getType(int AD_Reference_ID) {
		Class clazz = DisplayType.getClass(AD_Reference_ID, true);
		//	Verify Type
		if (clazz == String.class) {
			return "String";
		} else if (clazz == Integer.class) {
			return "int";
		} else if (clazz == BigDecimal.class) {
			addImportClass(BigDecimal.class);
			return "BigDecimal";
		} else if (clazz == Timestamp.class) {
			addImportClass(Timestamp.class);
			return "Timestamp";
		} else if (clazz == Boolean.class) {
			return "boolean";
		}
		//
		return "Object";
	}
	
	/**
	 * Get Type for declaration
	 * @param AD_Reference_ID
	 * @return
	 */
	private String getProcessMethod(int AD_Reference_ID, boolean isTo) {
		String type = getType(AD_Reference_ID);
		//	
		String typeForMethod = type.substring(0, 1);
		//	Change first
		typeForMethod = typeForMethod.toUpperCase();
		//	
		typeForMethod = typeForMethod + type.substring(1);
		//	Return
		if(typeForMethod.equals("Object"))
			return "getParameter";
		//	Default return
		return "getParameter" + (isTo? "To": "") + "As" + typeForMethod;
	}
	
	
	/**
	 * Add class name to class import list
	 * @param className
	 */
	private void addImportClass(String className) {
		if (className == null
				|| (className.startsWith("java.lang.") && !className.startsWith("java.lang.reflect.")))
			return;
		for(String name : s_importClasses) {
			if (className.equals(name))
				return;
		}
		s_importClasses.add(className);
	}
	
	/**
	 * Add class to class import list
	 * @param cl
	 */
	private void addImportClass(Class<?> cl) {
		if (cl.isArray()) {
			cl = cl.getComponentType();
		}
		if (cl.isPrimitive())
			return;
		addImportClass(cl.getCanonicalName());
	}
	
	/**
	 * Get import class for header
	 * @return
	 */
	private StringBuffer getImportClass() {
		StringBuffer importClass = new StringBuffer();
		for(String imp :s_importClasses) {
			//	For new line
			if(importClass.length() > 0)
				importClass.append(ModelInterfaceGenerator.NL);
			//	
			importClass.append("import ").append(imp).append(";");
		}
		//	Default return
		return importClass;
	}
	
	/**************************************************************************
	 * 	Write to file
	 * 	@param sb string buffer
	 * 	@param fileName file name
	 */
	private void writeToFile (StringBuffer sb, String fileName)
	{
		try
		{
			File out = new File (fileName);
			Writer fw = new OutputStreamWriter(new FileOutputStream(out, false), "UTF-8");
			for (int i = 0; i < sb.length(); i++)
			{
				char c = sb.charAt(i);
				//	after
				if (c == ';' || c == '}')
				{
					fw.write (c);
				}
				//	before & after
				else if (c == '{')
				{
					fw.write (c);
				}
				else
					fw.write (c);
			}
			fw.flush ();
			fw.close ();
			float size = out.length();
			size /= 1024;
			log.info(out.getAbsolutePath() + " - " + size + " kB");
		}
		catch (Exception ex)
		{
			log.log(Level.SEVERE, fileName, ex);
			throw new RuntimeException(ex);
		}
	}
}
